plugins {
    java
    id("io.freefair.lombok") version "5.1.0"
    id("com.google.cloud.tools.jib") version "2.8.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.0.0"
}

group = "com.bakdata.demo"

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
    implementation(group = "com.bakdata.seq2", name = "seq2", version = "1.0.0")
    implementation(group = "io.confluent", name = "kafka-streams-avro-serde", version = "6.0.2")
    implementation(group = "com.bakdata.kafka", name = "streams-bootstrap", version = "1.6.0")
    implementation(group = "org.slf4j", name = "slf4j-log4j12", version = "1.7.26")
}

jib {
    to {
        image = "bakdata/quick-demo-monitoring-trip-aggregator"
    }
}
