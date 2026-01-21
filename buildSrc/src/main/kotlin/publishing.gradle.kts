package buildsrc.convention

//  Signing relies on the following properties being set in the user gradle.properties file:
//
//  mavenCentralUsername=<user-token-username>
//  mavenCentralPassword=<user-token-password>
//  signingInMemoryKeyId=<key-id>
//  signingInMemoryKeyPassword=<password>
//  signingInMemoryKey=<ascii-armoured-key>
//
//  These can also be provided as environment variables by prefixing them with `ORG_GRADLE_PROJECT_` e.g.
//  `ORG_GRADLE_PROJECT_signingInMemoryKey`

plugins {
    id("com.vanniktech.maven.publish")
}

group = "io.github.doyouspeakfish.ktin"
version = "1.0"

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    pom {
        name = "KTin"
        description = "A minimalist dependency injection framework for Kotlin"
        url = "https://github.com/doYouSpeakFish/KTin"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                name = "Edward Sills"
                email = "doyouspeakfish@gmail.com"
                organization = "Edward Sills"
                organizationUrl = "https://github.com/doYouSpeakFish"
            }
        }
        scm {
            developerConnection = "scm:git:git://github.com/doYouSpeakFish/KTin.git"
            connection = "scm:git:ssh://github.com:doYouSpeakFish/KTin.git"
            url = "https://github.com/doYouSpeakFish/KTin"
        }
    }
}
