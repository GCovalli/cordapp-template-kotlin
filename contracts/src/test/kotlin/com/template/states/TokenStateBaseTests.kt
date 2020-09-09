package com.template.states

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity

open class TokenStateBaseTests {
    protected val alice = TestIdentity(CordaX500Name("Alice", "London", "GB")).party
    protected val bob = TestIdentity(CordaX500Name("Bob", "London", "GB")).party
    protected val carly = TestIdentity(CordaX500Name("Carly", "London", "GB")).party
}