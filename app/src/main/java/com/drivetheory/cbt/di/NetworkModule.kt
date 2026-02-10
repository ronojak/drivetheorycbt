package com.drivetheory.cbt.di

import com.drivetheory.cbt.BuildConfig
import com.drivetheory.cbt.core.network.ApiClient
import com.drivetheory.cbt.data.billing.api.BillingApi
import com.drivetheory.cbt.data.mpesa.api.MpesaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun provideRetrofit(): Retrofit = ApiClient.retrofit(BuildConfig.BACKEND_BASE_URL)
    @Provides @Singleton fun provideBillingApi(retrofit: Retrofit): BillingApi = retrofit.create(BillingApi::class.java)
    @Provides @Singleton fun provideMpesaApi(retrofit: Retrofit): MpesaApi = retrofit.create(MpesaApi::class.java)
}
