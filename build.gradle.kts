import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.yaml.snakeyaml.Yaml
import java.io.FileWriter
import java.io.FileReader

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.yaml:snakeyaml:1.29") // Use the latest version
    }
}

plugins {

    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    id("io.papermc.paperweight.userdev") version "1.5.2"
    java
}
group = project.properties["group"] as String
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    paperweight.paperDevBundle("1.19.4-R0.1-SNAPSHOT")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks.withType<ProcessResources> {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register("replaceTemplate") {
    doLast {
        val mainKotlinDir: File = sourceSets["main"].kotlin.srcDirs.single()
        val templatePackage: String = "tmp.template.kotlinpaperplugintemplate"
        val templatePackageDir: File = File(mainKotlinDir, templatePackage.replace('.', '/'))
        val templateClassFile: File = File(templatePackageDir, "KotlinPaperPluginTemplate.kts")
        if (!templatePackageDir.exists() || !templateClassFile.exists()) {
            error("Template has been replaced")
        }
        val yaml: Yaml = Yaml()
        val pluginyml: MutableMap<String, Any> = HashMap(FileReader("plugin.yml").use { reader ->
            yaml.load(reader) as Map<String, Any>
        })
        val name: String = project.properties["name"]!!.toString()
        val mainClass: String = name + "Plugin"
        val mainPackagePath: String = project.properties["group"]!!.toString() + "." + name.lowercase()
        val mainClassPath: String = "$mainPackagePath.$mainClass"
        pluginyml["name"] = name
        pluginyml["main"] = mainClassPath
        val packageDir = File(mainKotlinDir, mainPackagePath.replace('.', '/'))
        if (!packageDir.exists()) {
            packageDir.mkdirs()
            val newMainClassFile: File = File(packageDir, "$mainClass.kts")
            templateClassFile.renameTo(newMainClassFile)
            val oldContent: String = newMainClassFile.readText()
            newMainClassFile.writeText(oldContent.replace("KotlinPaperPluginTemplate", mainClass))
        }

    }
}
