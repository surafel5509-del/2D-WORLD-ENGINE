plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.enginerender"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.all { it.useJUnitPlatform() } }
}
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    api(project(":engine-core"))
}
