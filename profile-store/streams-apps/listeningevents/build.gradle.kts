val commonsCSVVersion: String by project
val confluentVersion: String by project
val fluentKafkaStreamsTestsVersion: String by project

description = "listening-events"

dependencies {
    implementation(project(":common"))
    implementation("org.apache.commons:commons-csv:$commonsCSVVersion")
    testImplementation("io.confluent:kafka-schema-serializer:$confluentVersion")
    testImplementation("io.confluent:kafka-streams-avro-serde:$confluentVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("com.bakdata.fluent-kafka-streams-tests:schema-registry-mock-junit5:$fluentKafkaStreamsTestsVersion")
}

jib {
    to {
        image = "bakdata/quick-demo-listeningevents-producer:" + project.version
    }
    extraDirectories {
        paths {
            path {
                setFrom("../../data/LFM-1b-sample")
                into = "/app/data/LFM-1b-sample"
            }
        }
    }
}
