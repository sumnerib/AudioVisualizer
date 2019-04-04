package visual

import android.view.View
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas

class VisualizationView(
    model: Visualization, 
    renderer: VisualizationRenderer, 
    context: Context) : View(context) {

    var useDoubleBuffering: Boolean = true
    protected lateinit var bg: Canvas
    protected lateinit var offScreenImage: Bitmap
    protected var model: Visualization = model
    var renderer: VisualizationRenderer = renderer

    init {
        super.setMinimumHeight(-1)
        super.setMinimumWidth(-1)
    }

    private fun createOffscreenBuffer(): Canvas {
        offScreenImage = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        bg = Canvas(offScreenImage)
        bg.clipRect(this.height, this.width, this.height, this.width)
        return bg
    }

    fun paint(canvas: Canvas) {

        var bufferCanvas = if (useDoubleBuffering) createOffscreenBuffer() else canvas

        preRendering(bufferCanvas)
        render(bufferCanvas)
        postRendering(bufferCanvas)

        if (useDoubleBuffering) {
            canvas.drawBitmap(offScreenImage, this.left.toFloat(), this.top.toFloat(), null)
            bg.clipRect(this.height, this.width, this.height, this.width)
        }
    }

    protected fun preRendering(c: Canvas) { renderer.render(c, model, this) }

    protected fun render(c: Canvas) { renderer.render(c, model, this) }

    protected fun postRendering(c: Canvas) { renderer.render(c, model, this) }

    fun update(c: Canvas) { paint(c) }
}