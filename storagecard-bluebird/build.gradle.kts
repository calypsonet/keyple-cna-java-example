///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

plugins {
  id("com.diffplug.spotless") version "8.2.1"
  id("com.android.application") version "8.13.2" apply false
  id("com.android.library") version "8.13.2" apply false
  kotlin("android") version "2.3.0" apply false
}

spotless {
  kotlin {
    target("**/*.kt")
    ktfmt()
    licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
  }
  kotlinGradle {
    target("**/*.kts")
    ktfmt()
  }
}
