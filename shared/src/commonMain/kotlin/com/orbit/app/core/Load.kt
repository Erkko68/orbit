package com.orbit.app.core

import co.touchlab.kermit.Logger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen

/** What a ViewModel knows of something it reads: nothing yet, that the read failed, or its latest value. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>

    data object Failed : Load<Nothing>

    data class Loaded<out T>(val value: T) : Load<T>
}

val <T> Load<T>.valueOrNull: T? get() = (this as? Load.Loaded)?.value

/**
 * A read the screen is about, as a [Load]: [Load.Loading] until the first value. A failure is logged as
 * [what] and shows as [Load.Failed] until [retry] receives, then the read starts over. It never throws, so
 * a failed read cannot crash the ViewModel that collects it.
 */
fun <T> Flow<T>.asLoad(what: String, retry: ReceiveChannel<Unit>): Flow<Load<T>> =
    map<T, Load<T>> { Load.Loaded(it) }
        .retryWhen { cause, _ ->
            Logger.e(cause) { "$what failed" }
            emit(Load.Failed)
            retry.receive()
            emit(Load.Loading)
            true
        }
        .onStart { emit(Load.Loading) }

/** A read the screen can do without, such as a name in a header: a failure is logged as [what] and reads as [fallback]. */
fun <T> Flow<T>.orOnFailure(what: String, fallback: T): Flow<T> = catch { cause ->
    Logger.e(cause) { "$what failed" }
    emit(fallback)
}
