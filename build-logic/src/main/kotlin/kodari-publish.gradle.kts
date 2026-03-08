plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(tasks.named("shadowJar"))
        }
    }

    repositories {
        maven {
            name = "kodari"
            url = uri("https://repo.kodari.ai/releases")
            credentials { // ~/.gradle/gradle.properties
                username = findProperty("kodariRepoUser") as String?
                password = findProperty("kodariRepoToken") as String?
            }
        }
    }
}