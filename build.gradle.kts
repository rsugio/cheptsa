plugins {
    groovy
    java
}

group = "io.rsug"
version = "0.0.2-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.codehaus.groovy:groovy-all:2.4.21")                 // взято из CPI
    implementation("org.apache.camel:camel-core:2.24.3")                    // взято из CPI
    implementation("org.apache.commons:commons-compress:1.21")

    implementation("org.osgi:org.osgi.core:4.3.1")                          // взято из CPI
    implementation("org.apache.felix:org.apache.felix.framework:5.6.12")    // взято из CPI
    implementation("org.apache.felix:org.apache.felix.configadmin:1.9.20")  // взято из CPI
    implementation("org.apache.felix:org.apache.felix.scr:2.1.26")  // взято из CPI CF
    implementation("javax.mail:mail:1.4")

    implementation("org.eclipse.jetty:jetty-servlet:9.4+")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    testImplementation("junit", "junit", "4.12")
    testImplementation("org.eclipse.jetty:jetty-server:9.4+")
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "cheptsa"
        attributes["Implementation-Version"] = archiveVersion
        attributes["testAttr"] = "12345_7890"
    }
}
tasks.register<Copy>("releases") {
    from(layout.buildDirectory.dir("libs/cheptsa-0.0.2-SNAPSHOT.jar"))
    into(layout.buildDirectory.dir("../releases"))
}
