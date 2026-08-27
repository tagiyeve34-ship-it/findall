package com.ailenezareti.panelnext.api

import android.content.Context
import com.ailenezareti.panelnext.BuildConfig
import com.ailenezareti.panelnext.Prefs
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile private var api: ApiService? = null
    fun get(context: Context): ApiService = api ?: synchronized(this) {
        api ?: Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).addInterceptor { chain ->
                val t = Prefs.token(context)
                val b = chain.request().newBuilder()
                if (t.isNotBlank()) b.header("Authorization", "Bearer $t")
                chain.proceed(b.build())
            }.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ApiService::class.java).also { api = it }
    }
}
