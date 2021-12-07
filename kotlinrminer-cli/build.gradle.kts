dependencies {
    implementation(project(":kotlinrminer-core"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.3.72")
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "org.jetbrains.research.kotlinrminer.KotlinRMiner"
    }

    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA", "misc/**")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
