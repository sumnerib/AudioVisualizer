package visual

import android.graphics.Rect

interface TransformableContent : SimpleContent {

    fun getBounds2D(ofTransformed: Boolean): Rect
    fun setLocation(x: Double, y: Double)
    fun setRotation(angle: Double, x: Double, y: Double)
    fun setScale(xScale: Double, yScale: Double)
}