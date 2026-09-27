plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.enginephysics"
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
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    api(project(":engine-core"))
    implementation("org.jbox2d:jbox2d-library:2.2.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}
