package com.akumau.oc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var webViewInstance: WebView? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            val results: Array<Uri>? = when {
                intentData?.clipData != null -> {
                    val count = intentData.clipData!!.itemCount
                    Array(count) { i -> intentData.clipData!!.getItemAt(i).uri }
                }
                intentData?.data != null -> arrayOf(intentData.data!!)
                else -> null
            }
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OCEditorWebView()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun OCEditorWebView() {
        val webView = remember {
            WebView(this@MainActivity).apply {
                webViewInstance = this
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                addJavascriptInterface(WebAppInterface(), "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        this@MainActivity.filePathCallback?.onReceiveValue(null)
                        this@MainActivity.filePathCallback = filePathCallback

                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }

                        try {
                            fileChooserLauncher.launch(intent)
                        } catch (e: Exception) {
                            this@MainActivity.filePathCallback = null
                            return false
                        }
                        return true
                    }
                }

                loadUrl("file:///android_asset/www/index.html")
            }
        }

        BackHandler(enabled = webView.canGoBack()) {
            webView.goBack()
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { webView }
        )
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun saveBlob(base64Data: String, fileName: String, mimeType: String) {
            try {
                val cleanFileName = if (fileName.isBlank()) "output_file.bin" else fileName
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)

                // Save to app cache directory first
                val cacheDir = File(cacheDir, "downloads").apply { mkdirs() }
                val targetFile = File(cacheDir, cleanFileName)
                FileOutputStream(targetFile).use { it.write(bytes) }

                // Share file so user can save to device storage / send
                val contentUri: Uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${applicationContext.packageName}.fileprovider",
                    targetFile
                )

                val effectiveMime = if (mimeType.isBlank()) "application/octet-stream" else mimeType
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = effectiveMime
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, cleanFileName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Simpan / Bagikan $cleanFileName")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Menyimpan $cleanFileName", Toast.LENGTH_SHORT).show()
                    startActivity(chooser)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Gagal menyimpan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
