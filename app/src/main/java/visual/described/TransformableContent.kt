package visual.described

import android.graphics.Color
import android.graphics.Paint

interface TransformableContent : visual.TransformableContent {

    fun setColor(color: Color)
    fun setPaint(paint: Paint)
}