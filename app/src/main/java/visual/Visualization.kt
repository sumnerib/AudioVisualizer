package visual

import java.util.concurrent.CopyOnWriteArrayList
import java.util.LinkedList
import com.example.audiovisualizer.app.App

class Visualization {

    protected var content = CopyOnWriteArrayList<SimpleContent>()
    protected var views = LinkedList<VisualizationView>()

    init { views.addFirst(createDefaultView()) }

    protected fun createDefaultView():  VisualizationView = 
        VisualizationView(this, PlainVisualizationRenderer(), App.context)
    
    fun add(r: SimpleContent) {
        if (!content.contains(r)) {
            content.add(r)
            
        }
    }

    fun repaint() {
        for (view in views) view.
    }
}