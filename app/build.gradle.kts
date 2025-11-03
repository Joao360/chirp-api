plugins {
    id("chirp.spring-boot-app")
}

group = "com.joaograca"
version = "0.0.1-SNAPSHOT"
description = "Chirp API"

dependencies {
    implementation(projects.user)
    implementation(projects.chat)
    implementation(projects.notification)
    implementation(projects.common)
}
