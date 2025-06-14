# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Building the project
- `./gradlew build` - Builds all targets
- `./gradlew :shared:build` - Builds shared module only
- `./gradlew :androidApp:build` - Builds Android app

### Running tests
- `./gradlew test` - Runs all tests
- `./gradlew :shared:hostTest` - Runs host platform tests (JVM/Apple)
- `./gradlew :shared:jsTest` - Runs JS tests
- `./gradlew :shared:connectedAndroidTest` - Runs Android instrumented tests

### Zipline-specific tasks
- `./gradlew :shared:packageDevelopmentZipline` - Creates development zipline bundles
- `./gradlew :shared:packageProductionZipline` - Creates production zipline bundles
- `./gradlew :shared:compileJsDevelopmentZipline` - Compiles JS for development zipline
- `./gradlew :shared:compileJsProductionZipline` - Compiles JS for production zipline

## Architecture Overview

This is a Kotlin Multiplatform project demonstrating CashApp Zipline integration across multiple platforms.

### Core Architecture
- **shared/commonMain**: Common interface definitions (`MyZiplineService`)
- **shared/jsMain**: JavaScript implementations that run in Zipline runtime (`RealMyZiplineService`, `Launch.kt`)
- **shared/hostMain**: Host platform code for loading and managing Zipline services (`BundledZiplineLoader`, `MyZiplineServiceProvider`)
- **Platform-specific**: AndroidJVM, Apple, JVM implementations that provide platform-specific Zipline loading

### Zipline Bundle Management
The project uses custom Gradle tasks to package Zipline bundles:
- `PackageAllZiplineBundlesTask` combines JS outputs into structured bundles
- Bundles are embedded as resources in JVM/Android or copied for iOS/tests
- `BundledZiplineLoader` provides abstraction for loading bundled JS code
- Main function entry point: `com.birbit.ziplineplayground.launchZipline`

### Key Components
- **MyZiplineService**: Interface defining the service contract between host and JS
- **MyZiplineServiceProvider**: Singleton that manages Zipline service lifecycle
- **BundledZiplineLoader**: Handles loading JS from embedded resources
- **RealMyZiplineService**: JS implementation of the service interface

### Target Platforms
- Android (API 24+)
- iOS (arm64, x64, simulator)
- JVM
- macOS (arm64, x64)
- JavaScript (browser)

### Testing Strategy
- Host tests verify service loading and basic functionality
- JS tests run in browser environment
- Android instrumented tests for Android-specific behavior
- iOS tests get bundled JS copied to test bundle directory