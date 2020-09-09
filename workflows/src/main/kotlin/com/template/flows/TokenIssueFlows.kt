package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object TokenIssueFlows {


    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val heldQuantities: List<Pair<Party, Long>>) : FlowLogic<SignedTransaction>() {

        constructor(holder: Party, quantity: Long) : this(listOf(Pair(holder, quantity)))

        init {
            require(heldQuantities.isNotEmpty()) { "heldQuantities cannot be empty" }
            val nonZero = heldQuantities.none { it.second <= 0 }
            require(nonZero) { "heldQuantities must all be above 0" }
        }

        @Suppress("ClassName")
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on parameters.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            val issuer = ourIdentity
            val outputTokens = heldQuantities.map {
                TokenState(issuer = issuer, holder = it.first, quantity = it.second)
            }

            val notary = serviceHub.networkMapCache.getNotary(Constants.desiredNotary)!!

            progressTracker.currentStep = GENERATING_TRANSACTION

            val txBuilder = TransactionBuilder(notary)
                    .addCommand(TokenContract.Commands.Issue(), issuer.owningKey)

            outputTokens.forEach { txBuilder.addOutputState(it, TokenContract.TOKEN_CONTRACT_ID) }

            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val fullySignedTx = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = FINALISING_TRANSACTION
            val holderFlows = outputTokens
                    .map { it.holder }
                    // Remove duplicates as it would be an issue when initiating flows, at least.
                    .distinct()
                    // Remove myself.
                    // I already know what I am doing so no need to inform myself with a separate flow.
                    .minus(issuer)
                    .map { initiateFlow(it) }

            return subFlow(FinalityFlow(
                    fullySignedTx,
                    holderFlows,
                    FINALISING_TRANSACTION.childProgressTracker()
            )).also { notarised ->
                serviceHub.recordTransactions(StatesToRecord.ALL_VISIBLE, listOf(notarised))
            }
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(ReceiveFinalityFlow(counterpartySession))
        }
    }
}
