/*
 * SPDX-FileCopyrightText: Octavia Togami <octy@octyl.net>
 * SPDX-License-Identifier: MPL-2.0
 */

plugins {
    java
    application
    id("net.octyl.level-headered") version "0.1.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.compileJava {
    options.compilerArgs = listOf("-parameters")
}

application {
    mainClass.set("net.octyl.ap.logkeeper.ApLogkeeper")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ap.client)
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)

    implementation(libs.tinylog.api)
    implementation(libs.tinylog.jul)
    runtimeOnly(libs.tinylog.impl)
    runtimeOnly(libs.tinylog.slf4j)
}

levelHeadered {
    headerTemplate(rootProject.file("HEADER.txt"))
}
