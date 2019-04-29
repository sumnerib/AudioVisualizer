package visual

import android.graphics.Rect

abstract class AbstractTransformableContent : TransformableContent {

    protected var angle = 0.0
    protected var xScale = 1.0
    protected var yScale = 1.0
    protected var x = 0.0
    protected var y = 0.0
    protected var xRotation = 0.0
    protected var yRotation = 0.0
    protected var relocated = false
    protected var rerotated = false
    protected var rescaled = false

    override abstract fun getBounds2D(transformed: Boolean): Rect

    fun getBounds2D(): Rect = getBounds2D(true)

    protected fun getAffineTransform(): AffineTransform {

        var transform = AffineTransform()
        if (rerotated) 
            transform.preConcat(AffineTransform.getRotateInstance(angle, xRotation, yRotation)) 

        if (rescaled) 
            transform.preConcat(AffineTransform.getScaleInstance(xScale, yScale))

        if (relocated)
            transform.preConcat(AffineTransform.getTranslateInstance(x, y))

        return transform
    }

    protected fun isTransformationRequired() = rerotated || relocated || rescaled

    override fun setLocation(x: Double, y: Double) {
        this.x = x
        this.y = y
        relocated = true
    }

    override fun setRotation(angle: Double, x: Double, y: Double) {
        this.angle = angle
        xRotation = x
        yRotation = y
        rerotated = true
    }

    override fun setScale(xScale: Double, yScale: Double) {
        this.xScale = xScale
        this.yScale = yScale
        rescaled = true
    }

    fun setScale(scale: Double) = setScale(scale, scale)

    protected fun setTransformationRequired(required: Boolean) {
        relocated = required
        rerotated = required
        rescaled = required
    }
}