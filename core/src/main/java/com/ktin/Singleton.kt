package com.ktin

/**
 * An abstract class that can be extended by the companion object of a class to turn it into
 * a singleton that can be retrieved using a normal no argument constructor.
 *
 * Example:
 * ```
 * class MySingleton(
 *     aDependency: Dependency,
 *     anotherDependency: AnotherDependency,
 * ) {
 *
 *     companion object : Singleton<MySingleton>() {
 *         override fun create() = MySingleton(
 *             aDependency = Dependency(),
 *             anotherDependency = AnotherDependency(),
 *         )
 *     }
 * }
 *
 * class DependsOnMySingleton(
 *     private val mySingleton: MySingleton = MySingleton() // This is a singleton instance
 * ) {
 *   ...
 * }
 * ```
 */
abstract class Singleton<T : Any> {
    /**
     * Creates a new instance of the singleton.
     */
    protected abstract fun create(): T

    /**
     * Returns the singleton instance.
     */
    fun getInstance() = getOrCreate()

    /**
     * Returns the singleton instance.
     */
    operator fun invoke(): T = getOrCreate()

    @OptIn(InternalApi::class)
    private fun getOrCreate(): T = SingletonStore.getOrPut(key = this, initializer = ::create)
}

/**
 * An abstract class that can be extended by an object or the companion object of a class to turn
 * it into a singleton that can be retrieved using a normal no argument constructor, and that can be
 * injected by calling the [inject] method of the object, allowing fakes to be injected for tests.
 *
 * The [inject] method accepts a lambda that is used to lazily initialize the singleton.
 *
 * Example:
 * ```
 * class MySingleton(
 *     aDependency: Dependency,
 *     anotherDependency: AnotherDependency,
 * ) {
 *
 *     companion object : InjectedSingleton<MySingleton>()
 * }
 *
 * class Main {
 *     fun main() {
 *         MySingleton.inject {
 *             MySingleton(
 *                 aDependency = Dependency(),
 *                 anotherDependency = AnotherDependency(),
 *             )
 *         }
 *     }
 * }
 *
 * class DependsOnMySingleton(
 *     private val mySingleton: MySingleton = MySingleton() // This is a singleton instance
 * ) {
 *   ...
 * }
 * ```
 */
abstract class InjectedSingleton<T : Any> : Singleton<T>() {

    /**
     * Lazily injects a new instance of the singleton. If the singleton has already been injected,
     * throws an [IllegalArgumentException].
     */
    fun inject(initializer: () -> T) {
        require(storeInitializerIfAbsent(this, initializer) == null) {
            "Singleton ${this::class.java.simpleName} has already been injected"
        }
    }

    override fun create() = requireNotNull(getInstanceFromStore()) {
        "Singleton ${this::class.java.simpleName} has not been injected"
    }

    @OptIn(InternalApi::class)
    private fun storeInitializerIfAbsent(
        key: Singleton<T>,
        initializer: () -> T,
    ): (() -> T)? = SingletonInitializerStore.putIfAbsent(key, initializer)

    @OptIn(InternalApi::class)
    private fun getInstanceFromStore() = SingletonInitializerStore.getInstance(this)
}

@InternalApi
object SingletonStore {
    @Volatile
    private var singletons = mapOf<Singleton<*>, Any>()

    fun clear() = synchronized(this) { singletons = emptyMap() }

    internal fun <T : Any> getOrPut(
        key: Singleton<T>,
        initializer: () -> T,
    ): T = get(key) ?: synchronized(key) {
        get(key) ?: initializer().also { put(key, it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> get(key: Singleton<T>) = singletons[key] as T?

    private fun put(
        key: Singleton<*>,
        value: Any,
    ) = synchronized(this) { singletons += key to value }
}

@InternalApi
object SingletonInitializerStore {
    @Volatile
    private var initializers = mapOf<Singleton<*>, () -> Any>()

    fun clear() = synchronized(this) { initializers = emptyMap() }

    fun <T : Any> putIfAbsent(
        key: Singleton<T>,
        initializer: () -> T,
    ): (() -> T)? = get(key) ?: synchronized(key) {
        get(key)?.let { return it }
        put(key, initializer)
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstance(key: Singleton<T>) = get(key)?.invoke()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> get(key: Singleton<T>) = initializers[key] as (() -> T)?

    private fun put(
        key: Singleton<*>,
        value: () -> Any
    ) = synchronized(this) { initializers += key to value }
}

@RequiresOptIn(
    "This is for internal use only, and should not be used in production code. " +
            "Doing so may cause unexpected behavior."
)
annotation class InternalApi
