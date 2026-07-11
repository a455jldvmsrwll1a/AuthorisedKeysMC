plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.5.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-SNAPSHOT"
    id ("com.diffplug.spotless") version "8.1.0"
}

spotless {
    setEnforceCheck(false)

    java {
        palantirJavaFormat()
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("26.2.build.+")

    implementation(project(":common"))

    // Source: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
    var bc_ver = project.property("bouncycastle_version") as String
    implementation("org.bouncycastle:bcprov-jdk18on:${bc_ver}")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveBaseName.set("AuthorisedKeysMC-PAPER")

        addMultiReleaseAttribute = false

        relocate("org.bouncycastle", "ph.jldvmsrwll1a.authorisedkeysmc.vendor.bouncycastle")
        exclude("META-INF/services/java.security.Provider")
        exclude("META-INF/versions/**")

        exclude { details ->
            var path = details.path

            var shouldRemove = path.startsWith("org/bouncycastle/x509/")
                    || path.startsWith("org/bouncycastle/pqc/")
                    || path.startsWith("org/bouncycastle/asn1/")
                    || path.startsWith("org/bouncycastle/math/ec/endo/")

            var shouldKeep = path.startsWith("org/bouncycastle/asn1/x9/")
                    || path.startsWith("org/bouncycastle/asn1/ASN1Object")
                    || path.startsWith("org/bouncycastle/asn1/ASN1Encod") // 'e' missing on purpose
                    || path.startsWith("org/bouncycastle/asn1/ASN1Output")

            !shouldKeep && shouldRemove
        }

        isZip64 = true
        minimize()
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version )
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
