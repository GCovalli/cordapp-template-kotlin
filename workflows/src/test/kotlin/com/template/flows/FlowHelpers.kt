package com.template.flows

import com.template.states.TokenState
import groovy.util.GroovyTestCase.assertEquals
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode

fun createFrom(
        issuer: StartedMockNode,
        holder: StartedMockNode,
        quantity: Long) = TokenState(
        issuer.info.singleIdentity(),
        holder.info.singleIdentity(),
        quantity)

fun TokenState.toPair() = Pair(holder, quantity)

fun StartedMockNode.assertHasStatesInVault(tokenStates: List<TokenState>) {
    val vaultTokens = transaction {
        services.vaultService.queryBy(TokenState::class.java).states
    }
    assertEquals(tokenStates.size, vaultTokens.size)
    assertEquals(tokenStates, vaultTokens.map { it.state.data })
}

class NodeHolding(val holder: StartedMockNode, val quantity: Long) {
    fun toPair() = Pair(holder.info.singleIdentity(), quantity)
}

fun StartedMockNode.issueTokens(network: MockNetwork, nodeHoldings: Collection<NodeHolding>) =
        TokenIssueFlows.Initiator(nodeHoldings.map(NodeHolding::toPair))
                .let { startFlow(it) }
                .also { network.runNetwork() }
                .getOrThrow()
                .toLedgerTransaction(services)
                .outRefsOfType<TokenState>()