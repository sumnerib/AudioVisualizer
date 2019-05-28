package visual.described

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.shapes.Shape
import android.graphics.drawable.ShapeDrawable
import visual.AbstractTransformableContent
import visual.AffineTransform
import android.graphics.Canvas

class Content(shape: Shape?, private var color: Color?, private var paint: Paint?) : AbstractTransformableContent(), TransformableContent {

    var transformedShape: Shape? = null
    var originalShape: Shape? = null
    lateinit var transformedBounds: Rect
    lateinit var originalBounds: Rect

    init { setShape(shape) }

    constructor(): this(null, null, null)

    override fun getBounds2D(transformed: Boolean): Rect = if (transformed) transformedBounds else originalBounds 

    override fun setLocation(x: Double, y: Double) {
        super.setLocation(x, y)
    }

    override fun setRotation(angle: Double, x: Double, y: Double) {
        super.setRotation(angle, x, y)
    }

    override fun setScale(xScale: Double, yScale: Double) {
        super.setScale(xScale, yScale)
    }

    override fun render(canvas: Canvas) {

        if (isTransformationRequired()) createTransformedContent()

        if (transformedShape != null) {
            paint?.color = color?.toArgb() ?: Color.BLACK
            (transformedShape as Shape).draw(canvas, paint)
        }
    }

    override fun setColor(color: Color) { }

    override fun setPaint(paint: Paint) { }

    private fun createTransformedContent() = createTransformedContent(getAffineTransform())

    private fun createTransformedContent(transform: AffineTransform) {

        transformedShape = transform.createTransformedShape(originalShape)
        this.setTransformationRequired(false)
        this.getBoundsFor(transformedBounds, transformedShape)
    }

    private fun getBoundsFor(rect: Rect, shape: Shape?) {

        var drawable = ShapeDrawable(shape)
        var rect2 = drawable.bounds
        rect.left = rect2.left
        rect.right = rect2.right
        rect.bottom = rect2.bottom
        rect.top = rect2.top
    }

    fun setShape(shape: Shape?) {
        
        originalShape = shape
        transformedBounds = Rect()
        originalBounds = Rect()
        
        if (originalShape != null) 
            if (isTransformationRequired()) createTransformedContent() else transformedShape = shape

        getBoundsFor(originalBounds, originalShape)
        getBoundsFor(transformedBounds, transformedShape)
    }
}