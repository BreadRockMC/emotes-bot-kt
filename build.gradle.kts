plugins {
    java
    val kotlinVersion = "1.8.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "7.1.2"
}


java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "net.dv8tion", name = "JDA", version = project.property("jda_version") as String)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(java.targetCompatibility.majorVersion.toInt())
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = java.targetCompatibility.toString()
    }

    java {
        withSourcesJar()
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${base.archivesName.get()}" }
        }
        manifest {
            attributes("Main-Class" to "dev.kosmx.discordBot.MainKt")
        }
        archiveClassifier.set("slim")
    }

    shadowJar {
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}
