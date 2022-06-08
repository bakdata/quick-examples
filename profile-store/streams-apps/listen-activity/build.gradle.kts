dependencies {
    implementation(project(":common"))
}

jib {
    to {
        image = "bakdata/quick-demo-listen-activity:" + project.version
    }
}
