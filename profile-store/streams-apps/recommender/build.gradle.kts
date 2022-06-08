val confluentVersion: String by project
val kafkaVersion: String by project
val slf4jVersion = "1.7.25"
val log4jVersion = "1.2.17"
val jettyVersion = "9.4.18.v20190429"
val jerseyVersion = "2.28"
val jaxbVersion = "2.2.11"


dependencies {
    implementation(project(":common"))
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-log4j12:$slf4jVersion")
    implementation("log4j:log4j:$log4jVersion")
    implementation("javax.ws.rs:javax.ws.rs-api:2.1")
    implementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:$jerseyVersion")
    implementation("org.glassfish.jersey.inject:jersey-hk2:$jerseyVersion")
    implementation("javax.xml.bind:jaxb-api:$jaxbVersion")
    implementation("com.sun.xml.bind:jaxb-core:$jaxbVersion")
    implementation("com.sun.xml.bind:jaxb-impl:$jaxbVersion")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:$jerseyVersion")
    implementation("javax.activation:activation:1.1.1")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
}

jib {
    to {
        image = "bakdata/quick-demo-recommender:" + project.version
    }
}
