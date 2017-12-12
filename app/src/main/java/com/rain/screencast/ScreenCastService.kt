package com.rain.screencast

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.os.Parcelable
import android.support.v4.app.NotificationCompat
import android.widget.RemoteViews

class ScreenCastService : Service() {
    private var projection: MediaProjection? = null
    private var imageStreamer: ImageStreamer? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            return Service.START_NOT_STICKY
        }

        if (ACTION_START_STREAM == intent.getStringExtra(EXTRA_DATA)) {
            startStream(intent.getParcelableExtra<Parcelable>(Intent.EXTRA_INTENT) as Intent)
        } else if (ACTION_STOP_STREAM == intent.getStringExtra(EXTRA_DATA)) {
            stopStream()
        }

        return Service.START_STICKY
    }

    private fun startStream(data: Intent) {
        startForeground(NOTIFICATION_STOP_STREAMING, getCustomNotification(NOTIFICATION_STOP_STREAMING))
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

    private fun getCustomNotification(notificationType: Int): Notification {
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        builder.setCategory(Notification.CATEGORY_SERVICE)
        builder.priority = NotificationCompat.PRIORITY_MAX
        builder.setSmallIcon(R.drawable.ic_service_stop_24dp)
        builder.setWhen(0)

        when (notificationType) {
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

    companion object {
        private val EXTRA_DATA = "EXTRA_DATA"
        private val ACTION_START_STREAM = "ACTION_START_STREAM"
        private val ACTION_STOP_STREAM = "ACTION_STOP_STREAM"
        private val NOTIFICATION_CHANNEL_ID = "NOTIFICATION_CHANNEL_ID"
        private val NOTIFICATION_STOP_STREAMING = 11

        internal fun getIntent(context: Context, data: Intent): Intent {
            return Intent(context, ScreenCastService::class.java)
                    .putExtra(EXTRA_DATA, ACTION_START_STREAM)
                    .putExtra(Intent.EXTRA_INTENT, data)
        }

        internal fun getIntentStop(context: Context): Intent {
            return Intent(context, ScreenCastService::class.java)
                    .putExtra(EXTRA_DATA, ACTION_STOP_STREAM)
        }
    }
}
