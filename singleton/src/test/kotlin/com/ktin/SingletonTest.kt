package com.ktin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class SingletonTest {
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
        clearStores()
    }

    @After
    fun tearDown() {
        clearStores()
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

    @Test
    fun singletonStoreReturnsExistingInstanceWhenInsertedDuringSynchronization() {
        @OptIn(InternalApi::class)
        val key = object : Singleton<String>() {
            override fun create() = "created"
        }

        val started = CountDownLatch(1)
        val done = CountDownLatch(1)
        var threadResult: String? = null

        @OptIn(InternalApi::class)
        synchronized(key) {
            val thread = Thread {
                started.countDown()
                threadResult = SingletonStore.getOrPut(key) { "thread" }
                done.countDown()
            }
            thread.start()
            started.await()
            val inserted = SingletonStore.getOrPut(key) { "main" }
            assertEquals("main", inserted)
        }

        done.await()
        assertEquals("main", threadResult)
    }

    @Test
    fun initializerStoreReturnsExistingInitializerWhenInsertedDuringSynchronization() {
        @OptIn(InternalApi::class)
        val key = object : Singleton<String>() {
            override fun create() = "created"
        }

        val started = CountDownLatch(1)
        val done = CountDownLatch(1)
        var threadInitializer: (() -> String)? = null

        @OptIn(InternalApi::class)
        synchronized(key) {
            val thread = Thread {
                started.countDown()
                threadInitializer = SingletonInitializerStore.putIfAbsent(key) { "thread" }
                done.countDown()
            }
            thread.start()
            started.await()
            val inserted = SingletonInitializerStore.putIfAbsent(key) { "main" }
            assertEquals(null, inserted)
        }

        done.await()
        assertEquals("main", threadInitializer?.invoke())
    }

    @OptIn(InternalApi::class)
    private fun clearStores() {
        SingletonInitializerStore.clear()
        SingletonStore.clear()
    }
}
