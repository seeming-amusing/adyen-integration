package com.deliveryhero.poc.android.adyen.activity

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ScrollView
import com.deliveryhero.poc.android.adyen.R
import com.deliveryhero.poc.android.adyen.integration.CreditCard
import com.deliveryhero.poc.android.adyen.integration.MakePayment
import com.deliveryhero.poc.android.adyen.integration.MakePaymentWithAdyen
import com.deliveryhero.poc.android.adyen.integration.PaymentOption
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.cc_input.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

  private val disposable: CompositeDisposable = CompositeDisposable()
  private val makePayment: MakePayment by lazy { MakePaymentWithAdyen(this, logAction = this::log) }

  private var requiredInput: MakePayment.State.InputRequired? = null

  override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    btn_pay_with_card.setOnClickListener { triggerPayment(PaymentOption.CreditCard) }
    btn_pay_with_token.setOnClickListener { triggerPayment(PaymentOption.SavedCard("visa", name = "1111")) }
    btn_submit_details.setOnClickListener { sendInput() }
  }

  ///////////////////////////////
  // Adyen-related integrations
  ///////////////////////////////
  private fun triggerPayment(paymentOption: PaymentOption) {
    disposable += makePayment(paymentOption)
        .subscribe({ handleState(it) },
                   { log("Unexpected error: ${it.message}", it) })
  }

  private fun handleState(state: MakePayment.State) {
    when (state) {
      is MakePayment.State.InputRequired -> handleInput(state)
      else -> log("Payment result: ${state.javaClass.simpleName}")
    }
  }

  private fun handleInput(state: MakePayment.State.InputRequired) {
    requiredInput = state
    BottomSheetBehavior.from(credit_card_input).state = BottomSheetBehavior.STATE_EXPANDED
    when (state) {
      is MakePayment.State.InputRequired.Card -> show(number, expiration, holder_name)
      is MakePayment.State.InputRequired.SavedCard -> hide(number, expiration, holder_name)
    }
  }

  private fun sendInput() {
    BottomSheetBehavior.from(credit_card_input).state = BottomSheetBehavior.STATE_COLLAPSED
    requiredInput?.let {
      when (it) {
        is MakePayment.State.InputRequired.Card -> it.callback(creditCardFromInput())
        is MakePayment.State.InputRequired.SavedCard -> it.callback(creditCardFromInput())
      }
    }
  }

  // View-related stuff; not so important
  private fun creditCardFromInput() = CreditCard(
      number = number.text.toString(),
      holder = holder_name.text.toString(),
      cvc = cvc.text.toString(),
      expirationMonth = expiration.text.substring(0..1),
      expirationYear = "20" + expiration.text.substring(2..3),
      shouldSave = true)

  private fun show(vararg views: View) = views.forEach { it.visibility = View.VISIBLE }
  private fun hide(vararg views: View) = views.forEach { it.visibility = View.GONE }

  private fun log(message: String, throwable: Throwable? = null) {
    if (throwable == null) {
      log_output.append("$message\n")
    } else {
      log_output.append("Error: $message (${throwable.message})\n")
    }
    scroll_log.post { scroll_log.fullScroll(ScrollView.FOCUS_DOWN) }
  }

  private operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
  }

  override fun onDestroy() = super.onDestroy().also {
    disposable.dispose()
  }
}