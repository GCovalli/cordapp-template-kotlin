package com.template.contracts

import com.template.contracts.TokenContract.Companion.TOKEN_CONTRACT_ID
import com.template.states.TokenState
import net.corda.testing.contracts.DummyContract
import net.corda.testing.contracts.DummyState
import net.corda.testing.node.transaction
import org.junit.Test

class TokenContractIssueTests : TokenContractBaseTests() {

    @Test
    fun `transaction must include a TokenContract command`() {
        ledgerServices.transaction {
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            // Let's add a command from an unrelated dummy contract.
            command(alice.owningKey, DummyContract.Commands.Create())
            `fails with`("Required com.template.contracts.TokenContract.Commands command")
            command(alice.owningKey, TokenContract.Commands.Issue())
            verifies()
        }
    }

    @Test
    fun `Issue transaction must have no inputs`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 10L))
            command(alice.owningKey, TokenContract.Commands.Issue())
            `fails with`("No input states should be consumed when issuing Tokens.")
        }
    }

    @Test
    fun `Issue transaction must have outputs`() {
        ledgerServices.transaction {
            output(TOKEN_CONTRACT_ID, DummyState())
            command(alice.owningKey, TokenContract.Commands.Issue())
            `fails with`("There should be output states created when issuing Tokens.")
        }
    }

    @Test
    fun `Outputs must not have a zero quantity`() {
        ledgerServices.transaction {
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 0L))
            command(alice.owningKey, TokenContract.Commands.Issue())
            `fails with`("All quantities should be above 0.")
        }
    }

    @Test
    fun `Outputs must not have negative quantity`() {
        ledgerServices.transaction {
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, -1L))
            command(alice.owningKey, TokenContract.Commands.Issue())
            `fails with`("All quantities should be above 0.")
        }
    }

    @Test
    fun `Issuer must sign Issue transaction`() {
        ledgerServices.transaction {
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            command(bob.owningKey, TokenContract.Commands.Issue())
            `fails with`("Only the issuers may sign Token issue transaction.")
        }
    }

    @Test
    fun `All issuers must sign Issue transaction`() {
        ledgerServices.transaction {
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(carly, bob, 20L))
            command(alice.owningKey, TokenContract.Commands.Issue())
            `fails with`("Only the issuers may sign Token issue transaction.")
        }
    }

    @Test
    fun `Can have different issuers in Issue transaction`() {
        ledgerServices.transaction {
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, alice, 20L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 30L))
            output(TOKEN_CONTRACT_ID, TokenState(carly, bob, 20L))
            output(TOKEN_CONTRACT_ID, TokenState(carly, alice, 20L))
            command(listOf(alice.owningKey, carly.owningKey), TokenContract.Commands.Issue())
            verifies()
        }
    }
}