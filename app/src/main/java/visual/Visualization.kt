package visual

import java.util.concurrent.CopyOnWriteArrayList
import java.util.LinkedList
import android.graphics.Color

import com.example.audiovisualizer.app.App

open class Visualization {

    protected var content = CopyOnWriteArrayList<SimpleContent>()
    protected var views = LinkedList<VisualizationView>()
    var view: VisualizationView
        get() = views.getFirst()
        set(view) {
            views.removeFirst()
            views.addFirst(view)
        } 
    val viewsIterator = views.iterator()
    val iterator = content.iterator()

    init { views.addFirst(createDefaultView()) }

    protected fun createDefaultView():  VisualizationView = 
        VisualizationView(this, PlainVisualizationRenderer(), App.context)
    
    fun add(r: SimpleContent) {
        if (!content.contains(r)) {
            content.add(r)
            repaint()
        }
    }

    fun repaint() { 
        for (view in views) view.invalidate() 
    }

    fun addView(view: VisualizationView) {
        views.addLast(view)
    }

    fun clear() {
        content.clear()
    }

    fun remove(r: SimpleContent) {
        if (content.remove(r)) repaint()
    }

    fun removeView(view: VisualizationView) {
        views.remove(view)
    }

    fun toBack(r: SimpleContent) {
        if (content.remove(r)) content.add(r)
    }

    fun toFront(r: SimpleContent) {
        if (content.remove(r)) content.add(0, r)
    }
}
