package visual.described

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.shapes.Shape
import android.graphics.drawable.ShapeDrawable
import visual.AbstractTransformableContent
import visual.AffineTransform

class Content(shape: Shape?, color: Color?, paint: Paint?) : AbstractTransformableContent(), TransformableContent {

    lateinit var transformedShape: Shape
    var originalShape: Shape? = null
    lateinit var transformedBounds: Rect
    lateinit var originalBounds: Rect

    init { setShape(shape) }

    constructor: this(null, null, null)

    override fun getBounds2D(ofTransformed: Boolean): Rect {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setLocation(x: Double, y: Double) {
        super.setLocation(x, y)
    }

    override fun setRotation(angle: Double, x: Double, y: Double) {
        super.setRotation(angle, x, y)
    }

    override fun setScale(xScale: Double, yScale: Double) {
        super.setScale(xScale, yScale)
    }

    override fun render() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setColor(color: Color) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setPaint(paint: Paint) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createTransformedContent(transform: AffineTransform) {

        transformedShape = transform.createTransformedShape(originalShape)
        this.setTransformationRequired(false)
        this.getBoundsFor(transformedBounds, transformedShape)
    }

    private fun getBoundsFor(rect: Rect, shape: Shape) {

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
        if (originalShape != null) {
            this.transformationRequired ? createTransformedContent
        }
    }
}