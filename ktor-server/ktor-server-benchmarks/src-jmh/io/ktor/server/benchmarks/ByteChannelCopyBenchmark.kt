package io.ktor.server.benchmarks

import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlinx.io.core.*
import org.openjdk.jmh.annotations.*

@Suppress("KDocMissingDocumentation")
@State(Scope.Benchmark)
@UseExperimental(ExperimentalIoApi::class)
class ByteChannelCopyBenchmark {
    @Benchmark
    fun execute(): Unit = runBlocking<Unit> {
        val ch = ByteChannel(false)

        launch {
            ch.writeSuspendSession {
                var total = 1000_000L // 30L * 1024L * 1024L * 1024L

                while (total > 0L) {
                    val r = request(1)
                    if (r == null) {
                        tryAwait(1)
                        continue
                    }

                    val size = minOf(r.writeRemaining.toLong(), total).toInt()

                    r.writeDirect(1) {
                        it.position(it.position() + size)
                    }

                    written(size)
                    total -= size
                }

                flush()
            }

            ch.close()
        }

        launch {
            ch.lookAheadSuspend {
                while (true) {
                    val r = request(0, 1)
                    if (r == null) {
                        if (!awaitAtLeast(1)) break
                        continue
                    }

                    val size = r.remaining()
                    r.position(r.position() + size)
                    consumed(size)
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    benchmark(args) {
        threads = 32
        run<ByteChannelCopyBenchmark>()
    }
}

