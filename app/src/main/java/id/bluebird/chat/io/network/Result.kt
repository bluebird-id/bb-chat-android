package id.bluebird.chat.io.network

sealed class Result<out T : Any> {
    /**
     * Successful result of request without errors
     */
    class Ok<out T : Any>(val data: T) : Result<T>() {
        override fun toString() = "Result.Ok{data=$data}"
    }

    /**
     * Network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response
     */
    class Exception(val exception: Throwable?) : Result<Nothing>() {
        val errorMessage: String
            get() {
                return getErrorMessage(exception)
            }

        override fun toString() = "Result.Exception{message=$errorMessage,$exception ${exception?.cause} ${exception?.message}}"

        /**
         * ERROR MESSAGE
         * for handle error from backend
         */
        var error1 = listOf("GRPC_STATUS_DEADLINE_EXCEEDED", "GRPC_STATUS_UNAVAILABLE")
        var error2 = listOf("GRPC_STATUS_INTERNAL", "GRPC_STATUS_UNKNOWN", "GRPC_STATUS_UNIMPLEMENTED", "GRPC_STATUS_RESOURCE_EXHAUSTED", "GRPC_STATUS_UNAUTHENTICATED")

        fun getErrorMessage(e: Throwable?): String {
            val stringMessage = e?.message?.split(":")?.get(0)
            return when {
                error1.find {
                    it.contains(stringMessage.orEmpty())
                }?.any() == true -> "Terjadi kendala, silahkan cek kembali koneksi anda."
                error2.find {
                    it.contains(stringMessage.orEmpty())
                }?.any() == true -> "Server sedang mengalami gangguan, silahkan coba kembali beberapa saat lagi."
                else -> "Terjadi kendala, silahkan coba kemabli"
            }
        }

    }
}