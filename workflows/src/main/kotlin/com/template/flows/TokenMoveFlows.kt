package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TokenContract
import com.template.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

object TokenMoveFlows {

    @CordaSerializable
    enum class TransactionRole { SIGNER, PARTICIPANT }

    @InitiatingFlow
    @StartableByRPC
    class Initiator @JvmOverloads constructor(private val inputTokens: List<StateAndRef<TokenState>>,
                                              private val outputTokens: List<TokenState>,
                                              override val progressTracker: ProgressTracker = tracker()) : FlowLogic<SignedTransaction>() {

        init {
            require(inputTokens.isNotEmpty()) { "inputTokens cannot be empty" }
            require(outputTokens.isNotEmpty()) { "outputTokens cannot be empty" }
            val noneZero = outputTokens.none { it.quantity <= 0 }
            require(noneZero) { "outputTokens quantities must all ve above 0" }
        }

        @Suppress("ClassName")
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on parameters.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = GENERATING_TRANSACTION

            val notary = inputTokens
                    .map { it.state.notary }
                    .distinct()
                    .single()

            val allSigners = inputTokens
                    .map { it.state.data.holder }
                    .toSet()
            if (!allSigners.contains(ourIdentity)) throw FlowException("I must be a holder.")

            val txCommand = Command(TokenContract.Commands.Move(), allSigners.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary).addCommand(txCommand)

            inputTokens.forEach { txBuilder.addInputState(it) }
            outputTokens.forEach { txBuilder.addOutputState(it) }

            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val partlySignedTx = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = GATHERING_SIGS
            val signerFlows = allSigners
                    .minus(ourIdentity)
                    .map { initiateFlow(it) }
                    .onEach { it.send(TransactionRole.SIGNER) }

            val fullySignedTx = if (signerFlows.isEmpty()) partlySignedTx
            else subFlow(CollectSignaturesFlow(
                    partlySignedTx,
                    signerFlows,
                    GATHERING_SIGS.childProgressTracker()))

            progressTracker.currentStep = FINALISING_TRANSACTION
            val newHolderFlows = outputTokens
                    .map { it.holder }
                    .distinct()
                    .minus(allSigners)
                    .minus(ourIdentity)
                    .map { initiateFlow(it) }
                    .onEach { it.send(TransactionRole.PARTICIPANT) }

            return subFlow(FinalityFlow(
                    fullySignedTx,
                    signerFlows.plus(newHolderFlows),
                    FINALISING_TRANSACTION.childProgressTracker()
            ))

        }

    }

    open class Responder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suppress("ClassName")
        companion object {
            object RECEIVING_ROLE : ProgressTracker.Step("Receiving role to impersonate.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.") {
                override fun childProgressTracker() = SignTransactionFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    RECEIVING_ROLE,
                    SIGNING_TRANSACTION,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = RECEIVING_ROLE
            val myRole = counterpartySession.receive<TransactionRole>().unwrap { it }

            progressTracker.currentStep = SIGNING_TRANSACTION
            val txId = when (myRole) {
                // We do not need to sign.
                TransactionRole.PARTICIPANT -> null
                TransactionRole.SIGNER -> {
                    val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                        override fun checkTransaction(stx: SignedTransaction) {
                            // Notice that there is still a security risk here as my node can be asked to sign
                            // without my human knowledge.
                            // I must be relevant. We don't like signing irrelevant transactions.
                            val relevant = stx.toLedgerTransaction(serviceHub, false)
                                    .inputsOfType<TokenState>()
                                    .any { it.holder == ourIdentity }
                            if (!relevant) throw FlowException("I must be relevant.")
                        }
                    }
                    subFlow(signTransactionFlow).id
                }
            }

            progressTracker.currentStep = FINALISING_TRANSACTION
            return subFlow(ReceiveFinalityFlow(counterpartySession, txId))
        }
    }
}