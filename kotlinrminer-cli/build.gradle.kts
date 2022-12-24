dependencies {
    implementation(project(":kotlinrminer-common"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.2.1.201812262042-r")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.3.72")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testImplementation("org.assertj:assertj-core:3.23.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "org.jetbrains.research.kotlinrminer.cli.KotlinRMiner"
    }

    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
