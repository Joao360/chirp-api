plugins {
    id("java-library")
    id("chirp.spring-boot-service")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation(projects.common)

    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql)

    implementation(libs.jwt.api)
    runtimeOnly(libs.jwt.impl)
    runtimeOnly(libs.jwt.jackson)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.spring.boot.starter.test)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}