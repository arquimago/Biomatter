package br.com.leaderp.biomatter.biomatter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var mCM: String? = null
    private var mUM: ValueCallback<*>? = null
    private var mUMA: ValueCallback<Array<Uri>>? = null

    //select whether you want to upload multiple files (set 'true' for yes)
    private val multiple_files = false

    lateinit var web: WebView

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (Build.VERSION.SDK_INT >= 21) {
            var results: Array<Uri>? = null
            //checking if response is positive
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) {
                        return
                    }
                    if (intent == null || intent.data == null) {
                        if (mCM != null) {
                            results = arrayOf(Uri.parse(mCM))
                        }
                    } else {
                        val dataString = intent.dataString
                        if (dataString != null) {
                            results = arrayOf(Uri.parse(dataString))
                        } else {
                            if (multiple_files) {
                                if (intent.clipData != null) {
                                    val numSelectedFiles = intent.clipData!!.itemCount
                                    results = arrayOf()
                                    for (i in 0 until numSelectedFiles) {
                                        results[i] = intent.clipData!!.getItemAt(i).uri
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mUMA!!.onReceiveValue(results)
            mUMA = null
        } else {
            if (requestCode == FCR) {
                if (null == mUM) return
                val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                if (result == null) mUM!!.onReceiveValue(result)
                mUM = null
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        web = this.webview

        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                1
            )
        }

        web.webViewClient = MyWebClient()
        val webSettings = web.settings
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true

        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.mixedContentMode = 0
            web.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else if (Build.VERSION.SDK_INT >= 19) {
            web.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            web.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        web.loadUrl("https://biomatter.leaderp.com.br/")
        web.webChromeClient = object : WebChromeClient() {
            //handling input[type="file"] requests for android API 16+
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) {
                mUM = uploadMsg
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "*/*"
                if (multiple_files) {
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FCR)
            }

            //handling input[type="file"] requests for android API 21+
            override fun onShowFileChooser(
                web: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                val perms = arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )

                //checking for storage permission to write images for upload
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(this@MainActivity, perms, FCR)

                    //checking for WRITE_EXTERNAL_STORAGE permission
                } else if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                        FCR
                    )

                    //checking for CAMERA permissions
                } else if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), FCR)
                }
                if (mUMA != null) {
                    mUMA!!.onReceiveValue(null)
                }
                mUMA = filePathCallback
                var takePictureIntent: Intent?
                takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(this@MainActivity.packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCM)
                    } catch (ex: IOException) {
                        Log.e(TAG, "Image file creation failed", ex)
                    }

                    if (photoFile != null) {
                        mCM = "file:" + photoFile.absolutePath
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                    } else {
                        takePictureIntent = null
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"
                if (multiple_files) {
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                val intentArray: Array<Intent>
                if (takePictureIntent != null) {
                    intentArray = arrayOf(takePictureIntent)
                } else {
                    intentArray = arrayOf()
                }

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, FCR)
                return true
            }
        }
    }

    //creating new image file here
    @Throws(IOException::class)
    private fun createImageFile(): File {
        @SuppressLint("SimpleDateFormat")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    //back/down key handling
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (web.canGoBack()) {
                        web.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val FCR = 1
    }

    inner class MyWebClient : WebViewClient() {
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            Toast.makeText(applicationContext, "Failed loading app!", Toast.LENGTH_SHORT).show()
            super.onReceivedError(view, request, error)
        }
    }
}
