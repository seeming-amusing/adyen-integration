package com.deliveryhero.poc.android.adyen.integration

import android.content.Context
import android.util.Log
import com.adyen.core.PaymentRequest
import io.reactivex.Observable

class MakePaymentWithAdyen(private val context: Context,
                           private val logAction: (message: String, throwable: Throwable?) -> Unit) : MakePayment {

  private var paymentRequest: PaymentRequest? = null

  override fun invoke(paymentOption: PaymentOption): Observable<MakePayment.State> = Observable.create { emitter ->
    paymentRequest?.cancelAndLog("----------------------------")
    paymentRequest = PaymentRequest(context,
                                    PaymentRequestWrapper(emitter, this::log),
                                    PaymentDetailsWrapper(paymentOption, emitter, this::log))
        .also { it.start() }
  }

  //////////////////////////////////
  // Logging stuff; can be removed
  //////////////////////////////////
  // TODO: Remove unneeded logging calls
  private fun PaymentRequest.cancelAndLog(message: String, throwable: Throwable? = null) {
    cancel()
    log(message, throwable)
  }

  private fun log(message: String, throwable: Throwable? = null) {
    if (throwable == null) {
      Log.d(TAG, message)
    } else {
      Log.e(TAG, message, throwable)
    }
    logAction(message, throwable)
  }

  companion object {
    private val TAG = MakePaymentWithAdyen::class.java.simpleName
  }
}