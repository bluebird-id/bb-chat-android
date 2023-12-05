package id.bluebird.chat.io

import id.bluebird.chat.NotifPipeline
import id.bluebird.chat.Platform
import id.bluebird.chat.io.model.DeviceTokenResp
import id.bluebird.chat.io.model.GenerateTokenResp
import id.bluebird.chat.io.model.ParticipantsResp
import id.bluebird.chat.io.model.RegisterResp
import id.bluebird.chat.io.network.Result

interface ChatServiceRepository {

    suspend fun generateNewToken(clientId: String, clientSecret: String): Result<GenerateTokenResp>

    suspend fun register(clientId: String, tinodeId: String, fullName: String): Result<RegisterResp>

    suspend fun getParticipants(orderId: String): Result<ParticipantsResp>

    suspend fun saveDeviceToken(
        clientId: String,
        participantId: String,
        deviceToken: String,
        platform: Platform,
        notifPipeline: NotifPipeline
    ): Result<DeviceTokenResp>
}

class ChatServiceRepositoryImpl(private val chatServiceApi: ChatServiceApi) :
    ChatServiceRepository {

    override suspend fun generateNewToken(
        clientId: String,
        clientSecret: String
    ): Result<GenerateTokenResp> {
        return chatServiceApi.generateNewToken(clientId, clientSecret) {
            val response = GenerateTokenResp()
            response.setItem(this)
            response
        }
    }

    override suspend fun register(
        clienUserId: String,
        tinodeId: String,
        fullName: String
    ): Result<RegisterResp> {
        return chatServiceApi.registerFuture(clienUserId, tinodeId, fullName) {
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

    override suspend fun saveDeviceToken(
        clientId: String,
        participantId: String,
        deviceToken: String,
        platform: Platform,
        notifPipeline: NotifPipeline
    ): Result<DeviceTokenResp> {
        return chatServiceApi.saveDeviceTokenFuture(clientId, deviceToken, participantId, platform, notifPipeline) {
            val response = DeviceTokenResp()
            response.setItem(this)
            response
        }
    }
}