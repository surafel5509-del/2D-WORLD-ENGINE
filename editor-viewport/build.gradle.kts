plugins { id("com.android.library"); kotlin("android") }
android {
    namespace = "world.engine.editorviewport"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    api(project(":engine-animation"))
    api(project(":engine-physics"))
    api(project(":engine-render"))
}
