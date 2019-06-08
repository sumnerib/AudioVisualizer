package masteringVisualizations

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout

import com.example.audiovisualizer.R
import net.beadsproject.beads.analysis.featureextractors.Power
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.data.Sample
import net.beadsproject.beads.ugens.RMS
import net.beadsproject.beads.ugens.SamplePlayer


/**
 * The AudioControlPanel builds the controls panel for audio playback and
 * volume control and meter of the audio signal.
 * @author Joey Arbogast revised 2019 by Isaac Sumner
 * @version 6.4.19
 */

class AudioControlPanel@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                             defStyleAttr: Int = 0): RelativeLayout(context, attrs, defStyleAttr) {


    private val jmuPurple = Color.argb(255, 69, 0, 132)
    private val jmuGold = Color.argb(255, 203, 182, 119)
    private val currVol: Int = 0
    private val prevVol: Int = 0
    var rmsBar = findViewById<ProgressBar>(R.id.rms_bar)

    //These are from the Beads library
    /**
     * Getter for the AudioContext
     *
     * @return the AudioContext
     */
    var ac: AudioContext? = null
        private set                //An audio context
    /**
     * Getter for the AudioContext
     *
     * @return the AudioContext
     */
    var sp: SamplePlayer? = null
        private set                //The audio player that takes sampled content
    private val power: Power? = null                    //This is used for computing the RMS level
    private var rms: RMS? = null                        //This computes the RMS level of an audio signal

    private var thread: Thread? = null

    init {
        setBackgroundColor(jmuPurple)
        samplePlayerInit()
        initListeners()
    }

    /**
     * The samplePlayerInit method initializes all of the sample player
     * items needed for audio playback for Beads.
     */
    private fun samplePlayerInit() {

        //var mediaPlayer = MediaPlayer.create(context, R.raw.Self_Driving)
        var sample = Sample(resources.openRawResource(R.raw.Self_Driving))

        ac = AudioContext()                //Construct an AudioContext object
        sp = SamplePlayer(ac, sample)        //Create a sampleplayer from the audio context and the sample as input

        sp?.killOnEnd = false                    //Do not kill the sample after playback gets to end of the sample

        rms = RMS(ac, 2,1024)  //Create an RootMeanSquare object, with the audio context, channels and memory size
        rms?.addInput(ac?.out)                    //Add the audio context as an input to the rms object
        ac?.out?.addDependent(rms)                //Make the rms a dependant of the audio context gain
    }

    /**
     * Performs actions based on button clicks
     */
    private fun initListeners() {

        val play = findViewById<View>(R.id.play_button)
        play.setOnClickListener {
            if (!ac!!.isRunning) {
                ac?.start()            //Start the audio context
                updateRMS()            //Start the updateRMS method
            }
        }

        val pause = findViewById<View>(R.id.pause_button)
        pause.setOnClickListener { ac?.stop() }

        val stop = findViewById<View>(R.id.stop_button)
        stop.setOnClickListener {
            ac?.stop()
            sp?.reset()
            ac?.reset()
            rmsBar.progress = 0
            try {
                thread?.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * The updateRMS method gets the RMS value of an audio signal
     * and constantly updates the progress bar in a thread running a
     * while loop.
     *
     */
    private fun updateRMS() {

        thread = object : Thread() {
            override fun run() {

                while (ac!!.isRunning) {
                    //Get the value of the rms and scale the float by 10000
                    val value = rms!!.value * 10000
                    //Update the volume progress bar with the rms value (synchronized access to it)
                    synchronized(this) { rmsBar.progress = value.toInt() }
                    //If the rms level is above a certain threshold change the color
                    //to let the user know the signal is too loud
                    var colorFilter = PorterDuffColorFilter(jmuGold, PorterDuff.Mode.SRC_IN)
                    if (value.toInt() >= 3000)
                        colorFilter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                    rmsBar.progressDrawable.colorFilter = colorFilter
                }
            }
        }

        thread!!.start()        //Start the thread to update the volume level.
    }

    /**
     * The resetControls method is called when the files drop down has focus,
     * and handles resetting filters and toggles off the playback buttons.
     */
//    fun resetControls() {
//        ac!!.stop()
//        sp!!.reset()
//
//        EQPanel.resetFilters()
//        EQPanel.resetPresets()
//        volume.setValue(0)
//        playbutton.setSelected(false)
//        pausebutton.setSelected(false)
//        stopbutton.setSelected(false)
//    }
}
