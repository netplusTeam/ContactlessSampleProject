package com.woleapp.netpluscontactlesssdkimplementationsampleproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.clearPinKey
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.google.gson.Gson
import com.netpluspay.contactless.sdk.start.ContactlessSdk
import com.netpluspay.contactless.sdk.utils.ContactlessReaderResult
import com.netpluspay.nibssclient.models.IsoAccountType
import com.netpluspay.nibssclient.models.MakePaymentParams
import com.netpluspay.nibssclient.models.UserData
import com.netpluspay.nibssclient.service.NetposPaymentClient
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.CARD_HOLDER_NAME
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.CONFIG_DATA
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.ERROR_TAG
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.KEY_HOLDER
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.PAYMENT_ERROR_DATA_TAG
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.PAYMENT_SUCCESS_DATA_TAG
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.POS_ENTRY_MODE
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.TAG_CHECK_BALANCE
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.TAG_MAKE_PAYMENT
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.TAG_TERMINAL_CONFIGURATION
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.getSampleUserData
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.AppUtils.getSavedKeyHolder
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.dialog.LoadingDialog
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.models.CardResult
import com.woleapp.netpluscontactlesssdkimplementationsampleproject.models.Status
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private val gson: Gson = Gson()
    private lateinit var makePaymentButton: Button
    private lateinit var checkBalanceButton: Button
    private lateinit var resultViewerTextView: TextView
    private lateinit var amountET: EditText
    private lateinit var accountType: com.danbamitale.epmslib.utils.IsoAccountType
    private var userData: UserData = getSampleUserData()
    private var cardData: CardData? = null
    private var previousAmount: Long? = null
