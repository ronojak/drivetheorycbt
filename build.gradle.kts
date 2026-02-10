// Root build script: declare plugin versions for reuse; apply in modules as needed.

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4" apply false

}
