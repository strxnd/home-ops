plugins {
    java
}

group = "dev.glitg"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.61-stable")
    compileOnly(files("libs/lifestealz-2.21.1.jar"))
}

tasks {
    compileJava {
        options.release.set(25)
        options.encoding = "UTF-8"
    }
    processResources {
        filteringCharset = "UTF-8"
    }
    jar {
        archiveFileName.set("SMPRules-${project.version}.jar")
    }
}
