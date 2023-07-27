package id.bluebird.chat.io.network

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun <T : Any> ListenableFuture<T>.awaitResult(): Result<T> {
    return suspendCancellableCoroutine { continuation ->
        Futures.addCallback(this@awaitResult, object : FutureCallback<T> {
            override fun onSuccess(result: T?) {
                when (result) {
                    null -> continuation.resume(Result.Exception(NullPointerException()))
                    else -> continuation.resume(Result.Ok(result))
                }

            }

            override fun onFailure(t: Throwable?) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resume(Result.Exception(t))
            }
        }, MoreExecutors.directExecutor())

        registerOnCompletion(continuation)
    }
}

suspend fun <T : Any, R : Any> ListenableFuture<T>.awaitResult(transform: T.() -> R): Result<R> {
    return suspendCancellableCoroutine { continuation ->
        Futures.addCallback(this@awaitResult, object : FutureCallback<T> {
            override fun onSuccess(result: T?) {
                when (result) {
                    null -> continuation.resume(Result.Exception(NullPointerException()))
                    else -> continuation.resume(Result.Ok(result.transform()))
                }

            }

            override fun onFailure(t: Throwable?) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resume(Result.Exception(t))
            }
        }, MoreExecutors.directExecutor())

        registerOnCompletion(continuation)
    }
}

private fun ListenableFuture<*>.registerOnCompletion(continuation: CancellableContinuation<*>) {
    continuation.invokeOnCancellation {
        try {
            cancel(true)
        } catch (ex: Throwable) {
            //Ignore cancel exception
        }
    }
}