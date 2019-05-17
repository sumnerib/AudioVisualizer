package masteringVisualizations

import android.widget.FrameLayout
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.content.Context
import android.util.AttributeSet
import android.graphics.Color
import android.view.View
import com.example.audiovisualizer.R

import net.beadsproject.beads.core.AudioContext
import visual.VisualizationView
import visual.described.Stage

/**
 * Layout class for holding the Visualizaiton
 * 
 * @author Isaac Sumner
 * @version 5.16.19
 */
class FrequencyVizLayout@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    
    var spinner: Spinner = findViewById(R.id.viz_spinner)
    private lateinit var stage: AudioAnimationStage
    val purpleInt = Color.argb(255, 69, 0, 132)

    init{
        setBackgroundColor(purpleInt)
        stage.start()
        ArrayAdapter.createFromResource(
            context,
            R.array.viz_names,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    fun initVisualization(audioContext: AudioContext) {
        stage = AudioAnimationStage(audioContext, 1, width, height - 10, 0)
    }

    private fun setupSpinnerListener() {
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { 
                stage.animationType = position
            }
        }
    }
}