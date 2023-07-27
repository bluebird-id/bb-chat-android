package id.bluebird.chat.io

import id.bluebird.chat.io.model.RegisterResp
import id.bluebird.chat.io.network.Result

interface ChatServiceRepository {

    suspend fun register(clientId: String, tinodeId: String): Result<RegisterResp>

    //suspend fun getParticipants(): Result<GetParticipantsResponse>
}

class ChatServiceRepositoryImpl(private val chatServiceApi: ChatServiceApi): ChatServiceRepository {

    override suspend fun register(clientId: String, tinodeId: String): Result<RegisterResp> {
        return chatServiceApi.registerFuture(clientId, tinodeId) {
            val response = RegisterResp()
            response.setItem(this)
            response
        }
    }

//    override suspend fun getParticipants(): Result<GetParticipantsResponse> {
//        return chatServiceApi.getParticipantsFuture() {
//            //GetParticipantsResponse(code, status, message, data)
//        }
//    }
}