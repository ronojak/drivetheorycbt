package com.drivetheory.cbt.di

import android.content.Context
import com.drivetheory.cbt.data.auth.AuthRepositoryImpl
import com.drivetheory.cbt.data.billing.BillingRepositoryImpl
import com.drivetheory.cbt.data.subscription.SubscriptionRepositoryImpl
import com.drivetheory.cbt.data.mpesa.MpesaRepositoryImpl
import com.drivetheory.cbt.data.user.UserRepositoryImpl
import com.drivetheory.cbt.domain.repository.AuthRepository
import com.drivetheory.cbt.domain.repository.BillingRepository
import com.drivetheory.cbt.domain.repository.SubscriptionRepository
import com.drivetheory.cbt.domain.repository.MpesaRepository
import com.drivetheory.cbt.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindSubRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository
    @Binds @Singleton abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository
    @Binds @Singleton abstract fun bindMpesaRepository(impl: MpesaRepositoryImpl): MpesaRepository
}
