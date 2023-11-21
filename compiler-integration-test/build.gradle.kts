@file:Suppress("UnstableApiUsage")


plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  namespace = "land.sungbin.composeinvestigator.compiler.test"
  compileSdk = 34

  defaultConfig {
    minSdk = 21
  }

  sourceSets {
    getByName("test").java.srcDir("src/main/kotlin")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true

      all { test ->
        test.useJUnitPlatform()
      }
    }
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.compose.core.get()
  }
}

kotlin {
  compilerOptions {
    optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    optIn.add("org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction")
    freeCompilerArgs.addAll(
      "-P",
      "plugin:land.sungbin.composeinvestigator.compiler:verbose=true",
    )
  }
}

dependencies {
  testImplementation(projects.runtime)
  testImplementation(projects.compiler)

  testImplementation(libs.compose.compiler)
  testImplementation(libs.compose.material)

  testImplementation(libs.kotlin.compiler.embedded)
  testImplementation(libs.test.kotlin.compilation)
  testImplementation(libs.test.kotlin.coroutines) {
    because("https://github.com/Kotlin/kotlinx.coroutines/issues/3673")
  }

  testImplementation(libs.test.mockk)
  testImplementation(libs.test.kotest)
  testImplementation(libs.test.robolectric) {
    because("https://stackoverflow.com/a/64287388/14299073")
  }

  testImplementation(libs.test.junit.core)
  testRuntimeOnly(libs.test.junit.enigne)
  testImplementation(libs.test.junit.compose)

  kotlinCompilerPluginClasspath(projects.compiler)
}
