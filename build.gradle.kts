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
    implementation("org.apache.aries.blueprint:org.apache.aries.blueprint.core:1.10.3")
    implementation("org.apache.aries.blueprint:org.apache.aries.blueprint.api:1.0.1")
    implementation("org.apache.aries:org.apache.aries.util:1.1.3")
    implementation("org.apache.aries.blueprint:blueprint-parser:1.6.1")


    implementation("org.apache.commons:commons-compress:1.21")

    implementation("org.osgi:org.osgi.core:4.3.1")                          // взято из CPI
    compileOnly("org.osgi:org.osgi.service.event:1.4.1")

    implementation("org.apache.felix:org.apache.felix.framework:5.6.12")    // взято из CPI
    implementation("org.apache.felix:org.apache.felix.configadmin:1.9.20")  // взято из CPI
    implementation("org.apache.felix:org.apache.felix.scr:2.1.26")  // взято из CPI CF
    implementation("javax.mail:mail:1.4")

    implementation("org.eclipse.jetty:jetty-servlet:9.4+")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    testImplementation("ch.qos.logback:logback-core:1.2+")

    testImplementation("junit", "junit", "4.12")
    testImplementation("org.eclipse.jetty:jetty-server:9.4+")
    testImplementation("org.apache.olingo:olingo-odata2-api:+")
    testImplementation("org.apache.olingo:olingo-odata2-core:+")

    testImplementation("com.itextpdf:itext7-core:7.2.1")
    testImplementation("com.itextpdf:itext7-kernel:7.2.1")
    testImplementation("com.itextpdf:itext7-io:7.2.1")
    testImplementation("com.itextpdf:itext7-layout:7.2.1")

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
