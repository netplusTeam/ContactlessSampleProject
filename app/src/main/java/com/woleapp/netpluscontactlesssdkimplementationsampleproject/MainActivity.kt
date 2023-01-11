package com.woleapp.netpluscontactlesssdkimplementationsampleproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.netpluspay.contactless.sdk.start.ContactlessSdk
import com.netpluspay.contactless.sdk.utils.ContactlessReaderResult

class MainActivity : AppCompatActivity() {
    private lateinit var readCardButton: Button
    private lateinit var cardResultTextView: TextView
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
                data?.let { i ->
                    val cardDataRead = i.getStringExtra("data")
                    cardResultTextView.text = cardDataRead
                    Log.e("tag", "value: ${i.getStringExtra("data")}")
                }
            }
            if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
                data?.let { i ->
                    val error = i.getStringExtra("data")
                    cardResultTextView.text = error
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialize Views
        initializeViews()
        readCardButton.setOnClickListener {
            ContactlessSdk.readContactlessCard(
                this,
                resultLauncher,
                "86CBCDE3B0A22354853E04521686863D", // pinKey
                100.0, // amount
                0.0 // cashbackAmount(optional)
            )
        }
    }

    private fun initializeViews() {
        readCardButton = findViewById(R.id.read_card_btn)
        cardResultTextView = findViewById(R.id.result_tv)
    }
}
