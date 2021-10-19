	plugins {
	id("com.gorylenko.gradle-git-properties") version("2.2.4")
	id("org.springframework.boot") version("2.5.5")
	id("io.spring.dependency-management") version("1.0.11.RELEASE")
//	checkstyle
	java
	jacoco
	application


}

group = "com.happymoney"

java.sourceCompatibility = JavaVersion.VERSION_11


gitProperties {
    keys = listOf("git.branch", "git.commit.id", "git.commit.id.abbrev", "git.commit.time", "git.tags", "git.closest.tag.name")
}

repositories {
	mavenCentral()
	maven("https://packages.confluent.io/maven/")
}

sourceSets {
	create("intTest") {
		compileClasspath += sourceSets.main.get().output
		runtimeClasspath += sourceSets.main.get().output
	}
}

configurations {
    all {
        exclude(mapOf("group" to "org.springframework.boot", "module" to "spring-boot-starter-logging"))
    }
}

//configurations["intTestImplementation"].extendsFrom(configurations.testImplementation.get())
//configurations["intTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
	// Platforms
	implementation(platform("org.springframework.boot:spring-boot-dependencies:2.3.4.RELEASE"))

	// Spring dependencies
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// Actuator dependencies
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Logging dependencies
	implementation("org.springframework.boot:spring-boot-starter-log4j2")

	// Validation and devtools dependencies
	implementation("org.springframework.boot:spring-boot-starter-validation")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// Data binding and format dependencies
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
	implementation("com.fasterxml.jackson.core:jackson-databind")


	// Testing dependencies
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(mapOf("group" to "org.junit.vintage", "module" to "junit-vintage-engine"))
	}
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
	testImplementation("org.mockito:mockito-core:3.7.7")
	testImplementation("org.mockito:mockito-junit-jupiter:3.7.7")
	testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.4.2.Final")

	// lombok dependencies
	compileOnly("org.projectlombok:lombok:1.18.20")
	annotationProcessor("org.projectlombok:lombok:1.18.20")

	// Use JUnit Jupiter Engine for testing.
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

	implementation("com.google.code.gson:gson:2.8.6")
	compileOnly("javax.servlet:javax.servlet-api:4.0.1")

	// https://mvnrepository.com/artifact/org.codehaus.jackson/jackson-mapper-asl
	implementation("org.codehaus.jackson:jackson-mapper-asl:1.9.13")

	// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
	implementation("com.fasterxml.jackson.core:jackson-core:2.12.5")

	// https://mvnrepository.com/artifact/org.apache.commons/commons-collections4
	implementation("org.apache.commons:commons-collections4:4.4")

	implementation("com.datadoghq:datadog-api-client:1.1.0")
	// https://mvnrepository.com/artifact/com.googlecode.json-simple/json-simple
	implementation("com.googlecode.json-simple:json-simple:1.1")

	// Mapstruct
	implementation("org.mapstruct:mapstruct:1.4.2.Final")
	implementation("org.mapstruct:mapstruct-processor:1.4.2.Final")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.4.2.Final")
}

tasks.compileJava {
	options.compilerArgs = listOf(
			"-Amapstruct.defaultComponentModel=spring",
			"-Amapstruct.unmappedTargetPolicy=IGNORE"
	)
}

tasks.bootRun {
	environment("spring_profiles_active", "local")
}

tasks.test {
    // Use junit platform for unit tests.
    useJUnitPlatform()
//    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

//val integrationTest = task<Test>("integrationTest") {
//    description = "Runs integration tests."
//    group = "verification"
//    onlyIf { project.hasProperty("intTest")}
//
//    testClassesDirs = sourceSets["intTest"].output.classesDirs
//    classpath = sourceSets["intTest"].runtimeClasspath
//    useJUnitPlatform()
//    shouldRunAfter("test")
//}

//tasks.check { dependsOn(integrationTest) }
//
//tasks.jacocoTestReport {
//    dependsOn(tasks.test)
//    finalizedBy(tasks.jacocoTestCoverageVerification)
//    classDirectories.setFrom(
//            sourceSets.main.get().output.asFileTree.matching {
//                // File Patterns to exclude from testing & code coverage metrics
//                exclude()
//            }
//    )
//}

//tasks.jacocoTestCoverageVerification {
//    violationRules {
//        rule {
//            classDirectories.setFrom(sourceSets.main.get().output.asFileTree.matching {
//                // File Patterns to exclude from testing & code coverage metrics
//                exclude()
//            })
//            limit {
//                // Minimum code coverage % for the build to pass
//                minimum = "0.2".toBigDecimal()  //TODO: Raise this value
//            }
//        }
//    }
//}
//tasks.withType<Checkstyle>().configureEach {
//	reports {
//		xml.required.set(false)
//		html.required.set(true)
//		html.stylesheet = resources.text.fromFile("${projectDir}/../config/xsl/checkstyle.xsl")
//	}
//}
//
//
//tasks.withType<Checkstyle>() {
//	exclude("**/com/happymoney/productionobservability/logging/*.java")
//}
