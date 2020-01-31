import com.google.protobuf.gradle.*

plugins {
    java
    application
    id("com.google.protobuf") version "0.8.11"
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.27.0"
val protocVersion = "3.7.1"

dependencies {
    implementation("io.grpc:grpc-netty:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    implementation("io.grpc:grpc-services:${grpcVersion}")
    implementation("javax.annotation:javax.annotation-api:1.3.1")
}

application {
    mainClassName = "server.TestServer"
}

tasks.register("runServer") {
    dependsOn(tasks.getByName("run"))
}

project.the<SourceSetContainer>()["main"].java {
    srcDir(buildDir.path + "/generated/source/proto/main/java")
    srcDir(buildDir.path + "/generated/source/proto/main/grpc")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protocVersion}"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
    }

    generateProtoTasks {
        ofSourceSet("main").forEach { task ->
            task.plugins {
                id("grpc")
            }
            task.generateDescriptorSet = true
            //task.descriptorSetOptions.path = ...
        }
    }
}
