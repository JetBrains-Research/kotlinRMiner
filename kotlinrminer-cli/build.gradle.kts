dependencies {
    implementation(project(":kotlinrminer-common"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.3.72")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "org.jetbrains.research.kotlinrminer.KotlinRMiner"
    }

    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA", "misc/**")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
