package id.bluebird.chat.io

import grpc.Chatservice.GetParticipantsResponse
import id.bluebird.chat.io.model.ParticipantsResp
import id.bluebird.chat.io.model.RegisterResp
import id.bluebird.chat.io.network.Result

interface ChatServiceRepository {

    suspend fun register(clientId: String, tinodeId: String, fullName: String): Result<RegisterResp>

    suspend fun getParticipants(orderId: String): Result<ParticipantsResp>
}

class ChatServiceRepositoryImpl(private val chatServiceApi: ChatServiceApi): ChatServiceRepository {

    override suspend fun register(clientId: String, tinodeId: String, fullName: String): Result<RegisterResp> {
        return chatServiceApi.registerFuture(clientId, tinodeId, fullName) {
            val response = RegisterResp()
            response.setItem(this)
            response
        }
    }

    override suspend fun getParticipants(orderId: String): Result<ParticipantsResp> {
        return chatServiceApi.getParticipantByOrderIdFuture(orderId) {
            val response = ParticipantsResp()
            response.setItem(this)
            response
        }
    }
}