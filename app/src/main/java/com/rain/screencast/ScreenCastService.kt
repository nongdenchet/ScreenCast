package com.rain.screencast

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Parcelable
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class ScreenCastService : Service() {
    private var projection: MediaProjection? = null
    private var imageStreamer: ImageStreamer? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_STOP_STREAMING, NOTIFICATION_STOP_STREAMING.getCustomNotification())
    }

    override fun onDestroy() {
        stopStream()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            return START_NOT_STICKY
        }

        if (imageStreamer == null && projection == null) {
            stopStream()
            startStream(intent.getParcelableExtra<Parcelable>(Intent.EXTRA_INTENT) as Intent)
        } else {
            Toast.makeText(this, "Already started", Toast.LENGTH_SHORT).show()
        }

        return START_STICKY
    }

    private fun startStream(data: Intent) {
        val projectionManager: MediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = projectionManager.getMediaProjection(Activity.RESULT_OK, data)
        imageStreamer = ImageStreamer(applicationContext, projection!!)
        imageStreamer?.bind()
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopForeground(true)
            }
        }, null)
    }

    private fun stopStream() {
        projection?.stop()
        projection = null
        imageStreamer?.unbind()
        imageStreamer = null
    }

    private fun Int.getCustomNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        builder.setCategory(Notification.CATEGORY_SERVICE)
        builder.priority = NotificationCompat.PRIORITY_MAX
        builder.setSmallIcon(R.drawable.ic_service_stop_24dp)
        builder.setWhen(0)

        when (this) {
            NOTIFICATION_STOP_STREAMING -> {
                val stopIntent = PendingIntent.getActivity(applicationContext, 2,
                        MainActivity.getStartIntent(applicationContext, MainActivity.ACTION_STOP_STREAM),
                        PendingIntent.FLAG_UPDATE_CURRENT)
                val smallView = RemoteViews(packageName, R.layout.stop_notification)
                smallView.setImageViewResource(R.id.imageViewStopNotificationSmallIconStop, R.drawable.ic_service_stop_24dp)
                smallView.setOnClickPendingIntent(R.id.imageViewStopNotificationSmallIconStop, stopIntent)
                builder.setCustomContentView(smallView)
            }
        }

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE)
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(chan)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "NOTIFICATION_CHANNEL_ID"
        private const val NOTIFICATION_CHANNEL_NAME = "NOTIFICATION_CHANNEL_NAME"
        private const val NOTIFICATION_STOP_STREAMING = 11

        internal fun getIntent(context: Context, data: Intent? = null): Intent {
            return Intent(context, ScreenCastService::class.java)
                    .putExtra(Intent.EXTRA_INTENT, data)
        }
    }
}
