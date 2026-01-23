plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.11.4"
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
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.24.0")
    }
}

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


    // Commons Libraries
    implementation("com.oracle.database.jdbc:ojdbc11")

    // Traces and Metrics
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
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
            buildArgs(
                "--color=always",
                "-H:+AddAllCharsets",
                "-J-Dfile.encoding=UTF-8",
                "-J-Duser.language=pt",
                "-J-Duser.country=BR",
                "-J-Duser.timezone=America/Sao_Paulo"
            )
        }
    }
}


