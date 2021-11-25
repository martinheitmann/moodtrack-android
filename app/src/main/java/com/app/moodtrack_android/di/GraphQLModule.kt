package com.app.moodtrack_android.di

import com.apollographql.apollo.ApolloClient
import com.app.moodtrack_android.BuildConfig
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object GraphQLModule {

    @Provides
    @Singleton
    fun provideApolloClient() : ApolloClient {
        return ApolloClient.builder()
            //.serverUrl("http://10.0.2.2:4000/graphql")
            .serverUrl(BuildConfig.SERVER_URL)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient() : OkHttpClient {
        return OkHttpClient()
    }

    @Provides
    @Singleton
    fun provideGson() : Gson {
        return Gson()
    }

}