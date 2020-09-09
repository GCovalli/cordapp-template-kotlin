package com.template.contracts

import com.template.contracts.TokenContract.Companion.TOKEN_CONTRACT_ID
import com.template.states.TokenState
import groovy.util.GroovyTestCase.assertEquals
import net.corda.core.contracts.TransactionVerificationException
import net.corda.testing.contracts.DummyContract
import net.corda.testing.node.transaction
import org.junit.Test

class TokenContractMoveTests : TokenContractBaseTests() {

    @Test
    fun `Transaction must include a TokenContract command`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 10L))
            command(alice.owningKey, DummyContract.Commands.Create())
            `fails with`("Required com.template.contracts.TokenContract.Commands command")
            command(bob.owningKey, TokenContract.Commands.Move())
            verifies()
        }
    }

    @Test
    fun `Move transaction must have inputs`() {
        ledgerServices.transaction {
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 10L))
            command(alice.owningKey, TokenContract.Commands.Move())
            `fails with`("There should be input states consumed when moving Tokens.")
        }
    }

    @Test
    fun `Move transaction must have outputs`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("There should be output states created when moving Tokens.")
        }
    }

    @Test
    // Testing this may be redundant as these wrong states would have to be issued first, but the contract would not
    // let that happen.
    fun `Inputs must not have a zero quantity`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 0L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            command(bob.owningKey, TokenContract.Commands.Move())
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
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 9L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("All quantities should be above 0.")
        }
    }

    @Test
    fun `Outputs must not have a zero quantity`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 0L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("All quantities should be above 0.")
        }
    }

    @Test
    fun `Outputs must not have negative quantity`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 11L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, -1L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("All quantities should be above 0.")
        }
    }

    @Test
    fun `Issuer must be conserved in Move transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(carly, bob, 10L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("Consumed and created issuers should be identical.")
        }
    }

    @Test
    fun `All issuers must be conserved in Move transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(carly, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 20L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("Consumed and created issuers should be identical.")
        }
    }

    @Test
    fun `Sum must be conserved in Move transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 15L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 20L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("The sum of quantities for each issuer should be conserved.")
        }
    }

    @Test
    fun `All sums per issuer must be conserved in Move transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 15L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 20L))
            input(TOKEN_CONTRACT_ID, TokenState(carly, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(carly, bob, 15L))
            output(TOKEN_CONTRACT_ID, TokenState(carly, bob, 30L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("The sum of quantities for each issuer should be conserved.")
        }
    }

    @Test
    fun `Sums that result in overflow are not possible in Move transaction`() {
        try {
            ledgerServices.transaction {
                input(TOKEN_CONTRACT_ID, TokenState(alice, bob, Long.MAX_VALUE))
                input(TOKEN_CONTRACT_ID, TokenState(alice, carly, 1L))
                output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 1L))
                output(TOKEN_CONTRACT_ID, TokenState(alice, carly, Long.MAX_VALUE))
                command(listOf(bob.owningKey, carly.owningKey), TokenContract.Commands.Move())
                verifies()
            }
            throw NotImplementedError("Should not reach here")
        } catch (e: TransactionVerificationException.ContractRejection) {
            assertEquals(ArithmeticException::class, e.cause!!::class)
        }
    }

    @Test
    fun `Current holder must sign Move transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 10L))
            command(alice.owningKey, TokenContract.Commands.Move())
            `fails with`("Only the holders may sign Token move transaction.")
        }
    }

    @Test
    fun `All current holders must sign Move transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, carly, 20L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 30L))
            command(bob.owningKey, TokenContract.Commands.Move())
            `fails with`("Only the holders may sign Token move transaction.")
        }
    }

    @Test
    fun `Can have different issuers in Move transaction`() {
        ledgerServices.transaction {
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 10L))
            input(TOKEN_CONTRACT_ID, TokenState(alice, bob, 20L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, alice, 5L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, bob, 5L))
            output(TOKEN_CONTRACT_ID, TokenState(alice, carly, 20L))
            input(TOKEN_CONTRACT_ID, TokenState(carly, carly, 40L))
            output(TOKEN_CONTRACT_ID, TokenState(carly, alice, 20L))
            output(TOKEN_CONTRACT_ID, TokenState(carly, bob, 20L))
            command(listOf(bob.owningKey, carly.owningKey), TokenContract.Commands.Move())
            verifies()
        }
    }
}