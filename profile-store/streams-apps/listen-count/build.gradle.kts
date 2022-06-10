dependencies {
    implementation(project(":common"))
}

jib {
    to {
        image = "bakdata/quick-demo-profile-listenings-count:" + project.version
    }
}