//    private var btnColor:Int = resources.getColor(com.netpluspay.contactless.sdk.R.color.colorPrimary)
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    var netposPaymentClient: NetposPaymentClient = NetposPaymentClient
    private val makePaymentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
                data?.let { i ->
                    val amountToPay = amountET.text.toString().toLong()
                    amountET.text.clear()
                    val cardReadData = i.getStringExtra("data")!!
                    val cardResult = gson.fromJson(cardReadData, CardResult::class.java)
                    makePayment(cardResult, amountToPay)
                }
            }
            if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
                data?.let { i ->
                    val error = i.getStringExtra("data")
                    error?.let {
                        Timber.d("ERROR_TAG===>%s", it)
                        resultViewerTextView.text = it
                    }
                }
            }
        }

    private val checkBalanceResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
                data?.let { i ->
                    val cardReadData = i.getStringExtra("data")!!
                    val cardResult = gson.fromJson(cardReadData, CardResult::class.java)
                    checkBalance(cardResult)
                }
            }
            if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
                data?.let { i ->
                    val error = i.getStringExtra("data")
                    error?.let {
                        Timber.d("ERROR_TAG===>%s", it)
                        resultViewerTextView.text = it
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialize Views
        initializeViews()
        configureTerminal()
//        val btnColor =
        netposPaymentClient.logUser(this, gson.toJson(userData))
        Timber.d("DEVICE_SERIAL_NUMBER===>%s", getSampleUserData().terminalSerialNumber)
        makePaymentButton.setOnClickListener {
            resultViewerTextView.text = ""
            if (amountET.text.isNullOrEmpty() || amountET.text.toString().toLong() < 200L) {
                Toast.makeText(this, getString(R.string.enter_valid_amount), Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            val amountToPay = amountET.text.toString().toLong().toDouble()
            launchContactless(makePaymentResultLauncher, amountToPay)
        }
//        val btnColor = this.resources.getColor(R.color.black)
//
//        checkBalanceButton.setOnClickListener {
//            launchContactless(checkBalanceResultLauncher, 200.0, btnColor)
//        }
    }

    private fun launchContactless(
        launcher: ActivityResultLauncher<Intent>,
        amountToPay: Double,
        cashBackAmount: Double = 0.0,
    ) {
        val savedKeyHolder = getSavedKeyHolder()

        savedKeyHolder?.run {
            ContactlessSdk.readContactlessCard(
                this@MainActivity,
                launcher,
                this.clearPinKey, // "86CBCDE3B0A22354853E04521686863D" // pinKey
                amountToPay, // amount
                cashBackAmount, // cashbackAmount(optional)
                colorCode = R.color.teal_700
                )
        } ?: run {
            Toast.makeText(
                this,
                getString(R.string.terminal_not_configured),
                Toast.LENGTH_LONG,
            ).show()
            configureTerminal()
        }
    }

    private fun initializeViews() {
        makePaymentButton = findViewById(R.id.read_card_btn)
        resultViewerTextView = findViewById(R.id.result_tv)
        amountET = findViewById(R.id.amountToPay)
        checkBalanceButton = findViewById(R.id.check_balance)
    }

    private fun configureTerminal() {
        val loaderDialog: LoadingDialog = LoadingDialog()
        loaderDialog.loadingMessage = getString(R.string.configuring_terminal)
        loaderDialog.show(supportFragmentManager, TAG_TERMINAL_CONFIGURATION)
        compositeDisposable.add(
            netposPaymentClient.init(this, Gson().toJson(userData))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let { response ->
                        Toast.makeText(
                            this,
                            getString(R.string.terminal_configured),
                            Toast.LENGTH_LONG,
                        ).show()
                        loaderDialog.dismiss()
                        val keyHolder = response.first
                        val configData = response.second
                        val pinKey = keyHolder?.clearPinKey
                        if (pinKey != null) {
                            Prefs.putString(KEY_HOLDER, gson.toJson(keyHolder))
                            Prefs.putString(CONFIG_DATA, gson.toJson(configData))
                        }
                    }
                    error?.let {
                        Toast.makeText(
                            this,
                            getString(R.string.terminal_config_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                        loaderDialog.dismiss()
                        Timber.d("%s%s", ERROR_TAG, it.localizedMessage)
                    }
                },
        )
    }

    private fun makePayment(cardResult: CardResult, amountToPay: Long) {
        val loaderDialog: LoadingDialog = LoadingDialog()
        loaderDialog.loadingMessage = getString(R.string.processing_payment)
        loaderDialog.show(supportFragmentManager, TAG_MAKE_PAYMENT)
        val cardData = cardResult.cardReadResult.let {
            CardData(it.track2Data, it.iccString, it.pan, POS_ENTRY_MODE)
        }

        val makePaymentParams =
            cardData.let { cdData ->
                previousAmount = amountToPay
                MakePaymentParams(
                    amount = amountToPay,
                    terminalId = userData.terminalId,
                    cardData = cdData,
                    accountType = IsoAccountType.SAVINGS,
                    remark = ""
                )
            }
        cardData.pinBlock = cardResult.cardReadResult.pinBlock
        Log.d("CHECKING", "CHECKING_HERE")
        compositeDisposable.add(
            netposPaymentClient.makePayment(
                this,
                userData.terminalId,
                gson.toJson(makePaymentParams),
                cardResult.cardScheme,
                CARD_HOLDER_NAME,
                rrn = "123456789101",
                stan = "123456"
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { transactionWithRemark ->
                        loaderDialog.dismiss()
                        resultViewerTextView.text = gson.toJson(transactionWithRemark)
                        Timber.d(
                            "$PAYMENT_SUCCESS_DATA_TAG%s",
                            gson.toJson(transactionWithRemark),
                        )
                    },
                    { throwable ->
                        loaderDialog.dismiss()
                        resultViewerTextView.text = throwable.localizedMessage
                        Timber.d(
                            "$PAYMENT_ERROR_DATA_TAG%s",
                            throwable.localizedMessage,
                        )
                    },
                ),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun checkBalance(cardResult: CardResult) {
        val loaderDialog: LoadingDialog = LoadingDialog()
        loaderDialog.loadingMessage = getString(R.string.checking_balance)
        loaderDialog.show(supportFragmentManager, TAG_CHECK_BALANCE)
        val cardData = cardResult.cardReadResult.let {
            CardData(it.track2Data, it.iccString, it.pan, POS_ENTRY_MODE).also { cardD ->
                cardD.pinBlock = it.pinBlock
            }
        }
        compositeDisposable.add(
            netposPaymentClient.balanceEnquiry(this, cardData, IsoAccountType.SAVINGS.name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        loaderDialog.dismiss()
                        val responseString = if (it.responseCode == Status.APPROVED.statusCode) {
                            "Response: APPROVED\nResponse Code: ${it.responseCode}\n\nAccount Balance:\n" + it.accountBalances.joinToString(
                                "\n",
                            ) { accountBalance ->
                                "${accountBalance.accountType}: ${
                                    accountBalance.amount.div(100).formatCurrencyAmount()
                                }"
                            }
                        } else {
                            "Response: ${it.responseMessage}\nResponse Code: ${it.responseCode}"
                        }
                        resultViewerTextView.text = responseString
                    }
                    error?.let {
                        loaderDialog.dismiss()
                        resultViewerTextView.text = it.localizedMessage
                    }
                },
        )
    }
}
