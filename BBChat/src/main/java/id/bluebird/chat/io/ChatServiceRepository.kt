package id.bluebird.chat.io

import id.bluebird.chat.io.model.DeviceTokenResp
import id.bluebird.chat.io.model.ParticipantsResp
import id.bluebird.chat.io.model.RegisterResp
import id.bluebird.chat.io.network.Result

interface ChatServiceRepository {

    suspend fun register(clientId: String, tinodeId: String, fullName: String): Result<RegisterResp>

    suspend fun getParticipants(orderId: String): Result<ParticipantsResp>

    suspend fun saveDeviceToken(appId: Long, participantId: String, deviceToken: String): Result<DeviceTokenResp>
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

    override suspend fun saveDeviceToken(appId: Long, participantId: String, deviceToken: String): Result<DeviceTokenResp> {
        return chatServiceApi.saveDeviceTokenFuture(appId, deviceToken, participantId) {
            val response = DeviceTokenResp()
            response.setItem(this)
            response
        }
    }
}