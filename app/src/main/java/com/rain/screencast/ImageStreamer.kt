package com.rain.screencast

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.socket.client.IO
import io.socket.client.Socket
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class ImageStreamer(context: Context, private val mediaProjection: MediaProjection) {
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handlerThread: HandlerThread = HandlerThread(ImageStreamer::class.java.simpleName, Process.THREAD_PRIORITY_BACKGROUND)
    private val lock = Any()
    private val handler: Handler
    private var socket: Socket? = null
    private var disposable: Disposable? = null
    private val imageReader: ImageReader
    private var virtualDisplay: VirtualDisplay? = null
    @Volatile
    private var reusableBitmap: Bitmap? = null
    private val resultJpegStream = ByteArrayOutputStream()
    private val screenSize: Point
    private val displayMetrics: DisplayMetrics
    private val tag = "ImageStreamer"

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        val defaultDisplay = windowManager.defaultDisplay
        displayMetrics = DisplayMetrics()
        screenSize = Point()
        defaultDisplay.getMetrics(displayMetrics)
        defaultDisplay.getRealSize(screenSize)
        imageReader = ImageReader.newInstance(screenSize.x, screenSize.y, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener(ImageListener(), handler)
    }

    fun bind() {
        disposable = Observable.interval(16, TimeUnit.MILLISECONDS)
                .doOnSubscribe { initSocket() }
                .map { windowManager.defaultDisplay.rotation }
                .map { rotation -> rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180 }
                .distinctUntilChanged()
                .skip(1)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe { capture() }
    }

    private fun initSocket() {
        socket = IO.socket("http://10.0.2.2:3000")
        socket?.connect()
        socket?.once(Socket.EVENT_CONNECT) {
            Log.d(tag, "connected")
            capture()
        }
        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d(tag, "error: $it")
        }
    }

    private fun capture() {
        synchronized(lock) {
            virtualDisplay = try {
                mediaProjection.createVirtualDisplay("SSVirtualDisplay", screenSize.x, screenSize.y, displayMetrics.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, null)
            } catch (e: SecurityException) {
                e.printStackTrace()
                null
            }
        }
    }

    private inner class ImageListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            synchronized(lock) {
                val image: Image?
                try {
                    image = reader.acquireLatestImage()
                } catch (exception: UnsupportedOperationException) {
                    exception.printStackTrace()
                    return
                }
                if (null == image) return

                val plane = image.planes[0]
                val width = plane.rowStride / plane.pixelStride

                val cleanBitmap: Bitmap
                if (width > image.width) {
                    if (null == reusableBitmap) reusableBitmap = Bitmap.createBitmap(width, image.height, Bitmap.Config.ARGB_8888)
                    reusableBitmap?.copyPixelsFromBuffer(plane.buffer)
                    cleanBitmap = Bitmap.createBitmap(reusableBitmap!!, 0, 0, image.width, image.height)
                } else {
                    cleanBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    cleanBitmap.copyPixelsFromBuffer(plane.buffer)
                }

                image.close()
                resultJpegStream.reset()
                cleanBitmap.compress(Bitmap.CompressFormat.JPEG, 50, resultJpegStream)
                socket?.emit("stream", Base64.encodeToString(resultJpegStream.toByteArray(), Base64.DEFAULT))
            }
        }
    }

    fun unbind() {
        disposable?.dispose()
        disposable = null
        socket?.close()
        socket = null
        virtualDisplay?.release()
        virtualDisplay = null
        reusableBitmap?.recycle()
        reusableBitmap = null
    }
}
