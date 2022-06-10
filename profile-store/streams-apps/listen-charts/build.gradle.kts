val confluentVersion: String by project

dependencies {
    implementation(project(":common"))
}

jib {
    to {
        image = "bakdata/quick-demo-profile-listenings-charts:" + project.version
    }
}
