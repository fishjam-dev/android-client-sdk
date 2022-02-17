package org.membraneframework.rtc.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.os.Looper
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import org.webrtc.*
import org.webrtc.RendererCommon.RendererEvents
import java.util.concurrent.CountDownLatch

class VideoTextureViewRenderer : TextureView, SurfaceHolder.Callback, SurfaceTextureListener, VideoSink, RendererEvents {
    // Cached resource name.
    private val resourceName: String
    private val videoLayoutMeasure = RendererCommon.VideoLayoutMeasure()
    private val eglRenderer: SurfaceEglRenderer

    // Callback for reporting renderer events. Read-only after initialization so no lock required.
    private var rendererEvents: RendererEvents? = null

    // Accessed only on the main thread.
    private var rotatedFrameWidth = 0
    private var rotatedFrameHeight = 0
    private var enableFixedSize = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    constructor(context: Context) : super(context) {
        resourceName = getResourceName()
        eglRenderer = SurfaceEglRenderer(resourceName)
        surfaceTextureListener = this
    }

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        resourceName = getResourceName()
        eglRenderer = SurfaceEglRenderer(resourceName)

        surfaceTextureListener = this
    }
    /**
     * Initialize this class, sharing resources with `sharedContext`. The custom `drawer` will be used
     * for drawing frames on the EGLSurface. This class is responsible for calling release() on
     * `drawer`. It is allowed to call init() to reinitialize the renderer after a previous
     * init()/release() cycle.
     */
    /**
     * Initialize this class, sharing resources with `sharedContext`. It is allowed to call init() to
     * reinitialize the renderer after a previous init()/release() cycle.
     */
    @JvmOverloads
    fun init(
        sharedContext: EglBase.Context?,
        rendererEvents: RendererEvents?, configAttributes: IntArray? = EglBase.CONFIG_PLAIN,
        drawer: RendererCommon.GlDrawer? = GlRectDrawer()
    ) {
        ThreadUtils.checkIsOnMainThread()

        this.rendererEvents = rendererEvents
        rotatedFrameWidth = 0
        rotatedFrameHeight = 0
        eglRenderer.init(sharedContext, this /* rendererEvents */, configAttributes, drawer)
    }

    /**
     * Block until any pending frame is returned and all GL resources released, even if an interrupt
     * occurs. If an interrupt occurs during release(), the interrupt flag will be set. This function
     * should be called before the Activity is destroyed and the EGLContext is still valid. If you
     * don't call this function, the GL resources might leak.
     */
    fun release() {
        eglRenderer.release()
    }

    /**
     * Register a callback to be invoked when a new video frame has been received.
     *
     * @param listener The callback to be invoked. The callback will be invoked on the render thread.
     * It should be lightweight and must not call removeFrameListener.
     * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
     * required.
     * @param drawer   Custom drawer to use for this frame listener.
     */
    fun addFrameListener(
        listener: EglRenderer.FrameListener?, scale: Float, drawerParam: RendererCommon.GlDrawer?
    ) {
        eglRenderer.addFrameListener(listener, scale, drawerParam)
    }

    /**
     * Register a callback to be invoked when a new video frame has been received. This version uses
     * the drawer of the EglRenderer that was passed in init.
     *
     * @param listener The callback to be invoked. The callback will be invoked on the render thread.
     * It should be lightweight and must not call removeFrameListener.
     * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
     * required.
     */
    fun addFrameListener(listener: EglRenderer.FrameListener?, scale: Float) {
        eglRenderer.addFrameListener(listener, scale)
    }

    fun removeFrameListener(listener: EglRenderer.FrameListener?) {
        eglRenderer.removeFrameListener(listener)
    }

    /**
     * Enables fixed size for the surface. This provides better performance but might be buggy on some
     * devices. By default this is turned off.
     */
    fun setEnableHardwareScaler(enabled: Boolean) {
        ThreadUtils.checkIsOnMainThread()

        enableFixedSize = enabled
        updateSurfaceSize()
    }

    /**
     * Set if the video stream should be mirrored or not.
     */
    fun setMirror(mirror: Boolean) {
        eglRenderer.setMirror(mirror)
    }

    /**
     * Set how the video will fill the allowed layout area.
     */
    fun setScalingType(scalingType: RendererCommon.ScalingType?) {
        ThreadUtils.checkIsOnMainThread()

        videoLayoutMeasure.setScalingType(scalingType)
        requestLayout()
    }

    fun setScalingType(
        scalingTypeMatchOrientation: RendererCommon.ScalingType?,
        scalingTypeMismatchOrientation: RendererCommon.ScalingType?
    ) {
        ThreadUtils.checkIsOnMainThread()

        videoLayoutMeasure.setScalingType(
            scalingTypeMatchOrientation,
            scalingTypeMismatchOrientation
        )

        requestLayout()
    }

    fun setFpsReduction(fps: Float) {
        eglRenderer.setFpsReduction(fps)
    }

    fun disableFpsReduction() {
        eglRenderer.disableFpsReduction()
    }

    fun pauseVideo() {
        eglRenderer.pauseVideo()
    }

    // VideoSink interface.
    override fun onFrame(frame: VideoFrame) {
        eglRenderer.onFrame(frame)
    }

    // View layout interface.
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        ThreadUtils.checkIsOnMainThread()

        val size =
            videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight)
        setMeasuredDimension(size.x, size.y)

        logD("onMeasure() New size: ${size.x}x${size.y}")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        ThreadUtils.checkIsOnMainThread()

        eglRenderer.setLayoutAspectRatio((right - left) / (bottom - top).toFloat())

        updateSurfaceSize()
    }

    private fun updateSurfaceSize() {
        ThreadUtils.checkIsOnMainThread()

        if (enableFixedSize && rotatedFrameWidth != 0 && rotatedFrameHeight != 0 && width != 0 && height != 0) {
            val layoutAspectRatio = width / height.toFloat()
            val frameAspectRatio = rotatedFrameWidth / rotatedFrameHeight.toFloat()
            val drawnFrameWidth: Int
            val drawnFrameHeight: Int

            if (frameAspectRatio > layoutAspectRatio) {
                drawnFrameWidth = (rotatedFrameHeight * layoutAspectRatio).toInt()
                drawnFrameHeight = rotatedFrameHeight
            } else {
                drawnFrameWidth = rotatedFrameWidth
                drawnFrameHeight = (rotatedFrameWidth / layoutAspectRatio).toInt()
            }

            // Aspect ratio of the drawn frame and the view is the same.
            val width = Math.min(width, drawnFrameWidth)
            val height = Math.min(height, drawnFrameHeight)

            logD(
                "updateSurfaceSize() "
                        + "layout size: ${getWidth()} x ${getHeight()}, "
                        + "frame size: $rotatedFrameWidth x $rotatedFrameHeight, "
                        + "requested surface size: $width x $height, "
                        + "old surface size: $surfaceWidth x $surfaceHeight"
            )

            if (width != surfaceWidth || height != surfaceHeight) {
                surfaceWidth = width
                surfaceHeight = height
                adjustAspectRatio(surfaceWidth, surfaceHeight);
            }
        } else {
            surfaceHeight = 0
            surfaceWidth = surfaceHeight
        }
    }

    /**
     * Sets the TextureView transform to preserve the aspect ratio of the video.
     */
    private fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
        val viewWidth = width
        val viewHeight = height
        val aspectRatio = videoHeight.toDouble() / videoWidth
        val newWidth: Int
        val newHeight: Int

        if (viewHeight > (viewWidth * aspectRatio).toInt()) {
            // limited by narrow width; restrict height
            newWidth = viewWidth
            newHeight = (viewWidth * aspectRatio).toInt()
        } else {
            // limited by short height; restrict width
            newWidth = (viewHeight / aspectRatio).toInt()
            newHeight = viewHeight
        }

        val xoff = (viewWidth - newWidth) / 2
        val yoff = (viewHeight - newHeight) / 2

        logD("video=$videoWidth x $videoHeight view=$viewWidth x $viewHeight"
            + " newView=$newWidth x $newHeight off=$xoff,$yoff"
        )

        val txform = Matrix()
        getTransform(txform)
        txform.setScale(newWidth.toFloat() / viewWidth, newHeight.toFloat() / viewHeight)
        txform.postTranslate(xoff.toFloat(), yoff.toFloat())
        setTransform(txform)
    }

    // SurfaceHolder.Callback interface.
    override fun surfaceCreated(holder: SurfaceHolder) {
        ThreadUtils.checkIsOnMainThread()

        surfaceHeight = 0
        surfaceWidth = surfaceHeight
        updateSurfaceSize()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    // TextureView.SurfaceTextureListener implementation
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, i: Int, i1: Int) {
        ThreadUtils.checkIsOnMainThread()
        eglRenderer.createEglSurface(Surface(surfaceTexture))
        surfaceHeight = 0
        surfaceWidth = surfaceHeight
        updateSurfaceSize()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        ThreadUtils.checkIsOnMainThread()

        logD("surfaceChanged: size: $width x $height")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        ThreadUtils.checkIsOnMainThread()

        val completionLatch = CountDownLatch(1)
        eglRenderer.releaseEglSurface { completionLatch.countDown() }

        ThreadUtils.awaitUninterruptibly(completionLatch)

        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun getResourceName(): String {
        return try {
            resources.getResourceEntryName(id)
        } catch (e: Resources.NotFoundException) {
            ""
        }
    }

    /**
     * Post a task to clear the SurfaceView to a transparent uniform color.
     */
    fun clearImage() {
        eglRenderer.clearImage()
    }

    override fun onFirstFrameRendered() {
        rendererEvents?.onFirstFrameRendered()
    }

    override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
        rendererEvents?.onFrameResolutionChanged(videoWidth, videoHeight, rotation)

        val rotatedWidth = if (rotation == 0 || rotation == 180) videoWidth else videoHeight
        val rotatedHeight = if (rotation == 0 || rotation == 180) videoHeight else videoWidth

        // run immediately if possible for ui thread tests
        postOrRun {
            rotatedFrameWidth = rotatedWidth
            rotatedFrameHeight = rotatedHeight
            updateSurfaceSize()
            requestLayout()
        }
    }

    private fun postOrRun(r: Runnable) {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            r.run()
        } else {
            post(r)
        }
    }

    private fun logD(string: String) {
        Logging.d(TAG, "$resourceName: $string")
    }

    companion object {
        private const val TAG = "SurfaceViewRenderer"
    }
}