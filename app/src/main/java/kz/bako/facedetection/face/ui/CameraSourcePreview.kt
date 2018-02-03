package kz.bako.facedetection.face.ui

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.google.android.gms.vision.CameraSource
import java.io.IOException

class CameraSourcePreview(private val mContext: Context, attrs: AttributeSet) : ViewGroup(mContext, attrs) {

	companion object {
		private val TAG = "CameraSourcePreview"
	}

	private val mSurfaceView: SurfaceView
	private var mStartRequested: Boolean = false
	private var mSurfaceAvailable: Boolean = false
	private var mCameraSource: CameraSource? = null

	private lateinit var mOverlay: GraphicOverlay

	private val isPortraitMode: Boolean
		get() {
			val orientation = mContext.resources.configuration.orientation
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				return false
			}
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				return true
			}

			Log.d(TAG, "isPortraitMode returning false by default")
			return false
		}

	init {
		mStartRequested = false
		mSurfaceAvailable = false

		mSurfaceView = SurfaceView(mContext)
		mSurfaceView.holder.addCallback(SurfaceCallback())
		addView(mSurfaceView)
	}

	@Throws(IOException::class)
	fun start(cameraSource: CameraSource?) {
		if (cameraSource == null) {
			stop()
		}

		mCameraSource = cameraSource

		if (mCameraSource != null) {
			mStartRequested = true
			startIfReady()
		}
	}

	@Throws(IOException::class)
	fun start(cameraSource: CameraSource, overlay: GraphicOverlay) {
		mOverlay = overlay
		start(cameraSource)
	}

	fun stop() {
		mCameraSource?.stop()
	}

	fun release() {
		mCameraSource?.release()
		mCameraSource = null
	}

	@Throws(IOException::class)
	private fun startIfReady() {
		if (mStartRequested && mSurfaceAvailable) {
			mCameraSource?.start(mSurfaceView.holder)
			mCameraSource?.let { cameraSource ->
				if (mOverlay != null) {
					val size = cameraSource.previewSize
					val min = Math.min(size.width, size.height)
					val max = Math.max(size.width, size.height)
					if (isPortraitMode) {
						// Swap width and height sizes when in portrait, since it will be rotated by
						// 90 degrees
						mOverlay.setCameraInfo(min, max, cameraSource.cameraFacing)
					} else {
						mOverlay.setCameraInfo(max, min, cameraSource.cameraFacing)
					}
					mOverlay.clear()
				}
			}
			mStartRequested = false
		}
	}

	private inner class SurfaceCallback : SurfaceHolder.Callback {
		override fun surfaceCreated(surface: SurfaceHolder) {
			mSurfaceAvailable = true
			try {
				startIfReady()
			} catch (e: IOException) {
				Log.e(TAG, "Could not start camera source.", e)
			}
		}

		override fun surfaceDestroyed(surface: SurfaceHolder) {
			mSurfaceAvailable = false
		}

		override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		var width = 640
		var height = 480
		mCameraSource?.let { cameraSource ->
			val size = cameraSource.previewSize
			if (size != null) {
				width = size.width
				height = size.height
			}
		}
		// Swap width and height sizes when in portrait, since it will be rotated 90 degrees
		if (isPortraitMode) {
			val tmp = width
			width = height
			height = tmp
		}
		val layoutWidth = right - left

		// Computes height and width for potentially doing fit width.
		val childHeight = (layoutWidth.toFloat() / width.toFloat() * height).toInt()

		for (i in 0 until childCount) {
			getChildAt(i).layout(0, 0, layoutWidth, childHeight)
		}

		try {
			startIfReady()
		} catch (e: IOException) {
			Log.e(TAG, "Could not start camera source.", e)
		}

	}
}