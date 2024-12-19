package com.lebatinh.messenger

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.lebatinh.messenger.account.otp.OTPRepository
import com.lebatinh.messenger.account.otp.OTPUseCase
import com.lebatinh.messenger.helper.GmailHelper
import com.lebatinh.messenger.helper.PermissionHelper
import com.lebatinh.messenger.mess.fragment.conversation.ConversationRepository
import com.lebatinh.messenger.mess.fragment.conversation.ConversationUseCase
import com.lebatinh.messenger.notification.NotiHelper
import com.lebatinh.messenger.notification.NotiManager
import com.lebatinh.messenger.user.UserRepository
import com.lebatinh.messenger.user.UserUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideRealtimeDB(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Provides
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth
    ): UserRepository {
        return UserRepository(firestore, firebaseAuth)
    }

    @Provides
    fun provideGmailHelper(): GmailHelper {
        return GmailHelper()
    }

    @Provides
    fun provideOTPRepository(
        gmailHelper: GmailHelper,
        firestore: FirebaseFirestore,
    ): OTPRepository {
        return OTPRepository(gmailHelper, firestore)
    }

    @Provides
    fun provideConversationRepository(
        firestore: FirebaseFirestore,
        realtimeDB: FirebaseDatabase
    ): ConversationRepository {
        return ConversationRepository(firestore, realtimeDB)
    }

    @Provides
    fun provideUserUseCase(
        repository: UserRepository
    ): UserUseCase {
        return UserUseCase(repository)
    }

    @Provides
    fun provideOTPUseCase(
        repository: OTPRepository
    ): OTPUseCase {
        return OTPUseCase(repository)
    }

    @Provides
    fun provideConversationUseCase(
        repository: ConversationRepository
    ): ConversationUseCase {
        return ConversationUseCase(repository)
    }

    @Provides
    fun providePermissionHelper(@ApplicationContext context: Context): PermissionHelper {
        return PermissionHelper(context)
    }

    @Provides
    fun provideNotificationHelper(@ApplicationContext context: Context): NotiHelper {
        return NotiHelper(context)
    }

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotiManager {
        return NotiManager(context)
    }
}