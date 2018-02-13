package com.deliveryhero.poc.android.adyen.integration

sealed class PaymentOption(val type: String) {
  object CreditCard : PaymentOption("card")
  object PayPal : PaymentOption("paypal")
  data class SavedCard(private val cardType: String, val name: String) : PaymentOption(cardType)
}

data class CreditCard(
    val number: String = "",
    val holder: String = "",
    val expirationMonth: String = "",
    val expirationYear: String = "",
    val cvc: String = "",
    val shouldSave: Boolean = false)
