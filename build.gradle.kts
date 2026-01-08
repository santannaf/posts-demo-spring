import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.1"
//    id("org.springframework.boot") version "3.5.9" // For Test version 3.5.X
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.11.3"
}

group = "santannaf.demo.brc.rinha.backend"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.23.0")
    }
}

//dependencyManagement {
//    imports {
//        mavenBom("io.opentelemetry:opentelemetry-bom:1.56.0")
//        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.23.0")
//        mavenBom("io.micrometer:micrometer-tracing-bom:1.6.0")
//    }
//}

dependencies {
    // Spring Boot 4.0.1
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Kotlin
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Unit Tests for Spring Boot 4.0.1
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-opentelemetry-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // For Spring 3.5.9
//    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
//    implementation("org.springframework.boot:spring-boot-starter-web")
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
//    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Commons Libraries
//    implementation("com.oracle.database.jdbc:ojdbc11:23.26.0.0.0")
    implementation("com.oracle.database.jdbc:ojdbc11")
//    runtimeOnly("com.oracle.database.jdbc:ojdbc11")
//    runtimeOnly("com.oracle.database.jdbc:ucp11")
//    implementation("com.oracle.database.jdbc:ucp:23.26.0.0.0")
//    implementation("com.oracle.database.spring:oracle-spring-boot-starter-ucp:26.0.0")

    // Traces and Metrics
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")

    // Metrics and Traces for Spring Boot 3.5.X
//    implementation("io.micrometer:micrometer-registry-otlp")
//    implementation("io.micrometer:micrometer-tracing-bridge-otel")
//    implementation("io.micrometer:micrometer-tracing")
//    implementation("io.opentelemetry:opentelemetry-extension-kotlin")

//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
//    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Unit Tests for Spring Boot 3.5.9
//    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
//    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

tasks.register<Exec>("runCustomJar") {
    group = "application"
    description = "Run custom jar"
    dependsOn("bootJar")
    val appName = "app.jar"
    val addressesBuild = "./build/libs/$appName"
    commandLine(
        "java",
        "-agentlib:native-image-agent=config-merge-dir=./src/main/resources/META-INF/native-image/",
        "-jar",
        addressesBuild
    )
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("app")
            verbose.set(true)
            debug.set(true)
            configurationFileDirectories.from(file("src/main/resources/META-INF/native-image"))
            buildArgs("--color=always", "-H:+AddAllCharsets",
                "--initialize-at-run-time=oracle.jdbc.driver.OracleDriver",
                "--initialize-at-run-time=oracle.jdbc.driver.T4CConnection")
        }
    }
}


