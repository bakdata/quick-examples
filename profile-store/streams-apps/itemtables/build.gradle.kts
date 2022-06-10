val commonsCSVVersion: String by project

description = "field tables (id join)"

dependencies {
    implementation(project(":common"))
    implementation("org.apache.commons:commons-csv:$commonsCSVVersion")
}

jib {
    to {
        image = "bakdata/quick-demo-profile-items-producer:" + project.version
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
