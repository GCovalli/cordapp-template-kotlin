package com.template.states

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenStateUtilitiesMapSumByIssuerTests : TokenStateBaseTests() {

    @Test
    fun `mapSumByIssuer returns empty on empty`() {
        val mappedSums = listOf<TokenState>().mapSumByIssuer()
        assertTrue(mappedSums.isEmpty())
    }

    @Test
    fun `mapSumByIssuer gets same value on singleton`() {
        val mappedSums = listOf(TokenState(alice, bob, 10)).mapSumByIssuer()
        assertEquals(1, mappedSums.size)
        assertEquals(10L, mappedSums[alice])
    }

    @Test
    fun `mapSumByIssuer gets sum on unique issuer`() {
        val mappedSums = listOf(
                TokenState(alice, bob, 10),
                TokenState(alice, carly, 15))
                .mapSumByIssuer()
        assertEquals(1, mappedSums.size)
        assertEquals(25L, mappedSums[alice])
    }

    @Test
    fun `mapSumByIssuer gets sum for each issuer`() {
        val mappedSums = listOf(
                TokenState(alice, bob, 10),
                TokenState(alice, carly, 15),
                TokenState(carly, bob, 30),
                TokenState(carly, carly, 25),
                TokenState(carly, alice, 2)
        )
                .mapSumByIssuer()
        assertEquals(2, mappedSums.size)
        assertEquals(25L, mappedSums[alice])
        assertEquals(57L, mappedSums[carly])
    }

    @Test(expected = ArithmeticException::class)
    fun `overflow triggers error in mapSumByIssuer`() {
        listOf(TokenState(alice, bob, Long.MAX_VALUE),
                TokenState(alice, carly, 1))
                .mapSumByIssuer()
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `mapSumByIssuer is immutable`() {
        val mappedSums = listOf(TokenState(alice, bob, 10)).mapSumByIssuer() as MutableMap
        mappedSums[alice] = 20L
    }
}