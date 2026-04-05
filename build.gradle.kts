plugins {
    id("java")
    kotlin("jvm")
}

group = "fr.arnaud"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "hytale"
        url = uri("https://maven.hytale.com/release")
    }

}

dependencies {
    implementation("com.hypixel.hytale:Server:+")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(kotlin("stdlib"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<ProcessResources> {
    filteringCharset = "UTF-8"
}

tasks.named<Jar>("jar") {
    from(sourceSets["main"].output)
}

tasks.register<Copy>("copyMod") {
    dependsOn(tasks.named("build"))
    from(tasks.named("jar"))
    into("/Users/arnaud/Desktop/Documents/Developpment/Hytale/server/Server/mods")
}

kotlin {
    jvmToolchain(21)
}
