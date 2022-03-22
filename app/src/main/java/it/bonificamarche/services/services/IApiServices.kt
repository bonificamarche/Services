package it.bonificamarche.services.services

import com.google.gson.JsonObject
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface IApiServices {

    /**
     * Upload photos.
     */
    @FormUrlEncoded
    @POST("maps/foto.php")
    fun uploadPhoto(
        @Field("kk") kk: String,
        @Field("ks") ks: String,
        @Field("u") u: String,
        @Field("t") typePhoto: String,
        @Field("n") name: String,
        @Field("f") photo: String
    ): Flowable<JsonObject>

    /**
     * Resize images after uploading photo.
     */
    @GET("i/r.php")
    fun resizeImages(
        @Query("f") folder: String,
        @Query("i") nameFile: String
    ): Flowable<JsonObject>


    companion object {
        /**
         * Create retrofit builder.
         *
         * @param baseUrl base url to make a request.
         */
        fun create(baseUrl: String): IApiServices {

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build()

            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .build()

            return retrofit.create(IApiServices::class.java)
        }
    }
}