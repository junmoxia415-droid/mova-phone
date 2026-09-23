pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MovaPhone"

includeBuild("build-logic")

include(":app")

// ---------- core ----------
listOf("common", "designsystem", "database", "security", "permissions", "navigation", "logging")
    .forEach { include(":core:$it") }

// ---------- domain ----------
listOf("contacts", "calls", "emergency", "automation")
    .forEach { include(":domain:$it") }

// ---------- data ----------
listOf("contacts", "calls", "messages", "emergency", "location", "automation")
    .forEach { include(":data:$it") }

// ---------- services ----------
listOf("calls", "sms", "location", "notifications")
    .forEach { include(":services:$it") }

// ---------- feature ----------
listOf(
    "home", "dialer", "calls", "contacts", "favorites", "emergency", "messages",
    "location", "automation", "security", "driving", "settings", "about", "smart-assistant"
).forEach { include(":feature:$it") }
