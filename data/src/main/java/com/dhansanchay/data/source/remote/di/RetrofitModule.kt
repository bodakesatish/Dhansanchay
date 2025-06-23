package com.dhansanchay.data.source.remote.di

import com.dhansanchay.data.source.remote.MutualFundApiService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

// Define custom qualifiers for better type safety than @Named
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MfApiRetrofit

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule { // Modules with only @Provides can be objects

    private const val BASE_URL_MF_API = "https://api.mfapi.in/" // Centralize constants
    private const val DEFAULT_TIMEOUT_SECONDS = 60L // Use Long for time units
    private const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 15L // Adjusted for example
    private const val DEFAULT_READ_TIMEOUT_SECONDS = 20L    // Adjusted for example
    private const val DEFAULT_WRITE_TIMEOUT_SECONDS = 20L   // Adjusted for example



    @Provides
    @Singleton
    fun provideGson(): Gson = Gson() // Provide Gson instance, can be customized if needed

    @AppOkHttpClient // Use your custom qualifier here
    @Provides
    @Singleton
    internal fun provideOkHTTPClient(
        // @ApplicationContext context: Context // Inject context if needed for BuildConfig from library module
    ): OkHttpClient {
        //default timeout is 60
        val client = OkHttpClient.Builder()
        client.readTimeout(DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        client.connectTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        client.writeTimeout(DEFAULT_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
//        client.callTimeout(10, TimeUnit.SECONDS)

        // Only add logging interceptor in debug builds
        // Ensure you have `buildFeatures { buildConfig = true }` in your module's build.gradle
        // and a proper `BuildConfig.java` generated.
        // Replace `com.dhansanchay.BuildConfig.DEBUG` with your actual BuildConfig path if different.

//        if (DEBUG) {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        client.interceptors().add(logging)
//        }
        // Add other interceptors here if needed (e.g., for authentication)
        // clientBuilder.addInterceptor(AuthInterceptor(...))
        return client.build()
    }

    @MfApiRetrofit // Use custom qualifier instead of  @Named("retrofitBuilderDataService")
    @Provides
    @Singleton
    internal fun provideRetrofitBuilderForDataService(
        @AppOkHttpClient okHttpClient: OkHttpClient,
        gson: Gson // Inject Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_MF_API)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))// Pass the provided Gson instance
            .build()
    }

    @Provides
    @Singleton
    internal fun provideDataService(@MfApiRetrofit retrofit: Retrofit): MutualFundApiService {
        return retrofit.create(MutualFundApiService::class.java)
    }

}