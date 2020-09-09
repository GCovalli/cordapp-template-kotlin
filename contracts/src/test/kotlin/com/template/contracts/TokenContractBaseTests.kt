package com.template.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices

open class TokenContractBaseTests {
    protected val ledgerServices = MockServices()
    protected val alice = TestIdentity(CordaX500Name("Alice", "London", "GB")).party
    protected val bob = TestIdentity(CordaX500Name("Bob", "New York", "US")).party
    protected val carly = TestIdentity(CordaX500Name("Carly", "New York", "US")).party
}