package com.deliveryhero.poc.android.adyen.service

import com.google.gson.annotations.Expose

data class SampleBody(
    @Expose val token: String,
    @Expose val merchantAccount: String = "TestMerchant",
    @Expose val shopperLocale: String = "NL",
    @Expose val returnUrl: String = "adyen-sample://",
    @Expose val countryCode: String = "NL",
    @Expose val channel: String = "Android",
    @Expose val reference: String = "Android Checkout SDK Payment: ${System.currentTimeMillis()}",
    @Expose val shopperReference: String = "example-customer@exampleprovider",
    @Expose val amount: Amount = Amount())

data class Amount(
    @Expose val value: Double = 0.10,
    @Expose val currency: String = "EUR")

data class ValidationRequest(
    @Expose val payload: String)

data class ValidationResult(
    @Expose val authResponse: String)
