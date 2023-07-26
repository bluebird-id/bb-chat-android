package id.bluebird.chat.io

import com.google.common.util.concurrent.ListenableFuture
import grpc.ChatServiceGrpc
import grpc.ChatServiceGrpc.ChatServiceFutureStub
import grpc.Chatservice
import id.bluebird.chat.io.network.Result
import id.bluebird.chat.io.network.awaitResult
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

class ChatServiceApi {

    val channel: ManagedChannel = ManagedChannelBuilder.forAddress("34.124.216.166", 6969)
        .usePlaintext()
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

    suspend fun <T: Any> getParticipantByOrderIdFuture(
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