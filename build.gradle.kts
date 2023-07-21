import kotlinx.benchmark.gradle.NativeBenchmarkExec
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.json.*
import kotlin.RuntimeException
import kotlin.math.abs

plugins {
    kotlin("multiplatform") version "1.8.21"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.json:json:20230618")
        classpath("org.apache.commons:commons-math3:3.6.1")
    }
}

fun parseResults(benchmarkName: String, path: File): DoubleArray {
    val doc = JSONArray(path.readText()).first {
        it as JSONObject
        it.getString("benchmark") == benchmarkName
    } as JSONObject
    val data = doc.getJSONObject("primaryMetric").getJSONArray("rawData")
    return data.getJSONArray(0).map { (it as Number).toDouble() }.toDoubleArray()
}

fun checkNoTrendExists(name: String, data: DoubleArray) {
    for (idx in 1 until data.size) {
        if (abs(data[idx] - data[idx - 1]) / data[idx] >= 0.1) {
            throw RuntimeException(
                "[$name] Results are unstable between iterations ${idx - 1} and ${idx}: ${data.contentToString()}"
            )
        }
    }
}

fun testStability(resultsFile: File, vararg names: String) {
    names.forEach {
        val data = parseResults(it, resultsFile)
        checkNoTrendExists(it, data)
    }
}

fun testDCE(resultsFile: File, dce: String, noDce: String) {
    val dceData = parseResults(dce, resultsFile)
    val noDceData = parseResults(noDce, resultsFile)
    val dceE = Mean().evaluate(dceData)
    val noDceE = Mean().evaluate(noDceData)
    val dceStd = StandardDeviation().evaluate(dceData)

    val diff = (dceE - noDceE) / dceE
    if (diff < 0.1 || abs(dceE - noDceE) < 3 * dceStd) {
        throw RuntimeException("Seems like use of the blackhole didn't prevent DCE, please check the results manually.")
    }
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val native = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.8")
            }
        }
        val nativeMain by getting {
            dependsOn(commonMain)
        }
    }
}

benchmark {
    configurations {
        named("main") {
            iterationTime = 10
            iterationTimeUnit = "sec"
            iterations = 10
        }
    }

    targets {
        register("native")
    }
}

tasks {
    withType(NativeBenchmarkExec::class) {
        doLast {
            this as NativeBenchmarkExec
            testStability(
                this.reportFile,
                "org.example.SampleBenchmark.implicitBlackhole",
                "org.example.SampleBenchmark.explicitBlackholeBoolean",
                "org.example.SampleBenchmark.explicitBlackholeByte",
                "org.example.SampleBenchmark.explicitBlackholeShort",
                "org.example.SampleBenchmark.explicitBlackholeInt",
                "org.example.SampleBenchmark.explicitBlackholeChar",
                "org.example.SampleBenchmark.explicitBlackholeLong",
                "org.example.SampleBenchmark.explicitBlackholeFloat",
                "org.example.SampleBenchmark.explicitBlackholeDouble",
                "org.example.SampleBenchmark.explicitBlackholeObject",
            )
            testDCE(
                this.reportFile,
                "org.example.SampleBenchmark.dce", "org.example.SampleBenchmark.noDce"
            )
        }
    }
}
