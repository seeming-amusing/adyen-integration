package com.deliveryhero.poc.android.adyen.integration

import com.adyen.core.PaymentRequest
import com.adyen.core.interfaces.PaymentDataCallback
import com.adyen.core.interfaces.PaymentRequestListener
import com.adyen.core.models.Payment
import com.adyen.core.models.PaymentRequestResult
import com.deliveryhero.poc.android.adyen.plusAssign
import com.deliveryhero.poc.android.adyen.schedule
import com.deliveryhero.poc.android.adyen.service
import com.deliveryhero.poc.android.adyen.service.SampleBody
import com.deliveryhero.poc.android.adyen.service.ValidationRequest
import com.deliveryhero.poc.android.adyen.service.ValidationResult
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.CompositeDisposable

class PaymentRequestWrapper(
    private val emitter: ObservableEmitter<MakePayment.State>,
    private val logAction: (message: String, throwable: Throwable?) -> Unit)
  : PaymentRequestListener {

  private val disposable = CompositeDisposable()

  init {
    emitter.setCancellable { disposable.clear() }
  }

  override fun onPaymentDataRequested(paymentRequest: PaymentRequest,
                                      sdkToken: String,
                                      callback: PaymentDataCallback) {
    log("Requesting setup payload")
    disposable += service.setup(SampleBody(sdkToken)).schedule()
        .subscribe({ callback.completionWithPaymentData(it.bytes()) },
                   { paymentRequest.cancelAndLog("Unable to setup payment", it) })
  }

  override fun onPaymentResult(paymentRequest: PaymentRequest,
                               result: PaymentRequestResult) {
    when {
      result.isProcessed -> process(result)
      else -> emitFailure(result.error)
    }
  }

  private fun process(result: PaymentRequestResult) {
    result.payment?.let { payment ->
      log("Payment status: ${payment.paymentStatus}")
      disposable += service.verify(ValidationRequest(payment.payload)).schedule()
          .subscribe({ it.check(payment).emitResult() },
                     { log("Unable to verify payment", it) })
    } ?: log("No payment object found")
  }

  private fun ValidationResult.check(payment: Payment) = (authResponse == payment.paymentStatus.toString())
      .also { isValid ->
        val result = when {
          isValid -> "$authResponse and verified"
          else -> "Unable to verify"
        }
        log("Payment validation result: $result")
      }

  private fun Boolean.emitResult() {
    emitter.onNext(when {
                     this -> MakePayment.State.Success
                     else -> MakePayment.State.Failure()
                   })
  }

  private fun emitFailure(error: Throwable?) {
    log("Payment failed with exception", error)
    emitter.onNext(MakePayment.State.Failure(error))
  }

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