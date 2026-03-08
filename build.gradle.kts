plugins {
    java
    alias(libs.plugins.shadow)

    id("module-java-versions") apply false
    id("kodari-publish")
}

allprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    configurations.all {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

    dependencies {
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)

        implementation(rootProject.libs.gson)
        implementation(rootProject.libs.bundles.netty)
    }
}

subprojects {
    apply(plugin = "module-java-versions")
}

dependencies {
    implementation(project(":sdk"))
}

tasks.shadowJar {
    relocate("io.netty", "ai.kodari.libs.netty")
    relocate("com.google.gson", "ai.kodari.libs.gson")

    archiveClassifier.set("")
}
