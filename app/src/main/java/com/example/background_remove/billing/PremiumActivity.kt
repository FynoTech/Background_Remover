package com.example.background_remove.billing

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.example.background_remove.BaseActivity
import com.example.background_remove.MainActivity
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.billingsubs.BillingsHelperSubsCription
import com.example.background_remove.utils.PrefsManagerRemoteConfig
import com.example.background_remove.utils.PrivacyPolicyUtils
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
import com.google.common.collect.ImmutableList
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityPurchaseBinding


class PremiumActivity : BaseActivity() {
    private lateinit var binding: ActivityPurchaseBinding
    private var billingsHelper: BillingsHelper? = null
    var checktype = "weekly"
    private var billingsHelpersybs: BillingsHelperSubsCription? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)
        binding.text.visibility = View.VISIBLE
        setListeners()
        checkPremium()
        updatePlanTitleStyles()

        if (PrefsManagerRemoteConfig.with(this@PremiumActivity)
                .getBoolean(Ads.KEY_IS_PURCHASED, false)
            || PrefsManagerRemoteConfig.with(this@PremiumActivity)
                .getBoolean(Ads.KEY_IS_PURCHASEDSubs, false)
        ) {
            updateButton()
        }
        updateButton()
        for (i in 0 until 3) {
            when (i) {
                0 -> {
                    //   billingsHelper?.selectedProductId=HomeNativeAd.product_weekly
                    //   billingsHelper?.selectedProductType=ProductType.SUBS
                    setSkuValues()
                }

                1 -> {
                    // billingsHelper?.selectedProductId=HomeNativeAd.product_monthly
                    // billingsHelper?.selectedProductType=ProductType.SUBS
                    setSkuValues()
                }

                2 -> {
                   /* billingsHelper?.selectedProductId = Ads.lifetimekey
                    billingsHelper?.selectedProductType = ProductType.INAPP
                    setSkuValues()*/
                }
            }

        }


        setSkuValues()

        backPressed()
    }

    private fun setStrikeThroughPrice(text: String) {
        try {
            val spannableString = SpannableString(text)
            spannableString.setSpan(
                StrikethroughSpan(),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // binding.tvStrikeText.text = spannableString
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPremium() {

        billingsHelper = BillingsHelper.getInstance(this)
        billingsHelper?.setListener(premiumListener)
        billingsHelper?.initializeBillingClient(this)
        BillingsHelper.billingClient = billingsHelper?.connectToBillingService()


        billingsHelpersybs = BillingsHelperSubsCription.getInstance(this)
        billingsHelpersybs?.setListener(null)
        billingsHelpersybs?.initializeBillingClient(this)
        BillingsHelperSubsCription.billingClient = billingsHelpersybs?.connectToBillingService()

    }

    private val premiumListener: OnUpdatePremiumListener = object : OnUpdatePremiumListener {
        override fun setPrice() {
            setSkuValues()
        }

        override fun updateUI() {
            if (PrefsManagerRemoteConfig.with(this@PremiumActivity)
                    .getBoolean(Ads.yearlykey, false)
            ) {

                val intent = Intent(this@PremiumActivity, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }
        }
    }
    private val premiumListener2: OnUpdatePremiumListener =
        object : OnUpdatePremiumListener {
            override fun setPrice() {
                setSkuValues()
            }

            override fun updateUI() {
                if (PrefsManagerRemoteConfig.with(this@PremiumActivity)
                        .getBoolean(Ads.KEY_IS_PURCHASEDSubs, false)
                ) {

                    val intent = Intent(this@PremiumActivity, MainActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                }
            }
        }

    private fun processFormattedPrice(
        formattedPrice: String,
        currencyCode: String
    ): String {
        return try {
            val numericPart = "(\\d+(?:\\.\\d+)?)".toRegex().find(formattedPrice)?.value
                ?.toDoubleOrNull()
                ?: return "$formattedPrice $currencyCode" // Return original if parsing fails

            val calculatedPrice = numericPart * 2.0
            "%s %.2f".format(currencyCode, calculatedPrice)
        } catch (e: Exception) {
            e.printStackTrace()
            "$formattedPrice $currencyCode" // Return original if an error occurs
        }
    }



    private fun setSkuValues() {
        try {
            /*   if (billingsHelper?.selectedProductId == Ads.lifetimekey) {
                   binding.lifetimeprice.text = PrefsManagerRemoteConfig.with(this)
                       .getString(Ads.lifetimekey, "$39.9")

                   binding.lifetimeprice.text =
                       PrefsManagerRemoteConfig.with(this)
                           .getString(Ads.lifetimekey, "$39.9")


               }*/

            val yearly = PrefsManagerRemoteConfig.with(this)
                .getString(Ads.yearlykey, "$19.99")
                .ifBlank { "$19.99" }
            val monthly = PrefsManagerRemoteConfig.with(this)
                .getString(Ads.monthlykey, "$9.99")
                .ifBlank { "$9.99" }
            val weekly = PrefsManagerRemoteConfig.with(this)
                .getString(Ads.weeklykey, "$2.99")
                .ifBlank { "$2.99" }

            binding.yearlyprice.text = yearly
            binding.monthlyprice.text = monthly
            binding.weeklyprice.text = weekly

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setListeners() {
        binding.closeBtn.setSafeClickListenertools {

            if (intent.getStringExtra("from") == "splash") {
                val intent = Intent(this@PremiumActivity, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
            } else {
                finish()
            }
        }

        binding.terms.paintFlags = binding.terms.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvPrivacy.paintFlags = binding.terms.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvPrivacy.visibility =
            if (RemoteConfigManager.isPrivacyPolicyEnabled()) View.VISIBLE else View.GONE
//.....................LIFE TIME ........................
        /*  binding.cardlifetime.setSafeClickListener(this) {
              checktype = "yearly"
              binding.cardlifetime.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_selected)
              binding.cardWeekly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_dark)
              binding.cardMonthly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_dark)
              binding.text.visibility = View.VISIBLE
              updateButton()

          }*/


        binding.terms.setSafeClickListenertools {
            val otherAppIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.freeprivacypolicy.com/live/d22d26fb-208a-43ef-ae00-5726550dbf65")
            )
            startActivity(otherAppIntent)
        }
        binding.btnPurchase.setSafeClickListenertools {
            when (checktype) {
                /*   "yearly" -> {
                       billingsHelper?.apply {
                           checkPremium()
                           launchBillingCus(billingsHelper?.lifeTimeSkuDetails)
                       }
                   }*/
                "yearly" -> {
                    BillingsHelperSubsCription.selectedProductId = Ads.yearlykey
                    BillingsHelperSubsCription.billingClient = null
                    billingsHelpersybs = BillingsHelperSubsCription.getInstance(this)
                    billingsHelpersybs?.setListener(premiumListener2)
                    billingsHelpersybs?.initializeBillingClient(this)
                    BillingsHelperSubsCription.billingClient =
                        billingsHelpersybs?.connectToBillingService()

                    Handler(Looper.getMainLooper()).postDelayed({
                        launchBillingFlow1(billingsHelpersybs?.subscriptionSkuDetails)
                    }, 300)
                }
                "monthly" -> {
                    BillingsHelperSubsCription.selectedProductId = Ads.monthlykey
                    BillingsHelperSubsCription.billingClient = null
                    billingsHelpersybs = BillingsHelperSubsCription.getInstance(this)
                    billingsHelpersybs?.setListener(premiumListener2)
                    billingsHelpersybs?.initializeBillingClient(this)
                    BillingsHelperSubsCription.billingClient =
                        billingsHelpersybs?.connectToBillingService()

                    Handler(Looper.getMainLooper()).postDelayed({
                        launchBillingFlow1(billingsHelpersybs?.subscriptionSkuDetails)
                    }, 300)
                }

                "weekly" -> {
                    BillingsHelperSubsCription.selectedProductId = Ads.weeklykey
                    BillingsHelperSubsCription.billingClient = null
                    billingsHelpersybs = BillingsHelperSubsCription.getInstance(this)
                    billingsHelpersybs?.setListener(premiumListener2)
                    billingsHelpersybs?.initializeBillingClient(this)
                    BillingsHelperSubsCription.billingClient =
                        billingsHelpersybs?.connectToBillingService()

                    Handler(Looper.getMainLooper()).postDelayed({
                        launchBillingFlow1(billingsHelpersybs?.subscriptionSkuDetails)
                    }, 300)
                }
            }

        }

        binding.cardMonthly.setSafeClickListenertools {
            binding.cardMonthly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_selected)
            binding.cardYearly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_dark)
            binding.cardWeekly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_dark)
            checktype = "monthly"
            binding.text.visibility = View.VISIBLE
            updatePlanTitleStyles()
            updateButton()

        }
        binding.cardYearly.setSafeClickListenertools {
            binding.cardYearly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_selected)
            binding.cardMonthly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_dark)
            binding.cardWeekly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_dark)
            checktype = "yearly"
            binding.text.visibility = View.VISIBLE
            updatePlanTitleStyles()
            updateButton()

        }

// WEEKLY
        binding.cardWeekly.setSafeClickListenertools {
            binding.cardWeekly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_selected)
            binding.cardYearly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_dark)
            binding.cardMonthly.background = ContextCompat.getDrawable(this, R.drawable.sub_plan_dark)
            checktype = "weekly"
            binding.text.visibility = View.VISIBLE
            updatePlanTitleStyles()
            updateButton()
        }



        binding.tvPrivacy.setSafeClickListenertools {
            PrivacyPolicyUtils.openPrivacyPolicyIfPossible(this)
        }
    }

    var check = false

    private fun updatePlanTitleStyles() {
        binding.tvMonthlyTitle.setTypeface(null, if (checktype == "monthly") Typeface.BOLD else Typeface.NORMAL)
        binding.tvWeeklyTitle.setTypeface(null, if (checktype == "weekly") Typeface.BOLD else Typeface.NORMAL)
        binding.tvYearlyTitle.setTypeface(null, if (checktype == "yearly") Typeface.BOLD else Typeface.NORMAL)
    }

    private fun launchBillingCus(skuDetails1: ProductDetails?) {
        if (!PrefsManagerRemoteConfig.with(this@PremiumActivity)
                .getBoolean(Ads.KEY_IS_PURCHASED, false)
        ) {
            Log.d("abcd", "launchBillingCus:name  ${skuDetails1?.name}")

            if (skuDetails1 != null) {
                Log.d("abcd", "startPurchase2: asdasd")
                startPurchase(skuDetails1)
            } else {
                billingsHelper?.startConnection(true)
            }

        } else {
            updateButton()
        }
    }

    private fun startPurchase(skuDetails: ProductDetails?) {
        Log.d("abcd", "startPurchase1: ${skuDetails?.productType} ${skuDetails?.name}  ")
        if (skuDetails != null) {
            val flowParams: BillingFlowParams =
                BillingFlowParams.newBuilder().setProductDetailsParamsList(
                    ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(skuDetails).build()
                    )
                ).build()
            Log.d("abcd", "startPurchase: asdasd")
            BillingsHelper.billingClient?.launchBillingFlow(this, flowParams)
        } else {
            billingsHelper?.startConnection(true)
        }
    }

    private fun launchBillingFlow1(skuDetails: ProductDetails?) {

        if (skuDetails != null) {
            startPurchase1(skuDetails)
        } else {
            billingsHelpersybs?.startConnection(true)
        }
    }


    private fun startPurchase1(skuDetails: ProductDetails?) {
        Log.d("abcd", "startPurchase1: ${skuDetails?.productType} ${skuDetails?.name}  ")
        if (skuDetails != null) {

            val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(skuDetails)
            if (skuDetails.subscriptionOfferDetails?.isNotEmpty() == true) {
                val offerToken = skuDetails.subscriptionOfferDetails!!.first().offerToken
                productDetailsParamsBuilder.setOfferToken(offerToken)
            }

            val flowParams: BillingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    ImmutableList.of(
                        productDetailsParamsBuilder.build()
                    )
                ).build()

            BillingsHelperSubsCription.billingClient?.launchBillingFlow(this, flowParams)
        } else {

            billingsHelpersybs?.startConnection(true)
        }
    }

    private fun updateButton() {
        binding.btnPurchase.text =
            if (checktype == "weekly") getString(R.string.subscription_try_free_3_days_trial)
            else getString(R.string.continue_text)
        var lifetime = PrefsManagerRemoteConfig.with(this@PremiumActivity)
            .getBoolean(Ads.KEY_IS_PURCHASED, false)
        var subsCription = PrefsManagerRemoteConfig.with(this@PremiumActivity)
            .getBoolean(Ads.KEY_IS_PURCHASEDSubs, false)

        disableButton(
            binding.btnPurchase,
            getString(R.string.already_purchased),
            PrefsManagerRemoteConfig.with(this@PremiumActivity)
                .getString(Ads.purchasedsubs, "none"), lifetime, subsCription
        )


    }

    private fun disableButton(
        button: View,
        message: String,
        string: String,
        lifetime: Boolean,
        subsCription: Boolean,
    ) {

        Log.d("asdasdasd", "disableButton: $lifetime $subsCription $string")
        if (lifetime) {
            if (button is TextView) {
                button.text = message
                button.alpha = 0.4f
                button.isClickable = false
                button.isEnabled = false
            }
        } else {
            if (string == "weekly" && checktype == "weekly") {
                if (button is TextView) {
                    button.text = message
                    button.alpha = 0.4f
                    button.isClickable = false
                    button.isEnabled = false
                }
            } else if (string == "monthly" && checktype == "monthly") {
                if (button is TextView) {
                    button.text = message
                    button.alpha = 0.4f
                    button.isClickable = false
                    button.isEnabled = false

                }
            } else if (string == "yearly" && checktype == "yearly") {
                if (button is TextView) {
                    button.text = message
                    button.alpha = 0.4f
                    button.isClickable = false
                    button.isEnabled = false

                }
            } else {
                if (button is TextView) {
                    button.text =
                        if (checktype == "weekly") getString(R.string.subscription_try_free_3_days_trial)
                        else getString(R.string.continue_text)
                    button.alpha = 1f
                    button.isClickable = true
                    button.isEnabled = true

                }
            }


        }
    }

    fun doubleCurrency(input: String): String {
        // Use a regular expression to separate the currency symbol and the number
        val regex = """([^\d.,]+)\s?([\d,]+\.\d{2})""".toRegex()
        val matchResult = regex.matchEntire(input) ?: return ""

        // If the input doesn't match the regex, return an empty string

        // Extract the currency symbol and the numeric part
        val currency = matchResult.groupValues[1].trim()
        val number = matchResult.groupValues[2].replace(",", "").toDouble()

        // Add the increment
        val updatedValue = number * 2

        // Format the updated value back to the original format
        return "$currency %,1.2f".format(updatedValue)
    }


    private fun backPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                if (intent.getStringExtra("from") == "splash") {
                    val intent = Intent(this@PremiumActivity, MainActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                } else {
                    finish()
                }

            }
        })
    }
}