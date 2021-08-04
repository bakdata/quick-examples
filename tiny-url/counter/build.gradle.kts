plugins {
    java
    id("io.freefair.lombok") version "5.1.0"
    id("com.google.cloud.tools.jib") version "2.7.1"
}

group = "com.bakdata.example"
version = "1.0.0"

tasks.withType<Test> {
    maxParallelForks = 4
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    maven(url = "http://packages.confluent.io/maven/")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(group = "com.bakdata.kafka", name = "streams-bootstrap", version = "1.6.0")
    implementation(group = "io.confluent", name = "kafka-streams-avro-serde", version = "6.1.1")

    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.6.0")
    testImplementation(group = "com.bakdata.fluent-kafka-streams-tests", name = "fluent-kafka-streams-tests-junit5", version = "2.3.0")
    testImplementation(group = "com.bakdata.fluent-kafka-streams-tests", name = "schema-registry-mock-junit5", version = "2.3.0")
    testImplementation(group = "net.mguenther.kafka", name = "kafka-junit", version = "2.7.0")
    testImplementation(group = "org.assertj", name = "assertj-core", version = "3.11.1")
    implementation(group = "org.slf4j", name = "slf4j-log4j12", version = "1.7.26")
}

jib {
    to {
        image = "us.gcr.io/d9p-quick/demo/tiny-url-counter:" + project.version
    }
}
