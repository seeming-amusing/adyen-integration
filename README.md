Adyen Integration - Sample
===

This provides a sample on how to integrate the Adyen SDK into a code base. The integration consists of three separate
components:

1. [FakeMcDuckStuff](app/src/main/java/com/deliveryhero/poc/android/adyen/service/FakeMcDuckStuff.kt): Retrofit 
service for the server integration
2. [MakePayment](app/src/main/java/com/deliveryhero/poc/android/adyen/integration/MakePayment.kt): Integration of Adyen 
SDK logic with the server integration
3. [MainActivity](app/src/main/java/com/deliveryhero/poc/android/adyen/activity/MainActivity.kt): Activity that 
contains view logic for integration