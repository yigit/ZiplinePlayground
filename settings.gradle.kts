enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://androidx.dev/kmp/builds/13613460/artifacts/snapshots/repository")
            content {
                includeGroup("androidx.kruth")
            }
        }
    }
}

rootProject.name = "ZiplinePlayground"
include(":androidApp")
include(":shared")