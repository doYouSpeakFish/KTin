# What is KTin?
KTin is a very lightweight and minimalist dependency injection framework, built with a philosophy that the framework 
should not attempt to replicate functionality that the programming language is already able to easily provide. Since 
Kotlin already includes default arguments and singleton objects, manual dependency injection is very easy, almost 
entirely removing the need for a dependency injection framework. KTin handles some issues with this approach to manual
dependency injection and provides a way to use this approach that will also work for tests, allowing fakes to be 
injected and ensuring singletons are cleared between each test.

KTin is a very minimalist approach to dependency injection. It provides two abstract classes `Singleton` and 
`InjectedSingleton`, and a test rule `KTinTestRule`. We believe, that on top of the tools provided by the kotlin
programming language, this is all you need to implement dependency injection.

# Declaring Singletons
A singleton class can be declared using KTin by adding to the class, a companion object that extends the `Singleton` 
abstract class.

Example:
```kotlin
class MySingleton(aDependency: Dependency) {
    companion object : Singleton<MySingleton>() {
        override fun create() = MySingleton(
            aDependency = Dependency()
        )
    }
}
```

The `Singleton` abstract class provides a `getInstance` method for retrieving the singleton instance, and an `invoke`
operator method for convenience, allowing the companion object to be called like a constructor to retrieve the singleton
instance. The instance will be created lazily when it is first accessed.

Example:

```kotlin
val mySingleton = MySingleton() // This creates a singleton instance
val mySingletonAgain = MySingleton() // This is the exact same instance as mySingleton
val mySingletonYetAgain = MySingleton.getInstance() // This provides another way to retrieve the singleton instance
```

If Multiple instances of the same type are needed, or you need to create a singleton instance of a class where the 
companion object is not available to you, simply create a provider object instead of using the companion object.

Example:
```kotlin
class MySingleton(aDependency: Dependency)

object FirstMySingletonProvider : Singleton<MySingleton>() {
    override fun create() = MySingleton(aDependency = FirstConcreteDependency())
}
object SecondMySingletonProvider : Singleton<MySingleton>() {
    override fun create() = MySingleton(aDependency = SecondConcreteDependency())
}

// Two different singleton instances are now available via the two providers:
val firstSingleton: MySingleton = FirstMySingletonProvider()
val secondSingleton: MySingleton = SecondMySingletonProvider()
```

# Faking Singletons
To inject a fake singleton instance, have this provider or companion object extend the `InjectedSingleton` class. This
provides an `inject` method that can be called to inject an initializer at runtime for creating instances.

A typical example would be injecting a `CoroutineDispatcher`. In tests, a test dispatcher can be injected instead. For
example:
```kotlin
object IoDispatcherProvider : InjectedSingleton<CoroutineDispatcher>()

fun main() {
    // Choose what to inject at runtime before launching the rest of the app code
    IoDispatcherProvider.inject { Dispatchers.IO }
    
    // Application code can now run using the injected dispatcher
}
```

# Testing
To handle cleanup of singleton instances between tests, a `KTinTestRule` should be used. In tests, a test dispatcher 
can be provided:
```kotlin
class MyTests {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val testRule = KTinTestRule()

    @Before
    fun before() {
        IoDispatcherProvider.inject { dispatcher }
    }
    
    @Test
    fun aTest() {
        // ...
    }
}
```

# Injecting non-singleton classes
Simply use kotlin default arguments!

Example:
```kotlin
class MyUseCase(mySingleton: MySingleton = MySingleton()) // MySingleton() returns a singleton instance

class MyViewModel(myUseCase: MyUseCase = MyUseCase()) // MyUseCase() returns a fresh instance every time
```

This makes it very easy to provide some arguments at runtime. For example:
```kotlin
class MyViewModel(
    id: String, // Not injected, needs to be provided at runtime
    myUseCase: MyUseCase = MyUseCase(), // Injected automatically
)

val viewModel = MyViewModel(id = "123")
```

# What if I want to set at runtime which non-singleton class to inject?
In this case, create a factory that is a singleton for providing instances. For example:
```kotlin
interface MyUseCase {
    companion object {
        // Optionally, an invoke method can be used here too to provide a zero arg constructor. 
        // Actual instances will come from the factory which is supplied at runtime.
        operator fun invoke() = MyUseCaseFactory().createInstance()
    }
}

class DefaultMyUseCase : MyUseCase

fun interface MyUseCaseFactory {
    fun createInstance(): MyUseCase
    
    // The factory is a singleton that is set at runtime
    companion object : InjectedSingleton<MyUseCaseFactory>()
}

fun main() {
    MyUseCaseFactory.inject {
        MyUseCaseFactory { DefaultMyUseCase() }
    } // Or in tests, a fake can be injected

    // Two separate instances provided by the factory
    val firstMyUseCase = MyUseCase()
    val secondMyUseCase = MyUseCase()
}
```
