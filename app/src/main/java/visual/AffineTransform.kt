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

    fun createTransformedShape(shape: Shape?): Shape {
        var path: Path = Path()
        path.transform(this)
        return PathShape(path, 1.0f, 1.0f)
    } 
}