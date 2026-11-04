package dev.kyrylost.smartstatusbar

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlin.time.Duration

/**
 * Like [debounce], but if values keep arriving the latest one is still emitted at most [maxWait]
 * after the first value of the burst. With `maxWait == Duration.INFINITE` this is plain [debounce].
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal fun <T> Flow<T>.debounceWithMaxWait(timeout: Duration, maxWait: Duration): Flow<T> {
    if (maxWait == Duration.INFINITE) return debounce(timeout)
    val upstreamFlow = this
    return flow {
        coroutineScope {
            val upstream = produce { upstreamFlow.collect { send(it) } }
            while (true) {
                var latest = upstream.receiveCatching()
                if (latest.isClosed) break
                val deadline = async { delay(maxWait) }
                var settled = false
                var upstreamClosed = false
                while (!settled) {
                    select {
                        // Deadline first so a constantly busy upstream cannot starve it.
                        deadline.onAwait { settled = true }
                        upstream.onReceiveCatching { r ->
                            if (r.isClosed) {
                                upstreamClosed = true
                                settled = true
                            } else {
                                latest = r
                            }
                        }
                        onTimeout(timeout) { settled = true }
                    }
                }
                deadline.cancel()
                emit(latest.getOrThrow())
                if (upstreamClosed) break
            }
        }
    }
}
