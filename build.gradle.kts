plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.codeyogico"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.codeyogico.vectorsearch.MainKt")
    // Enable Lucene 10's SIMD vector scoring (Panama Vector API is still incubating on JDK 21)
    applicationDefaultJvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}

repositories {
    mavenCentral()
}

// Lucene 10 requires Java 21
kotlin {
    jvmToolchain(21)
}

val luceneVersion = "10.4.0"
val ktorVersion = "2.3.8"
val djlVersion = "0.28.0"

dependencies {
    // Lucene — HNSW vector search, BM25, query parser
    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-analysis-common:$luceneVersion")
    implementation("org.apache.lucene:lucene-highlighter:$luceneVersion")

    // DJL + ONNX Runtime for real sentence embeddings (all-MiniLM-L6-v2)
    implementation("ai.djl:api:$djlVersion")
    implementation("ai.djl.huggingface:tokenizers:$djlVersion")
    implementation("ai.djl.onnxruntime:onnxruntime-engine:$djlVersion")

    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Serialization + logging
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest { attributes["Main-Class"] = "com.codeyogico.vectorsearch.MainKt" }
    mergeServiceFiles()
}
