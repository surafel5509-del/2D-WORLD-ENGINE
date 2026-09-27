pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "2DWorldEngine"
include(":engine-math")
include(":engine-core")
include(":engine-render")
include(":engine-physics")
include(":engine-audio")
include(":engine-animation")
include(":engine-assets")
include(":engine-scripting")
include(":engine-ai")
include(":engine-tilemap")
include(":engine-particles")
include(":engine-ui")
include(":engine-io")
include(":engine-build")
include(":editor-ui")
include(":editor-viewport")
include(":editor-assets")
include(":editor-animation")
include(":editor-scripting")
include(":editor-build")
include(":app")
