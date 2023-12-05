package id.bluebird.chat.io

import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import grpc.ChatServiceGrpc
import grpc.ChatServiceGrpc.ChatServiceFutureStub
import grpc.Chatservice
import id.bluebird.chat.NotifPipeline
import id.bluebird.chat.Platform
import id.bluebird.chat.io.network.Result
import id.bluebird.chat.io.network.awaitResult
import id.bluebird.chat.sdk.Cache
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

class ChatServiceApi(
    private val userId: String?,
) {

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
                    if (userId != null) {
                        val USRID_KEY: Metadata.Key<String> =
                            Metadata.Key.of("userid", Metadata.ASCII_STRING_MARSHALLER)
                        headers?.put(USRID_KEY, userId)
                    }
                    if (Cache.getToken().token?.isNotEmpty() == true) {
                        val TOKEN_KEY: Metadata.Key<String> =
                            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
                        headers?.put(TOKEN_KEY, "Bearer ${Cache.getToken().token}")
                    }
                    headers?.let {
                        metadata = headers
                    }
                    super.start(responseListener, headers)
                }
            }
        }
    }

    val channel: ManagedChannel = ManagedChannelBuilder.forAddress(
        "34.124.216.166",
        6969
    )
        .usePlaintext()
        .intercept(interceptor)
        .build()

    /*** generate new token ***/

    private fun createGenerateTokenParam(
        clientId: String,
        clientSecret: String
    ): Chatservice.GenerateTokenRequest = Chatservice.GenerateTokenRequest.newBuilder()
        .setClientId(clientId)
        .setClientSecret(clientSecret)
        .build()

    suspend fun <T : Any> generateNewToken(
        clientId: String,
        clientSecret: String,
        transform: Chatservice.GenerateTokenResponse.() -> T
    ): Result<T> {
        val request = createGenerateTokenParam(clientId, clientSecret)
        return channel.futureStubResult(transform) {
            generateToken(request)
        }
    }

    /*** login or register ***/
    private fun createRegisterParam(
        clientId: String,
        tinodeId: String,
        fullName: String
    ): Chatservice.RegisterRequest = Chatservice.RegisterRequest.newBuilder()
        .setUserId(clientId)
        .setTinodeId(tinodeId)
        .setFullName(fullName)
        .build()

    suspend fun <T : Any> registerFuture(
        clientId: String,
        tinodeId: String,
        fullName: String,
        transform: Chatservice.RegisterResponse.() -> T
    ): Result<T> {
        val request = createRegisterParam(clientId, tinodeId, fullName)
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

    /*** save device token ***/

    suspend fun <T : Any> saveDeviceTokenFuture(
        clientId: String,
        deviceToken: String,
        recipientId: String,
        platform: Platform,
        notifPipeline: NotifPipeline,
        transform: Chatservice.SaveDeviceTokenResponse.() -> T
    ): Result<T> {
        val request = Chatservice.SaveDeviceTokenRequest.newBuilder()
            .setClientId(clientId)
            .setToken(deviceToken)
            .setRecipientId(recipientId)
            .setPlatform(platform.value)
            .setNotifPipeline(notifPipeline.value)
            .build()
        return channel.futureStubResult(transform) {
            saveDeviceToken(request)
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