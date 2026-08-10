plugins {
    java
}

group = "dev.glitg"
version = "2.0.0"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper 26.2 has no published Maven API coordinate. The 26.1.2 API is binary-compatible for these events.
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.61-stable")
    compileOnly(files("libs/lifestealz-2.21.1.jar"))
}

tasks {
    compileJava {
        options.release.set(25)
        options.encoding = "UTF-8"
    }
    jar { archiveFileName.set("SMPRules-${project.version}.jar") }
}
