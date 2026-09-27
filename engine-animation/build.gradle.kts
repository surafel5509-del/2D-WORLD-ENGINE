plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.engineanimation"
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
    api(project(":engine-core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}
