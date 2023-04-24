dependencies {
    implementation(project(":kotlinrminer-common"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.2.1.201812262042-r")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.8.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.jetbrains:annotations:24.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testImplementation("org.mockito:mockito-core:4.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "org.jetbrains.research.kotlinrminer.cli.KotlinRMiner"
    }

    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
