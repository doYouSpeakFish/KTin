![Maven Central Version](https://img.shields.io/maven-central/v/io.github.doyouspeakfish.ktin/core)
![GitHub License](https://img.shields.io/github/license/doYouSpeakFish/KTin)


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

# Why use KTin?
- Ultra simple dependency injection
- No code generation, so faster builds
- Lazy instantiation and no dependency graph construction on startup, so faster app startup times
- Leverages built-in kotlin functionality for dependency injection, giving you the full power of the programming language

# Setup
Add the following dependencies:
```kotlin
dependencies {
    implementation("io.github.doyouspeakfish.ktin:core:1.0.1")
    testImplementation("io.github.doyouspeakfish.ktin:test:1.0.1")
}
```

# Declaring Singletons
Let's say we have the following class:
```kotlin
class MySingleton(aDependency: Dependency)
```
We have two ways of providing this class as a singleton:
```kotlin
object MySingletonProvider : Singleton<MySingleton>() {
    override fun create() = MySingleton(aDependency = Dependency())
}
```
Or if we want to be able to fake it in tests, we can set up what to inject at runtime:
```kotlin
object MySingletonProvider : InjectedSingleton<MySingleton>()

fun main() {
    MySingletonProvider.inject { MySingleton(aDependency = Dependency()) } // In tests we could inject a fake instead
}
```
Instances can then be retrieved by calling either the `invoke` operator function providing a constructor style syntax, or the `getInstance` function on the provider:
```kotlin
val mySingleton = MySingletonProvider() // This creates a singleton instance
val mySingletonAgain = MySingletonProvider() // This is the exact same instance as mySingleton
val mySingletonYetAgain = MySingletonProvider.getInstance() // This provides another way to retrieve the singleton instance
```
# Zero-arg constructors for everything
The provider can be the companion object of the class. If we do this, we have a zero argument constructor that provides singleton instances:
```kotlin
class MySingleton(aDependency: Dependency) {
    companion object : Singleton<MySingleton>() {
        override fun create() = MySingleton(aDependency = Dependency())
    }
}

fun main() {
    val mySingleton = MySingleton()
}
```

If we combine this with kotlin default arguments, that's all we need to implement dependency injection in an application.
We can setup every class to have a no-args constructor:
```kotlin
class MyUseCase(mySingleton: MySingleton = MySingleton()) // MySingleton() returns a singleton instance

class MyViewModel(myUseCase: MyUseCase = MyUseCase()) // MyUseCase() returns a fresh instance every time
```

We can also easily provide some arguments at runtime if we want. For example:
```kotlin
class MyViewModel(
    id: String, // Not injected, needs to be provided at runtime
    myUseCase: MyUseCase = MyUseCase(), // Injected automatically
)

val viewModel = MyViewModel(id = "123")
```

# Testing
To handle cleanup of singleton instances between tests, a `KTinTestRule` should be used. For example, we might want to inject
a coroutine dispatcher:
```kotlin
object IoDispatcher : InjectedSingleton<CoroutineDispatcher>()

fun main() {
    IoDispatcher.inject { Dispatchers.IO }
}
```
In tests, we can then inject a test dispatcher:
```kotlin
class MyTests {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val testRule = KTinTestRule()

    @Before
    fun before() {
        IoDispatcher.inject { testDispatcher }
    }
    
    @Test
    fun aTest() {
        // ...
    }
}
```
# Injecting factories
One scenario we might have, is that we want to be able to fake something in tests that is not a singleton. 
In this case, we can create a singleton factory for providing instances. Let's say we have the following:
```kotlin
interface MyUseCase

class DefaultMyUseCase : MyUseCase
```
We can create a factory for providing instances, and setup what to inject at runtime:
```kotlin
fun interface MyUseCaseFactory {
    fun createInstance(): MyUseCase
    
    // The factory is a singleton that is set at runtime
    companion object : InjectedSingleton<MyUseCaseFactory>()
}

fun main() {
    MyUseCaseFactory.inject {
        MyUseCaseFactory { DefaultMyUseCase() }
    } // Or in tests, a fake can be injected
}
```
To make this nicer, we can add an `invoke` method to the companion object of `MyUseCase` that uses the factory to provide instances:
```kotlin
interface MyUseCase {
    companion object {
        operator fun invoke() = MyUseCaseFactory().createInstance()
    }
}
```
Then we can create instances of `MyUseCase` as if it was a concrete type with a zero-arg constructor:
```kotlin
val firstMyUseCase = MyUseCase()
val secondMyUseCase = MyUseCase()
```
