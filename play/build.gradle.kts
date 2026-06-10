plugins {
    alias(libs.plugins.shadow)
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    implementation(project(":JVM-MCLCore"))
}

tasks.shadowJar {
    archiveClassifier.set(null as String?)

    manifest.attributes(
        "Created-By" to "Copyright(c) 2026 Open Code Studio.",
        "Main-Class" to "org.Open_code_Studio.jmcl.play.Launcher",
        "Multi-Release" to "true"
    )
}

tasks.jar {
    enabled = false
    dependsOn(tasks["shadowJar"])
}