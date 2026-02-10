package com.drivetheory.cbt.data.billing.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class InitRequest(val uid: String, val plan: String)
data class InitResponse(val checkoutUrl: String?, val reference: String?)
data class VerifyResponse(val success: Boolean)

interface BillingApi {
    @POST("paystack/initialize")
    suspend fun initialize(@Body req: InitRequest): InitResponse

    @GET("paystack/verify")
    suspend fun verify(@Query("reference") reference: String): VerifyResponse
}

