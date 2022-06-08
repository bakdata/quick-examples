val commonsCSVVersion: String by project

description = "tsv data producer"

dependencies {
    implementation("org.apache.commons:commons-csv:$commonsCSVVersion")
    implementation("org.slf4j:slf4j-api:1.7.25")
}