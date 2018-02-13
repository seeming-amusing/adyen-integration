package com.deliveryhero.poc.android.adyen

import com.deliveryhero.poc.android.adyen.service.FakeMcDuckStuff
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

val service: FakeMcDuckStuff by lazy {
  retrofit.create(FakeMcDuckStuff::class.java)
}

private val retrofit by lazy {
  Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .baseUrl("https://checkoutshopper-test.adyen.com/checkoutshopper/demoserver/")
      .client(okhttp)
      .build()
}

private val okhttp by lazy {
  OkHttpClient.Builder()
      .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
      .addNetworkInterceptor { chain ->
        chain.proceed(chain.request()
                          .newBuilder()
                          .header("Content-Type", "application/json; charset=UTF-8")
                          .header("x-demo-server-api-key", "0101428667F12E8CC8681D44C349BF9F7439A9FDD56C8E4104854F4A7C7EC36AA4D3724E8DD33B34EE5E3232721AD1FBC08380933F83BF14CE671A0DDC146DC99F2041F1E910C15D5B0DBEE47CDCB5588C48224C6007")
                          .build())
      }
      .build()
}
