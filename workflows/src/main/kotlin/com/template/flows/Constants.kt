package com.template.flows

import net.corda.core.identity.CordaX500Name


interface Constants {
    companion object {
        // Hard-coded for simplicity's sake. In general, a configuration file is preferred.
        const val desiredNotaryName = "O=Notary, L=London, C=GB"
        val desiredNotary = CordaX500Name.parse(desiredNotaryName)
    }
}