dependencies {
    implementation(project(":common"))
}

jib {
    to {
        image = "bakdata/quick-demo-listen-count:" + project.version
    }
}
