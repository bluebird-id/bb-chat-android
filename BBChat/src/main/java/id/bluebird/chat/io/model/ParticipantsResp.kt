package id.bluebird.chat.io.model

import grpc.Chatservice

class ParticipantsResp {

    var participants = ArrayList<String>()

    fun setItem(response: Chatservice.GetParticipantsResponse) {
        participants = response.participantsIdListList as ArrayList<String>
    }
}