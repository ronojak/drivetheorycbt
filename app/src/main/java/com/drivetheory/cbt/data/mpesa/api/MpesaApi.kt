package com.drivetheory.cbt.data.mpesa.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class StkPushRequest(val uid: String, val phoneNumber: String, val planId: String, val amount: Int? = null)
data class StkPushResponse(
    val serverPaymentId: String,
    val merchantRequestId: String?,
    val checkoutRequestId: String?,
    val status: String
)

data class PaymentStatusResponse(
    val paymentId: String,
    val status: String,
    val planId: String?,
    val amount: Int?,
    val checkoutRequestId: String?,
    val merchantRequestId: String?,
    val resultCode: Int?,
    val resultDesc: String?,
    val mpesaReceipt: String?,
    val updatedAt: Long?
)

data class EntitlementResponse(
    val status: String,
    val plan: String?,
    val expiresAt: Long?
)

interface MpesaApi {
    @POST("api/payments/mpesa/stk-push")
    suspend fun stkPush(@Body req: StkPushRequest): StkPushResponse

    @GET("api/payments/status")
    suspend fun status(@Query("paymentId") paymentId: String): PaymentStatusResponse

    @GET("api/me/entitlements")
    suspend fun entitlements(@Query("uid") uid: String): EntitlementResponse

    // Manual receipt fallback
    data class ManualReceiptRequest(
        val uid: String,
        val receipt: String,
        val planId: String,
        val phoneNumber: String? = null,
        val amount: Int? = null
    )
    data class ManualReceiptResponse(
        val paymentId: String,
        val status: String,
        val matched: Boolean
    )

    @POST("api/payments/mpesa/manual-receipt")
    suspend fun manualReceipt(@Body req: ManualReceiptRequest): ManualReceiptResponse
}
