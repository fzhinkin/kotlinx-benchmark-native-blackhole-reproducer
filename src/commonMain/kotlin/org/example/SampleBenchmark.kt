package org.example

import kotlinx.benchmark.*

const val C0 = 1234124124
const val C1 = 4124124
var V = 31231

@State(Scope.Benchmark)
open class SampleBenchmark {
    private val array = ByteArray(8192)

    @Benchmark
    fun implicitBlackhole() = array[42]

    @Benchmark
    fun explicitBlackholeBoolean(blackhole: Blackhole) {
        blackhole.consume(array[42].toInt() == 0)
    }

    @Benchmark
    fun explicitBlackholeByte(blackhole: Blackhole) {
        blackhole.consume(array[42])
    }

    @Benchmark
    fun explicitBlackholeChar(blackhole: Blackhole) {
        blackhole.consume(array[42].toInt().toChar())
    }

    @Benchmark
    fun explicitBlackholeShort(blackhole: Blackhole) {
        blackhole.consume(array[42].toShort())
    }

    @Benchmark
    fun explicitBlackholeInt(blackhole: Blackhole) {
        blackhole.consume(array[42].toInt())
    }

    @Benchmark
    fun explicitBlackholeLong(blackhole: Blackhole) {
        blackhole.consume(array[42].toLong())
    }

    @Benchmark
    fun explicitBlackholeFloat(blackhole: Blackhole) {
        blackhole.consume(array[42].toFloat())
    }

    @Benchmark
    fun explicitBlackholeDouble(blackhole: Blackhole) {
        blackhole.consume(array[42].toDouble())
    }

    @Benchmark
    fun explicitBlackholeObject(blackhole: Blackhole) {
        blackhole.consume(array[42] as Any)
    }

    @Benchmark
    fun dce(blackhole: Blackhole) {
        val dead = C0 / V * C1
    }

    @Benchmark
    fun noDce(blackhole: Blackhole) {
        blackhole.consume(C0 / V * C1)
    }
}
