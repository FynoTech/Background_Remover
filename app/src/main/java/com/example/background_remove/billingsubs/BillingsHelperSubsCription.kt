package com.example.background_remove.billingsubs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.SharedPreferenceUtils.save
import com.example.background_remove.billing.OnUpdatePremiumListener
import com.example.background_remove.billing.Security
import com.example.background_remove.utils.PrefsManagerRemoteConfig
import com.photo.editor.background.eraser.bgeraser.R


import java.io.IOException
import java.lang.ref.WeakReference

var onSetPrice: ((String) -> Unit)? = null

class BillingsHelperSubsCription private constructor(activity: AppCompatActivity) :
    BillingClientStateListener, PurchasesUpdatedListener {

    private val TAG = "BillingsHelperSubsCription"

    companion object {
        private var billingsHelper: BillingsHelperSubsCription? = null
        var billingClient: BillingClient? = null
        var price: String? = null
        var selectedProductId: String? = null
        val productIds = listOf( Ads.monthlykey,Ads.yearlykey,Ads.weeklykey,)

        @Synchronized
        fun getInstance(activity: AppCompatActivity): BillingsHelperSubsCription {
            if (billingsHelper == null || billingsHelper?.activityContext == null) {
                billingsHelper = BillingsHelperSubsCription(activity)
            }
            return billingsHelper as BillingsHelperSubsCription
        }
    }

    private val activityRef: WeakReference<AppCompatActivity> = WeakReference(activity)
    private val activityContext: Context?
        get() = activityRef.get()

    private var onUpdatePremiumListener: OnUpdatePremiumListener? = null
    var subscriptionSkuDetails: ProductDetails? = null
    private var currentPurchase: Purchase? = null
    private var reconnectMilliseconds: Long = 1000
    private var recallStatus = false

    fun setListener(premiumListener: OnUpdatePremiumListener?) {
        onUpdatePremiumListener = premiumListener
    }

    fun getCurrentPurchase(): Purchase? {
        return currentPurchase
    }

    fun initializeBillingClient(context: Context) {
        Log.d(TAG, "initializeBillingClient: $productIds")
        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()
        }
    }

    fun getBillingClient(): BillingClient? = billingClient

    fun startConnection(status: Boolean) {
        recallStatus = status
        billingClient?.startConnection(this)
    }

    fun connectToBillingService(): BillingClient? {
        if (billingClient?.isReady == false) {
            billingClient?.startConnection(this)
        } else {
            verifyPurchase()
        }
        return billingClient
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d("selectedpproduct", selectedProductId.toString())
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (selectedProductId ==null) {
                val subscriptionSkus = productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }

                val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(subscriptionSkus)
                    .build()

                billingClient?.queryProductDetailsAsync(queryProductDetailsParams, productDetailsResponseListener)
                verifyPurchase()
            } else {
                val subscriptionSkus = mutableListOf<QueryProductDetailsParams.Product>()
                val productSubscription = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(selectedProductId!!)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                subscriptionSkus.add(productSubscription)
                val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(subscriptionSkus)
                    .build()

                billingClient?.queryProductDetailsAsync(
                    queryProductDetailsParams,
                    productDetailsResponseListener
                )

                verifyPurchase()
            }
        } else {
            Log.d(TAG, "onBillingSetupFinished: code does not match")
        }
    }

    override fun onBillingServiceDisconnected() {
        retryBillingServiceConnection()
    }

    private fun retryBillingServiceConnection() {
        Handler(Looper.getMainLooper()).postDelayed({
            connectToBillingService()
        }, reconnectMilliseconds)
        reconnectMilliseconds *= 2
    }

    private fun verifyPurchase() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            subscriptionResponseListener
        )
    }

    private val subscriptionResponseListener = PurchasesResponseListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (!purchases.isNullOrEmpty()) {
                for (purchase in purchases) {
                    when {
                        purchase.products.contains(Ads.weeklykey) -> {
                            currentPurchase = purchase
                            PrefsManagerRemoteConfig.with(activityContext).apply {
                                save(Ads.KEY_IS_PURCHASEDSubs, true)
                                save(Ads.purchasedsubs, "weekly")
                            }
                            Log.d(TAG, "Weekly subscription active: ${purchase.products}")
                        }
                        purchase.products.contains(Ads.monthlykey) -> {
                            currentPurchase = purchase
                            PrefsManagerRemoteConfig.with(activityContext).apply {
                                save(Ads.KEY_IS_PURCHASEDSubs, true)
                                save(Ads.purchasedsubs, "monthly")
                            }
                            Log.d(TAG, "Monthly subscription active: ${purchase.products}")
                        }
                        purchase.products.contains(Ads.yearlykey) -> {
                            currentPurchase = purchase
                            PrefsManagerRemoteConfig.with(activityContext).apply {
                                save(Ads.KEY_IS_PURCHASEDSubs, true)
                                save(Ads.purchasedsubs, "yearly")
                            }
                            Log.d(TAG, "Yearly subscription active: ${purchase.products}")
                        }
                        else -> {
                            Log.d(TAG, "Other subscription active: ${purchase.products}")
                        }
                    }
                }
            } else {
                Log.d(TAG, "No active subscriptions")
                PrefsManagerRemoteConfig.with(activityContext).apply {
                    save(Ads.KEY_IS_PURCHASEDSubs, false)
                    save(Ads.purchasedsubs, "none")
                }
            }
        }
    }


    private val productDetailsResponseListener = ProductDetailsResponseListener { billingResult, productDetailsList ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Product ID: size ${productDetailsList.size}")
            productDetailsList.forEach { productDetails ->
                Log.d(TAG, "Product ID: ${productDetails.productId}")
                if (productIds.contains(productDetails.productId)) {
                    val keyPrice = if (productDetails.productId == Ads.monthlykey)
                        Ads.monthlykey else if (productDetails.productId == Ads.weeklykey) Ads.weeklykey else Ads.yearlykey

                    subscriptionSkuDetails = productDetails

                    val formattedPrice = subscriptionSkuDetails
                        ?.subscriptionOfferDetails?.get(0)
                        ?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice

                    formattedPrice?.let {
                        Log.d(TAG, "Product ID: ${productDetails.productId}, Price: $it")
                        PrefsManagerRemoteConfig.with(activityContext).save(keyPrice, it)
                        onSetPrice?.invoke(it)
                        onUpdatePremiumListener?.setPrice()
                    }

                    if (recallStatus) {
                        recallStatus = false
                        val offerToken = subscriptionSkuDetails
                            ?.subscriptionOfferDetails?.get(0)?.offerToken ?: ""
                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(
                                listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(subscriptionSkuDetails!!)
                                        .setOfferToken(offerToken)
                                        .build()
                                )
                            ).build()
                        billingClient?.launchBillingFlow(activityContext as AppCompatActivity, billingFlowParams)
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        val responseCode = billingResult.responseCode
        if (responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            purchases.forEach { purchase ->
                try {
                    if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                        Toast.makeText(
                            activityContext,
                            activityContext?.getString(R.string.security_issue_not_valid),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            activityContext,
                            activityContext?.getString(R.string.purchase_success_message),
                            Toast.LENGTH_LONG
                        ).show()
                        setPremium(purchase, activityContext!!)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            errorHandle(responseCode)
        }
    }

    private fun setPremium(purchase: Purchase, context: Context) {
        when {
            purchase.products.contains(Ads.weeklykey) -> {
                PrefsManagerRemoteConfig.with(context).apply {
                    save(Ads.KEY_IS_PURCHASEDSubs, true)
                    save(Ads.purchasedsubs, "weekly")
                }
            }
            purchase.products.contains(Ads.monthlykey) -> {
                PrefsManagerRemoteConfig.with(context).apply {
                    save(Ads.KEY_IS_PURCHASEDSubs, true)
                    save(Ads.purchasedsubs, "monthly")
                }
            }
            purchase.products.contains(Ads.yearlykey) -> {
                PrefsManagerRemoteConfig.with(context).apply {
                    save(Ads.KEY_IS_PURCHASEDSubs, true)
                    save(Ads.purchasedsubs, "yearly")
                }
            }
            else -> {
                // In case it's a different product or not recognized
                PrefsManagerRemoteConfig.with(context).apply {
                    save(Ads.KEY_IS_PURCHASEDSubs, true)
                    save(Ads.purchasedsubs, "none")
                }
            }
        }

        acknowledgePurchase(purchase)
        onUpdatePremiumListener?.updateUI()
    }


    private fun verifyValidSignature(signedData: String?, signature: String?): Boolean {
        return try {
            Security.verifyPurchase(signedData.orEmpty(), signature.orEmpty())
        } catch (e: IOException) {
            Log.e(TAG, "Got an exception trying to validate a purchase: $e")
            false
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(acknowledgePurchaseParams) {
                if (it.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully")
                }
            }
        }
    }

    private fun errorHandle(responseCode: Int) {
        when (responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> Log.d(TAG, "User canceled")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> Toast.makeText(
                activityContext, "Item already purchased.", Toast.LENGTH_LONG
            ).show()
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> Toast.makeText(
                activityContext, "Item not available.", Toast.LENGTH_LONG
            ).show()
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> Toast.makeText(
                activityContext, "Service disconnected.", Toast.LENGTH_LONG
            ).show()
            BillingClient.BillingResponseCode.ERROR -> Toast.makeText(
                activityContext, "Billing error.", Toast.LENGTH_LONG
            ).show()
        }
    }

    fun restorePurchases() {
        verifyPurchase()
    }

    fun openSubscriptionManagement() {
        try {
            val uri = Uri.parse(
                "https://play.google.com/store/account/subscriptions?sku=${productIds.first()}&package=${activityContext?.packageName}"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activityContext?.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activityContext,
                "Failed to open subscription management.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
