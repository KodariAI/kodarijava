plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["shadow"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
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