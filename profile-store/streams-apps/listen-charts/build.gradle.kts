val confluentVersion: String by project

dependencies {
    implementation(project(":common"))
}

jib {
    to {
        image = "bakdata/quick-demo-listen-charts:" + project.version
    }
}
