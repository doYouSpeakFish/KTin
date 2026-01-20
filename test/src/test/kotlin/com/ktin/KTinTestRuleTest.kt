package com.ktin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class KTinTestRuleTest {
    @Rule
    @JvmField
    val rule = KTinTestRule()

    class CounterSingleton private constructor(val id: Int) {
        companion object : Singleton<CounterSingleton>() {
            private var counter = 0

            fun reset() {
                counter = 0
            }

            override fun create() = CounterSingleton(++counter)
        }
    }

    object InjectedString : InjectedSingleton<String>()

    @Before
    fun setUp() {
        CounterSingleton.reset()
    }

    @Test
    fun singletonReturnsSameInstance() {
        val first = CounterSingleton()
        val second = CounterSingleton.getInstance()

        assertSame(first, second)
        assertEquals(1, first.id)
    }

    @Test
    fun injectedSingletonReturnsInjectedInstance() {
        InjectedString.inject { "value" }

        assertEquals("value", InjectedString())
    }

    @Test
    fun injectedSingletonThrowsWhenInjectedTwice() {
        InjectedString.inject { "first" }

        val error = assertThrows(IllegalArgumentException::class.java) {
            InjectedString.inject { "second" }
        }

        assertTrue(error.message!!.contains("already been injected"))
    }

    @Test
    fun injectedSingletonThrowsWhenNotInjected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            InjectedString()
        }

        assertTrue(error.message!!.contains("has not been injected"))
    }
}
