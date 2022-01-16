plugins {
    groovy
    java
}

group = "io.rsug"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.codehaus.groovy:groovy-all:2.4.21")
    implementation("org.apache.camel:camel-core:2.24.3")
    implementation("org.apache.camel:camel-core:2.24.3")
    implementation("org.apache.commons:commons-compress:1.21")

//    implementation("org.apache.camel:camel-servlet:2.24.3")
    implementation("org.osgi:org.osgi.core:4+")
    implementation("org.apache.felix:org.apache.felix.framework:5.6.12")

    testImplementation("junit", "junit", "4.12")
    testImplementation("org.eclipse.jetty:jetty-server:9.4+")
    implementation("org.eclipse.jetty:jetty-servlet:9.4+")

}
