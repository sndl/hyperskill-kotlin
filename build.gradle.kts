group = "sndl.hyperskill.kotlin"
version = "0.0.1"
description = "Hyperskill Kotlin Projects"

plugins {
    id("tanvd.kosogor") version "1.0.5"
    kotlin("jvm") version "1.3.31" apply true
}

allprojects {
    apply {
        plugin("kotlin")
        plugin("tanvd.kosogor")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    repositories {
        jcenter()
    }
}
