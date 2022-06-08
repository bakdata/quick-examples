val kafkaVersion: String by project
val confluentVersion: String by project
val streamsBootstrapVersion: String by project
val avroVersion: String by project
val picoCLIVersion: String by project
val fluentKafkaStreamsTestsVersion: String by project

plugins {
    java
    id("com.github.davidmc24.gradle.plugin.avro") version "1.2.0"
    id("io.freefair.lombok") version "5.3.3.3"
    id("com.google.cloud.tools.jib") version "3.1.1"
}

allprojects {
    apply {
        plugin("java")
    }
    repositories {
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
    }
}

subprojects {
    group = "com.bakdata.profilestore"
    version = "1.0.0"
    java.sourceCompatibility = JavaVersion.VERSION_11

    apply {
        plugin("com.github.davidmc24.gradle.plugin.avro")
        plugin("io.freefair.lombok")
        plugin("com.google.cloud.tools.jib")
    }
    dependencies {
        implementation("com.bakdata.kafka:streams-bootstrap:$streamsBootstrapVersion")
        implementation("org.apache.avro:avro:$avroVersion")
        implementation("io.confluent:kafka-streams-avro-serde:$confluentVersion")
        implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
        implementation("info.picocli:picocli:$picoCLIVersion")
        testImplementation("com.bakdata.fluent-kafka-streams-tests:fluent-kafka-streams-tests-junit5:$fluentKafkaStreamsTestsVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    }
    tasks.test {
        useJUnitPlatform()
    }
}
