package com.template.contracts

import com.template.states.TokenState
import com.template.states.mapSumByIssuer
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class TokenContract : Contract {

    companion object {
        const val TOKEN_CONTRACT_ID = "com.template.contracts.TokenContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        val inputStates = tx.inputsOfType<TokenState>()
        val outputStates = tx.outputsOfType<TokenState>()
        val areAllQuantitiesPositive = inputStates.all { it.quantity > 0 } && outputStates.all { it.quantity > 0 }

        when (command.value) {
            is Commands.Issue -> requireThat {
                "No input states should be consumed when issuing Tokens." using inputStates.isEmpty()
                "There should be output states created when issuing Tokens." using outputStates.isNotEmpty()

                "All quantities should be above 0." using areAllQuantitiesPositive
                "Only the issuers may sign Token issue transaction." using command.signers.containsAll(outputStates.map { it.issuer.owningKey }.distinct())
            }

            is Commands.Move -> requireThat {
                "There should be input states consumed when moving Tokens." using inputStates.isNotEmpty()
                "There should be output states created when moving Tokens." using outputStates.isNotEmpty()

                "All quantities should be above 0." using areAllQuantitiesPositive
                "Only the holders may sign Token move transaction." using command.signers.containsAll(inputStates.map { it.holder.owningKey }.distinct())

                val inputStatesSums = inputStates.mapSumByIssuer()
                val outputStatesSums = outputStates.mapSumByIssuer()
                "Consumed and created issuers should be identical." using (inputStatesSums.keys == outputStatesSums.keys)
                "The sum of quantities for each issuer should be conserved." using inputStatesSums.all { outputStatesSums[it.key] == it.value }
            }

            is Commands.Redeem -> requireThat {
                "There should be input states consumed when redeeming Tokens." using inputStates.isNotEmpty()
                "No output states should be created when redeeming Tokens." using outputStates.isEmpty()

                "All quantities should be above 0." using areAllQuantitiesPositive
                "The issuers should sign Token redeem transaction." using command.signers.containsAll(inputStates.map { it.issuer.owningKey }.distinct())
                "The holders should sign Token redeem transaction." using command.signers.containsAll(inputStates.map { it.holder.owningKey }.distinct())
            }

            else -> throw IllegalStateException("Unknown command ${command.value}.")
        }
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Move : Commands
        class Redeem : Commands
    }
}