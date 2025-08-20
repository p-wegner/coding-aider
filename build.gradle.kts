import org.gradle.internal.classpath.Instrumented.systemProperty
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.jetbrains.intellij.platform") version "2.7.2"
    id("com.github.ben-manes.versions") version "0.51.0"
}

group = "de.andrena"
version = file("version.txt").readText().trim()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.1")
    implementation("net.sourceforge.htmlunit:htmlunit:2.70.0")
    implementation("com.vladsch.flexmark:flexmark:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-util:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-autolink:0.64.8")
    implementation("com.vladsch.flexmark:flexmark:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-util:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-autolink:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-tasklist:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-definition:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-footnotes:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-toc:0.64.8")
    implementation("com.knuddels:jtokkit:1.1.0")
    implementation("io.projectreactor:reactor-core:3.7.0")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.3")
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.5.0")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
        intellijIdeaCommunity("2025.2")
        pluginVerifier()
        instrumentationTools()
        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledPlugin("Git4Idea")

    }
}
intellijPlatform {
    pluginVerification {
        ides {
            recommended()
            ide("IC-2024.2.4")
            ide("IC-2025.2")
        }
    }

}
tasks.test {
    useJUnitPlatform()
}

sourceSets {
    test {
        java {
            srcDirs("src/test/kotlin")
        }
    }
}


tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        sinceBuild.set("242")
        untilBuild.set("252.*")
        version = project.version.toString()
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-XXLanguage:+BreakContinueInInlineLambdas"))
}

tasks.named("runIde") {
    systemProperty("idea.log.level", "INFO") // or INFO, WARN, ERROR
}
