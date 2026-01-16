package com.ktin

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A test rule that clears all singletons after each test, ensuring that each test runs with fresh
 * instances of singletons.
 */
class KTinTestRule : TestRule {
    override fun apply(
        base: Statement,
        description: Description,
    ) = object : Statement() {
        @OptIn(InternalApi::class)
        override fun evaluate() {
            SingletonInitializerStore.clear()
            SingletonStore.clear()
            base.evaluate()
            SingletonInitializerStore.clear()
            SingletonStore.clear()
        }
    }
}
