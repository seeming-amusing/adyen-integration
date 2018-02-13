package com.deliveryhero.poc.android.adyen.integration

import io.reactivex.Observable

interface MakePayment {

  operator fun invoke(paymentOption: PaymentOption): Observable<State>

  sealed class State {

    object Success : State()

    data class Failure(val throwable: Throwable? = null) : State()

    sealed class InputRequired : State() {

      data class Card(val callback: (CreditCard) -> Unit) : InputRequired()

      data class SavedCard(val callback: (CreditCard) -> Unit) : InputRequired()
    }
  }
}