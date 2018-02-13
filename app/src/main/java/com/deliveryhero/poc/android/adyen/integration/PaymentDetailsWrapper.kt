package com.deliveryhero.poc.android.adyen.integration

import adyen.com.adyencse.pojo.Card
import com.adyen.core.PaymentRequest
import com.adyen.core.interfaces.PaymentDetailsCallback
import com.adyen.core.interfaces.PaymentMethodCallback
import com.adyen.core.interfaces.PaymentRequestDetailsListener
import com.adyen.core.interfaces.UriCallback
import com.adyen.core.models.PaymentMethod
import com.adyen.core.models.paymentdetails.CVCOnlyPaymentDetails
import com.adyen.core.models.paymentdetails.CreditCardPaymentDetails
import com.adyen.core.models.paymentdetails.InputDetail
import io.reactivex.ObservableEmitter
import java.util.*

class PaymentDetailsWrapper(
    private val paymentOption: PaymentOption,
    private val emitter: ObservableEmitter<MakePayment.State>,
    private val logAction: (message: String, throwable: Throwable?) -> Unit)
  : PaymentRequestDetailsListener {

  override fun onPaymentMethodSelectionRequired(paymentRequest: PaymentRequest,
                                                savedOptions: List<PaymentMethod>,
                                                otherOptions: List<PaymentMethod>,
                                                callback: PaymentMethodCallback) {
    log("Selecting payment option: ${paymentOption.javaClass.simpleName} (type: ${paymentOption.type})")
    when (paymentOption) {
      is PaymentOption.CreditCard ->
        otherOptions.completeOnCondition(callback,
                                         condition = { it.type == paymentOption.type },
                                         cancel = { paymentRequest.cancelAndLog("Cannot pay with credit card") })
      is PaymentOption.SavedCard ->
        savedOptions.completeOnCondition(callback,
                                         condition = { it matches paymentOption },
                                         cancel = { paymentRequest.cancelAndLog("Cannot find saved card") })
      else -> paymentRequest.cancelAndLog("Cannot handle payment option: $paymentOption")
    }
  }

  private inline fun List<PaymentMethod>.completeOnCondition(callback: PaymentMethodCallback,
                                                             condition: (PaymentMethod) -> Boolean,
                                                             cancel: () -> Unit) =
      find(condition)
          ?.let { callback.completionWithPaymentMethod(it) }
          ?: cancel()

  private infix fun PaymentMethod.matches(paymentOption: PaymentOption.SavedCard) =
      type == paymentOption.type && name.endsWith(paymentOption.name)

  override fun onRedirectRequired(paymentRequest: PaymentRequest,
                                  redirectUrl: String,
                                  uriCallback: UriCallback) { // TODO: Handle URL payment options
    paymentRequest.cancelAndLog("Requesting URL redirection; currently not handled")
  }

  override fun onPaymentDetailsRequired(paymentRequest: PaymentRequest,
                                        inputDetails: Collection<InputDetail>,
                                        callback: PaymentDetailsCallback) {
    paymentRequest.paymentMethod?.let {
      when (paymentOption) {
        is PaymentOption.CreditCard ->
          emitter.onNext(paymentRequest.toCardInput(with = callback, inputDetails = inputDetails))
        is PaymentOption.SavedCard ->
          emitter.onNext(savedCardInput(with = callback, inputDetails = inputDetails))
        else -> paymentRequest.cancelAndLog("Unexpected payment option: $paymentOption")
      }
    } ?: paymentRequest.cancelAndLog("Missing payment method")
  }

  private fun PaymentRequest.toCardInput(with: PaymentDetailsCallback, inputDetails: Collection<InputDetail>) =
      MakePayment.State.InputRequired.Card { card ->
        with.completionWithPaymentDetails(creditCardDetails(card, publicKey!!, inputDetails))
      }

  private fun creditCardDetails(creditCard: CreditCard, publicKey: String, inputDetails: Collection<InputDetail>) =
      CreditCardPaymentDetails(inputDetails).apply {
        fillCardToken(creditCard.asAdyenCard().serialize(publicKey))
        fillStoreDetails(creditCard.shouldSave)
      }.also { log("Paying with card: ${creditCard.number}") }

  private fun CreditCard.asAdyenCard() = Card.Builder().setNumber(number)
      .setHolderName(holder)
      .setCvc(cvc)
      .setExpiryMonth(expirationMonth)
      .setExpiryYear(expirationYear)
      .setGenerationTime(Date())
      .build()

  private fun savedCardInput(with: PaymentDetailsCallback, inputDetails: Collection<InputDetail>) =
      MakePayment.State.InputRequired.SavedCard { card ->
        with.completionWithPaymentDetails(cvcDetails(card, inputDetails))
      }

  private fun cvcDetails(creditCard: CreditCard, inputDetails: Collection<InputDetail>) =
      CVCOnlyPaymentDetails(inputDetails)
          .apply { fillCvc(creditCard.cvc) }
          .also { log("Verifying CVC: ${creditCard.cvc}") }

  //////////////////////////////////
  // Logging stuff; can be removed
  //////////////////////////////////
  // TODO: Remove unneeded logging functions
  private fun log(message: String, throwable: Throwable? = null) = logAction(message, throwable)

  private fun PaymentRequest.cancelAndLog(message: String, throwable: Throwable? = null) {
    cancel()
    log(message, throwable)
  }
}