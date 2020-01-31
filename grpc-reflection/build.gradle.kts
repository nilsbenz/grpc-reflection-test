plugins {
    java
    application
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.27.0"
dependencies {
    implementation("io.grpc:grpc-okhttp:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    implementation("io.grpc:grpc-services:${grpcVersion}")
}

application {
    mainClassName = "grpcreflection.CLI"
}
