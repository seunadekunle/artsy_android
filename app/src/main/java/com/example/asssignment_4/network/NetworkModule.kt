package com.example.asssignment_4.network

import android.content.Context
import com.example.asssignment_4.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.JavaNetCookieJar
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.CookieJar
import okhttp3.CookieManager
import okhttp3.CookiePolicy
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://your-backend-url.com" // TODO: Replace with actual backend URL

    fun provideCookieJar(context: Context): CookieJar {
        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        return JavaNetCookieJar(cookieManager)
    }

    fun provideOkHttpClient(cookieJar: CookieJar): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
