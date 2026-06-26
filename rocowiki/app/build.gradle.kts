plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.qiuh0330.gdtools"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.qiuh0330.gdtools"
        minSdk = 24
        targetSdk = 34
        versionCode = 12
        versionName = "2.10"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            // 直接复用网页版资源（image/ json/），避免在仓库里复制 40MB 图片
            assets.srcDir(rootProject.file("../roco"))
        }
    }

    androidResources {
        // 原生版用不到网页文件，打包时剔除
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~:!index.html:!roco.png:!icon-192.png:!icon-512.png"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
