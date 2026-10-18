plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val enableComposeCompilerMetrics = providers.gradleProperty("enableComposeCompilerMetrics")
    .orNull
    ?.toBoolean() == true

android {
    namespace = "com.docforge.core.ui"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        if (enableComposeCompilerMetrics) {
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${layout.buildDirectory.dir("compose-metrics").get().asFile.absolutePath}",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${layout.buildDirectory.dir("compose-reports").get().asFile.absolutePath}"
            )
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.collections.immutable)
}
