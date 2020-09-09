package com.template.contracts

import com.template.contracts.TokenContract.Companion.TOKEN_CONTRACT_ID
import com.template.states.TokenState
import net.corda.testing.contracts.DummyContract
import net.corda.testing.contracts.DummyState
import net.corda.testing.node.transaction
import org.junit.Test

class TokenContractRedeemTests : TokenContractBaseTests() {

    @Test
    fun `Transaction must include a TokenContract command`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            command(listOf(alice.owningKey, bob.owningKey), DummyContract.Commands.Create())
            `fails with`("Required com.template.contracts.TokenContract.Commands command")
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Redeem())
            verifies()
        }
    }

    @Test
    fun `Redeem transaction must have no outputs`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 10L))
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Redeem())
            `fails with`("No output states should be created when redeeming Tokens.")
        }
    }

    @Test
    fun `Redeem transaction must have inputs`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, DummyState())
            command(alice.owningKey, TokenContract.Commands.Redeem())
            `fails with`("There should be input states consumed when redeeming Tokens.")
        }
    }

    @Test
    // Testing this may be redundant as these wrong states would have to be issued first, but the contract would not
    // let that happen.
    fun `Inputs must not have a zero quantity`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 0L))
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Redeem())
            `fails with`("All quantities should be above 0.")
        }
    }

    @Test
    // Testing this may be redundant as these wrong states would have to be issued first, but the contract would not
    // let that happen.
    fun `Inputs must not have negative quantity`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, -1L))
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Redeem())
            `fails with`("All quantities should be above 0.")
        }
    }

    @Test
    fun `Issuer must sign Redeem transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            command(bob.owningKey, TokenContract.Commands.Redeem())
            `fails with`("The issuers should sign Token redeem transaction.")
        }
    }

    @Test
    fun `Current holder must sign Redeem transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            command(alice.owningKey, TokenContract.Commands.Redeem())
            `fails with`("The holders should sign Token redeem transaction.")
        }
    }

    @Test
    fun `All issuers must sign Redeem transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(carly, bob, 20L))
            command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Redeem())
            `fails with`("The issuers should sign Token redeem transaction.")
        }
    }

    @Test
    fun `All current holders must sign Redeem transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(carly, bob, 20L))
            command(listOf(alice.owningKey, carly.owningKey), TokenContract.Commands.Redeem())
            `fails with`("The holders should sign Token redeem transaction.")
        }
    }

    @Test
    fun `Can have different issuers in Redeem transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, alice, 20L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 30L))
            input(TOKEN_CONTRACT_ID, TokenState(carly, bob, 20L))
            input(TOKEN_CONTRACT_ID, TokenState(carly, alice, 20L))
            command(listOf(alice.owningKey, bob.owningKey, carly.owningKey), TokenContract.Commands.Redeem())
            verifies()
        }
    }
}