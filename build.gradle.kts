plugins {
    `java-library`
    alias(libs.plugins.shadow)

    id("module-java-versions") apply false
    id("kodari-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

allprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    configurations.all {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

    dependencies {
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)

        api(rootProject.libs.gson)
        api(rootProject.libs.bundles.netty)
    }
}

subprojects {
    apply(plugin = "module-java-versions")
}

dependencies {
    implementation(project(":sdk"))

    shadow(libs.gson)
    shadow(libs.bundles.netty)
}

tasks.shadowJar {
    dependencies {
        include(project(":sdk"))
    }

    archiveClassifier.set("")
}

tasks.jar {
    enabled = false
}
