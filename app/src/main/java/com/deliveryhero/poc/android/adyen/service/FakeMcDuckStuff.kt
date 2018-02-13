package com.deliveryhero.poc.android.adyen.service

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface FakeMcDuckStuff {

  @POST("setup")
  fun setup(@Body body: SampleBody): Single<ResponseBody>

  @POST("verify")
  fun verify(@Body request: ValidationRequest): Single<ValidationResult>
}