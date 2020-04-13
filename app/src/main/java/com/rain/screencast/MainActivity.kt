package com.rain.screencast

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        findViewById<Button>(R.id.startBtn).setOnClickListener {
            startActivityForResult(mediaProjectionManager!!.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
        }
    }

    override fun onDestroy() {
        disposable?.dispose()
        disposable = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (Activity.RESULT_OK != resultCode) {
                Toast.makeText(this, "Screen cast permission denied", Toast.LENGTH_SHORT).show()
                return
            }

            if (null == data) {
                Toast.makeText(this, "No screen cast found", Toast.LENGTH_SHORT).show()
                return
            }

            ContextCompat.startForegroundService(applicationContext, ScreenCastService.getIntent(applicationContext, data))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.getStringExtra(EXTRA_DATA) ?: return
        when (action) {
            ACTION_STOP_STREAM -> {
                stopService(ScreenCastService.getIntent(applicationContext))
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 10001
        const val ACTION_STOP_STREAM = "ACTION_STOP_STREAM"
        private const val EXTRA_DATA = "EXTRA_DATA"

        fun getStartIntent(context: Context, action: String): Intent {
            return Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_DATA, action)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }
}
