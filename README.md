# Zipline Sample

This is a sample project using CashApp Zipline

The `shared` module has a JS target which gets bundled as a Zipline artifact into its host platform
targets.

To be able to ship JS bundle with the app (for then initial load), there is some gradle magic:

* There are 2 `PackageAllZiplineBundlesTask` tasks, (1 for development and 1 for production js targets)
whose sole purpose is to put all zipline outputs into 1 folder structured in the following format:
`zipline/<appName>`
* The output of that task is copied to each platform using different methods:
  * For JVM and Android, it is shipped as part of Resources (regular java class loader resources)
  * For iOS, it is copied as a build step. (open the XCode project to see it)
  * For simulator or host (mac) tests, it is copied into the test bundle directory via gradle.

Once that is ready, the `BundledZiplineLoader` loads it from the right location. Then it is used
by the singleton `MyZiplineServiceProvider` to load the service.

You can trace the `PlaygroundTest` to see how it works.



### License

    Copyright 2025 Yigit Boyar

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

