package com.example.background_remove.billing

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.utils.PrefsManagerRemoteConfig

import java.io.IOException
import java.lang.ref.WeakReference

class BillingsHelper private constructor(activity: AppCompatActivity) : BillingClientStateListener,
    PurchasesUpdatedListener {
    private val TAG = "BillingsHelper_log"
    private val TAG2 = "BillingsHelper_log23"

    companion object {
        private var billingsHelper: BillingsHelper? = null
        var billingClient: BillingClient? = null

        @Synchronized
        fun getInstance(activity: AppCompatActivity): BillingsHelper {
            if (billingsHelper == null || billingsHelper?.activityContext == null) {
                billingsHelper = BillingsHelper(activity)
            }
            return billingsHelper as BillingsHelper
        }
    }

    private val activityRef: WeakReference<AppCompatActivity> = WeakReference(activity)
    private val activityContext: Context?
        get() = activityRef.get()

    private var onUpdatePremiumListener: OnUpdatePremiumListener? = null

    var lifeTimeSkuDetails: ProductDetails? = null
    private var currentPurchase: Purchase? = null
     var selectedProductType: String = BillingClient.ProductType.INAPP
     var selectedProductId: String = Ads.lifetimekey
    private var reconnectMilliseconds: Long = 1000
    private var recallStatus = false

    fun setListener(premiumListener: OnUpdatePremiumListener?) {
        onUpdatePremiumListener = premiumListener
    }

    fun initializeBillingClient(context: Context) {
        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()
        }
    }

    fun startConnection(status: Boolean) {
        recallStatus = status
        billingClient?.startConnection(this)
    }

    fun connectToBillingService(): BillingClient? {
        if (!billingClient?.isReady!!)
            billingClient?.startConnection(this)
        else {
            verifyPurchase()
        }
        return billingClient
    }
    private fun verifyPurchase() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(selectedProductType).build(),
            inAppResponseListener
        )
    }
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            queryProductDetails()
        }
    }

    private val inAppResponseListener = PurchasesResponseListener { billingResult, purchasesList ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchasesList?.let {
                if (it.isNotEmpty()) {
                    for (purchase in it) {
                        Log.d(TAG2, "purchased")




                                currentPurchase = purchase
                                billingsHelper?.selectedProductId = Ads.lifetimekey
                                billingsHelper?.selectedProductType = BillingClient.ProductType.INAPP
                                PrefsManagerRemoteConfig.with(activityContext)
                                    .save(Ads.KEY_IS_PURCHASED, true)


                    }
                } else {
                    Log.d(TAG2, "no purchases")
                    // Reset flags if no purchases found
                    PrefsManagerRemoteConfig.with(activityContext)
                        .save(Ads.KEY_IS_PURCHASED, false)
                }
            }
        } else {
            Log.e(TAG2, "Error querying purchases: ${billingResult.debugMessage}")
        }
    }

    fun queryProductDetails() {

        val productList = mutableListOf<QueryProductDetailsParams.Product>()
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(selectedProductId)
            .setProductType(selectedProductType) // Use the selected product type
            .build()
        productList.add(product)

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(
            queryProductDetailsParams,
            productDetailsResponseListener
        )
        verifyPurchase()
    }

    override fun onBillingServiceDisconnected() {
        retryBillingServiceConnection()
    }

    private fun retryBillingServiceConnection() {
        Handler(Looper.myLooper()!!).postDelayed(
            { billingClient?.startConnection(this) },
            reconnectMilliseconds
        )
        reconnectMilliseconds *= 2
    }

    private val productDetailsResponseListener =
        ProductDetailsResponseListener { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                Log.d(TAG, "Product details response: ${selectedProductId}")
                productDetailsList.forEach { productDetails ->
                    Log.d(TAG, "Product details response1: ${productDetails}")
                    Log.d(TAG, "Product details response2: ${productDetails.name}")
                    Log.d(TAG, "Product details response3: ${productDetails.title}")
                    if (productDetails.productId==selectedProductId) {


                                Log.d(TAG, "Product details response3: lifetime")
                                lifeTimeSkuDetails=productDetails
                                val price = productDetails.oneTimePurchaseOfferDetails?.formattedPrice
                        Log.d("abcd12312",price.toString())
                                price?.let {
                                    PrefsManagerRemoteConfig.with(activityContext)
                                        .save(Ads.lifetimekey, it)
                                }


                }else{
                        Log.d(TAG, "id not match")
                }}

                onUpdatePremiumListener?.setPrice()
            } else {
                Log.d(TAG, "Failed to fetch product details: ${billingResult.responseCode}")
            }
        }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (verifyValidSignature(purchase.originalJson, purchase.signature)) {
                    setPremium(purchase)
                }
            }
        } else {
            errorHandle(billingResult.responseCode)
        }
    }

    private fun setPremium(purchase: Purchase) {
        if (purchase.products.contains(selectedProductId)) {
            PrefsManagerRemoteConfig.with(activityContext)
                .save(Ads.KEY_IS_PURCHASED, true)
        }
        acknowledgePurchase(purchase)
        onUpdatePremiumListener?.updateUI()
    }

    private fun verifyValidSignature(signedData: String?, signature: String?): Boolean {
        return try {
            Security.verifyPurchase(signedData.orEmpty(), signature.orEmpty())
        } catch (e: IOException) {
            false
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener)
        }
    }

    private val acknowledgePurchaseResponseListener =
        AcknowledgePurchaseResponseListener { Log.d(TAG, "Purchase acknowledged") }

    private fun errorHandle(responseCode: Int) {
        val errorMsg = when (responseCode) {
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Check Your Network Connection"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "You already own this item."
            else -> "An error occurred."
        }

        activityContext?.let {
            Toast.makeText(it, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }
}
