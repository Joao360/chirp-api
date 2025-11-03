import gradle.kotlin.dsl.accessors._46e63eab28c6ec47af58720eaeecc791.dependencyManagement

plugins {
    id("chirp.kotlin-common")
    id("io.spring.dependency-management")
    kotlin("plugin.jpa")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libraries.findVersion("spring-boot").get()}")
    }
}

dependencies {
    implementation(libraries.findLibrary("kotlin-reflect").get())
    implementation(libraries.findLibrary("kotlin-stdlib").get())
    implementation(libraries.findLibrary("spring-boot-starter-web").get())

    testImplementation(libraries.findLibrary("spring-boot-starter-test").get())
    testImplementation(libraries.findLibrary("kotlin-test-junit5").get())
    testRuntimeOnly(libraries.findLibrary("junit-platform-launcher").get())
}