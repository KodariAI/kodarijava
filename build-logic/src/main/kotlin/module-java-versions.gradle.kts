val java8 = setOf(
    "sdk"
)

plugins {
    `java-library`
}

java {
    toolchain {
        val version = when (project.name) {
            in java8 -> 8
            else -> 8
        }
        languageVersion.set(JavaLanguageVersion.of(version))
    }
}