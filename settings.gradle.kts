pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public") }
        maven { setUrl("https://repo1.maven.org/maven2/") }
        maven { setUrl("https://bintray.com/wechat-sdk-team/maven") }
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://zendesk.jfrog.io/artifactory/repo") }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { setUrl("https://maven.aliyun.com/nexus/content/groups/public") }
        maven { setUrl("https://repo1.maven.org/maven2/") }
        maven { setUrl("https://bintray.com/wechat-sdk-team/maven") }
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://zendesk.jfrog.io/zendesk/repo") }
        mavenCentral()
    }
}

rootProject.name = "Robot"
include(":app")
