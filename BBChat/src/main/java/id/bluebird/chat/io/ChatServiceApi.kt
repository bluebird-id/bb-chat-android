package id.bluebird.chat.io

import com.google.common.util.concurrent.ListenableFuture
import grpc.ChatServiceGrpc
import grpc.ChatServiceGrpc.ChatServiceFutureStub
import grpc.Chatservice
import id.bluebird.chat.io.network.Result
import id.bluebird.chat.io.network.awaitResult
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import java.util.concurrent.TimeUnit

class ChatServiceApi {

    private val interceptor = object : ClientInterceptor {

        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel
        ): ClientCall<ReqT, RespT> {

            return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(
                    method,
                    callOptions.withDeadlineAfter(30, TimeUnit.SECONDS)
                )
            ) {
                override fun sendMessage(message: ReqT) {

                    super.sendMessage(message)

                }

                override fun request(numMessages: Int) {
                    super.request(numMessages)
                }

                override fun start(responseListener: Listener<RespT>?, headers: Metadata?) {
                    lateinit var metadata: Metadata
                    if (true) {
                        val USRID_KEY: Metadata.Key<String> =
                            Metadata.Key.of("userid", Metadata.ASCII_STRING_MARSHALLER)
                        headers?.put(USRID_KEY, "driver23")
                        headers?.let {
                            metadata = headers
                        }
                    }
                    super.start(responseListener, headers)
                }
            }
        }
    }

    val channel: ManagedChannel = ManagedChannelBuilder.forAddress("34.124.216.166", 6969)
        .usePlaintext()
        .intercept(interceptor)
        .build()

    /*** login or register ***/
    private fun createRegisterParam(
        clientId: String,
        tinodeId: String
    ): Chatservice.RegisterRequest = Chatservice.RegisterRequest.newBuilder()
        .setUserId(clientId)
        .setTinodeId(tinodeId)
        .build()

    suspend fun <T : Any> registerFuture(
        clientId: String,
        tinodeId: String,
        transform: Chatservice.RegisterResponse.() -> T
    ): Result<T> {
        val request = createRegisterParam(clientId, tinodeId)
        return channel.futureStubResult(transform) {
            register(request)
        }
    }

    /*** get participant ***/
    private fun getParticipantRequest(
        orderId: String
    ): Chatservice.GetParticipantsRequest = Chatservice.GetParticipantsRequest.newBuilder()
        .setOrderId(orderId)
        .build()

    suspend fun <T : Any> getParticipantByOrderIdFuture(
        orderId: String,
        transform: Chatservice.GetParticipantsResponse.() -> T
    ): Result<T> {
        val request = getParticipantRequest(orderId)
        return channel.futureStubResult(transform) {
            getParticipants(request)
        }
    }

    /*** call grpc ***/
    suspend fun <T : Any, R : Any> ManagedChannel.futureStubResult(
        transform: T.() -> R,
        action: ChatServiceFutureStub.() -> ListenableFuture<T>
    ): Result<R> {
        val stub = ChatServiceGrpc.newFutureStub(this)
        val future = stub.action()
        val result = future.awaitResult(transform)
        try {
            this.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return result
    }

}