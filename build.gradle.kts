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
    maven("https://libraries.minecraft.net") {
        name = "Mojang"
    }
    maven("https://maven.kosmx.dev/"){
        name = "KosmX"
    }
}

dependencies {
    implementation(group = "net.dv8tion", name = "JDA", version = project.property("jda_version") as String)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")

    implementation("com.mojang:brigadier:${project.property("brigadier_version")}")
    implementation("dev.kosmx.player-anim:anim-core:${project.property("player_anim")}")

    //compileOnly("org.slf4j:slf4j-api:${project.property("slf4j_version")}")
    runtimeOnly("org.slf4j:slf4j-jdk14:${project.property("slf4j_version")}")

    // findbugs, kotlin can use nullability annotations
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
}

tasks {

    test {
        useJUnitPlatform()
    }

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
