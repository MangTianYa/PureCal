import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
}

val versionPropsFile = rootProject.file("version.properties")

fun getOrInitVersionProps(): Properties {
    val props = Properties()
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { props.load(it) }
    } else {
        props["VERSION_MAJOR"] = "1"
        props["VERSION_MINOR"] = "1"
        props["VERSION_PATCH"] = "0"
        props["VERSION_CODE"] = "2"
        FileOutputStream(versionPropsFile).use {
            props.store(it, "DotCalendar Version Configuration")
        }
    }
    return props
}

val isBuildingApk = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("assemble", ignoreCase = true) ||
    taskName.contains("package", ignoreCase = true) ||
    taskName.contains("install", ignoreCase = true)
}

val (autoVersionCode, autoVersionName) = run {
    val props = getOrInitVersionProps()
    val code = props.getProperty("VERSION_CODE", "2").toIntOrNull() ?: 2
    val major = props.getProperty("VERSION_MAJOR", "1")
    val minor = props.getProperty("VERSION_MINOR", "1")
    val patch = props.getProperty("VERSION_PATCH", "0").toIntOrNull() ?: 0

    if (isBuildingApk) {
        val nextCode = code + 1
        val nextPatch = patch + 1
        props["VERSION_CODE"] = nextCode.toString()
        props["VERSION_PATCH"] = nextPatch.toString()
        FileOutputStream(versionPropsFile).use {
            props.store(it, "DotCalendar Version Configuration - Auto-updated on build")
        }
        println(">> [DotCalendar] 每次编译自动更新版本号: VersionName = $major.$minor.$nextPatch, VersionCode = $nextCode")
        Pair(nextCode, "$major.$minor.$nextPatch")
    } else {
        Pair(code, "$major.$minor.$patch")
    }
}

android {
    namespace = "com.dotcalendar.app"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.dotcalendar.app"
        minSdk = 26
        targetSdk = 37
        versionCode = autoVersionCode
        versionName = autoVersionName

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            val signingPropsFile = rootProject.file("keystore/signing.properties")
            val signingProps = Properties().apply {
                if (signingPropsFile.exists()) {
                    FileInputStream(signingPropsFile).use { load(it) }
                }
            }
            val ksPath = System.getenv("SIGNING_KEYSTORE_PATH")
                ?: signingProps.getProperty("storeFile")
            val ksPassword = System.getenv("SIGNING_STORE_PASSWORD")
                ?: signingProps.getProperty("storePassword")
            val ksAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: signingProps.getProperty("keyAlias")
            val ksKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: signingProps.getProperty("keyPassword")

            val ksFile = ksPath?.let { rootProject.file(it) }
            if (ksFile != null && ksFile.exists() &&
                !ksPassword.isNullOrBlank() &&
                !ksAlias.isNullOrBlank() &&
                !ksKeyPassword.isNullOrBlank()
            ) {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile?.exists() == true) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.gson)
    implementation(libs.lunar)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.paparazzi)
}

tasks.withType<Test> {
    // Paparazzi 1.3.5 的内置 LayoutLib 尚未适配 Android 17 (API 37) 新增的 Build.VERSION_CODES_FULL 内部类，
    // 在 Paparazzi 官方发布支持 API 37 的版本前，单元测试默认排除 Paparazzi 截图测试，保留业务单元测试正常执行。
    exclude("**/*ScreenshotTest*")
}

