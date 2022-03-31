val streamsBootstrapVersion: String by project
val logbackVersion: String by project
val slf4jVersion: String by project
val junitVersion: String by project
val fluentKafkaStreamsTestVersion: String by project
val kafkaJunitVersion: String by project
val assertJVersion: String by project

plugins {
    java
    id("com.google.cloud.tools.jib") version "3.2.1"
}

group = "com.bakdata.quick.examples.tinyurl.counter"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(group = "com.bakdata.kafka", name = "streams-bootstrap", version = streamsBootstrapVersion)
    implementation(group = "io.confluent", name = "kafka-streams-avro-serde", version = "6.2.2")

    // Logging
    implementation(group = "ch.qos.logback", name = "logback-classic", version = logbackVersion)
    implementation(group = "ch.qos.logback", name = "logback-core", version = logbackVersion)
    implementation(group = "org.slf4j", name = "slf4j-log4j12", version = slf4jVersion)

    // Testing
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
    testImplementation(
        group = "com.bakdata.fluent-kafka-streams-tests",
        name = "fluent-kafka-streams-tests-junit5",
        version = fluentKafkaStreamsTestVersion
    )
    testImplementation(
        group = "com.bakdata.fluent-kafka-streams-tests",
        name = "schema-registry-mock-junit5",
        version = fluentKafkaStreamsTestVersion
    )
    testImplementation(group = "net.mguenther.kafka", name = "kafka-junit", version = kafkaJunitVersion)
    testImplementation(group = "org.assertj", name = "assertj-core", version = assertJVersion)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

configurations.all {
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j-impl")
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
}

jib {
    to {
        image = "bakdata/quick-demo-tinyurl:" + project.version
    }
}
