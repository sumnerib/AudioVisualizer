package visual

import android.graphics.Canvas

interface VisualizationRenderer {

    fun postRendering(canvas: Canvas, model: Visualization, view: VisualizationView)
    fun preRendering(canvas: Canvas, model: Visualization, view: VisualizationView)
    fun render(canvas: Canvas, model: Visualization, view: VisualizationView)
}