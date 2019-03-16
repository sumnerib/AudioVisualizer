package visual

import android.graphics.Matrix
import android.graphics.drawable.shapes.Shape
import android.graphics.Path
import android.graphics.drawable.shapes.PathShape

class AffineTransform: Matrix() {

    init {
       this.setValues(floatArrayOf(1f, 0f, 0f,
                                   0f, 1f, 0f,
                                   0f, 0f, 1f))
    }

    companion object {
        
        fun getRotateInstance(theta: Double, anchorX: Double, anchorY: Double) : AffineTransform {
            var transform = AffineTransform()
            transform.setRotate(theta.toFloat(), anchorX.toFloat(), anchorY.toFloat())
            return transform
        }

        fun getScaleInstance(sx: Double, sy: Double): AffineTransform {
            var transform = AffineTransform()
            transform.setScale(sx.toFloat(), sy.toFloat())
            return transform
        }

        fun  getTranslateInstance(tx: Double, ty: Double): AffineTransform {
            var transform = AffineTransform()
            transform.setTranslate(tx.toFloat(), ty.toFloat())
            return transform
        }
    }

    fun createTransformedShape(shape: Shape?): Shape {
        var path: Path = Path()
        path.transform(this)
        return PathShape(path, 1.0f, 1.0f)
    }
}