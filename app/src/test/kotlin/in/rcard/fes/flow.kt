package `in`.rcard.fes

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration

// Thanks to Marcin Moskała for this snippet from his book "Kotlin Coroutines"
suspend fun <T> Flow<T>.toListDuring(duration: Duration): List<T> =
    coroutineScope {
        val result = mutableListOf<T>()
        val job =
            launch {
                this@toListDuring.collect(result::add)
            }
        delay(duration)
        job.cancel()
        return@coroutineScope result
    }
