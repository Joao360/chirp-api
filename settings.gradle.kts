pluginManagement {
    includeBuild("build-logic")
	repositories {
		maven { url = uri("https://repo.spring.io/snapshot") }
		gradlePluginPortal()
	}
}
rootProject.name = "chirp"

include("app")
include("user")
include("chat")
include("notification")
include("common")