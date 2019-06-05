package masteringVisualizations

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.RelativeLayout
import java.io.IOException

import io.ResourceFinder
import net.beadsproject.beads.analysis.featureextractors.Power
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.data.Sample
import net.beadsproject.beads.ugens.Gain
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

    private val finder: ResourceFinder    //A resource finder for getting audio files and images

    //Colors used in the application
    private val jmuPurple = Color.argb(255, 69, 0, 132)
    private val jmuGold = Color.argb(255, 203, 182, 119)
    private val currVol: Int = 0
    private val prevVol: Int = 0    //

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
        //Get a resource finder instance and load the images for the play, pause and stop buttons
        finder = ResourceFinder.createInstance(this)
        samplePlayerInit()
    }

    /**
     * The samplePlayerInit method initializes all of the sample player
     * items needed for audio playback for Beads.
     */
    fun samplePlayerInit() {
        //Store the selected audio file name
        val audioFile = files.getSelectedItem().toString()
        //Get an inputstream using the resourcefinder and the audio file name
        val sourceStream = finder.findInputStream("/audio/$audioFile")

        var sample: Sample? = null
        try {
            sample = Sample(sourceStream)        //Create a sample from the inputstream

        } catch (e1: IOException) {

            println("could not find $audioFile")
            e1.printStackTrace()
        } catch (e: UnsupportedAudioFileException) {

            e.printStackTrace()
        }


        ac = AudioContext()                //Construct an AudioContext object
        sp = SamplePlayer(ac!!, sample!!)        //Create a sampleplayer from the audio context and the sample as input


        mainGain = Gain(ac, 1, 0.0f)        //Create a gain object for the master gain of the audio context

        mainGain!!.addInput(sp)                //Chain the sample player to the input of the master gain
        ac!!.out.addInput(mainGain)            //Add the gain object as an input to the gain of the audio context
        ac!!.out.gain = 0.06f                    //Set the initial gain of the audio context.
        mainGain!!.setGain(volumeSlider.getValue())        //set mastergain by getting the value from the slider
        sp!!.killOnEnd = false                    //Do not kill the sample after playback gets to end of the sample

        rms = RMS(
            ac,
            2,
            1024
        )                //Create an RootMeanSquare object, with the audio context, channels and memory size
        rms!!.addInput(ac!!.out)                    //Add the audio context as an input to the rms object
        ac!!.out.addDependent(rms)                //Make the rms a dependant of the audio context gain
    }

    /**
     * The buildDropDown method builds the file selector drop down box and returns it.
     * @return  JComboBox The combo box of the audio files list
     */
    fun buildDropDown(): JComboBox<String> {
        val audioFiles =
            arrayOf("veilofshadows.au", "orchpiece_2.au", "underminers-drumloop.au", "veilofshadows-outro.au")

        val fileSelect = JComboBox<String>(audioFiles)
        fileSelect.addActionListener(this)
        return fileSelect

    }


    /**
     * Performs actions based on button clicks
     */
    fun actionPerformed(e: ActionEvent) {
        //Whenever any button is clicked, get the selected audio file in the dropdown box.
        val audioFile = files.getSelectedItem().toString()
        //Get an input stream of the audio sample
        val sourceStream = finder.findInputStream("/audio/$audioFile")


        var sample: Sample? = null
        try {
            sample = Sample(sourceStream)        //Create a sample from the audio file
        } catch (e1: IOException) {
            // TODO Auto-generated catch block
            e1.printStackTrace()
        } catch (e2: UnsupportedAudioFileException) {
            e2.printStackTrace()
        }

        //If the files drop down box has focus, reset all of the audio playback and filters
        if (files.hasFocus()) {
            resetControls()
            sp!!.sample = sample!!
        }

        //Playbutton pressed
        if (e.getSource().equals(playbutton)) {
            //Make sure the audio context is not null and that the audio context is not currently running
            if (ac != null && !ac!!.isRunning) {
                ac!!.start()            //Start the audio context
                //				currVol = volumeSlider.getValue();	//Store the current value of the volume slider
                //				prevVol = currVol;		//Make the previous value of the volume slider the previous value
                updateRMS()            //Start the updateRMS method
                stopbutton.setSelected(false)
                pausebutton.setSelected(false)
            }

        } else if (e.getSource().equals(pausebutton)) {
            ac!!.stop()
            playbutton.setSelected(false)
            stopbutton.setSelected(false)

        } else if (e.getSource().equals(stopbutton)) {
            ac!!.stop()
            sp!!.reset()
            ac!!.reset()
            volume.setValue(0)
            playbutton.setSelected(false)
            pausebutton.setSelected(false)

            //Join the thread back up
            try {
                thread!!.join()
                volume.setValue(0)

            } catch (e1: InterruptedException) {
                // TODO Auto-generated catch block
                e1.printStackTrace()
            }

        }//If the stop button is pressed, stop playback and reset the position to the beginning of the audio sample
        //If the stop button is pressed then stop audio playback
    }

    /**
     * stateChanged listens for changes in the volume slider control and adjust the
     * gain of the audio context.
     */
    fun stateChanged(e: ChangeEvent) {

        if (e.getSource().equals(volumeSlider)) {
            //If the value of the slider is 0 then mute the audio
            if (volumeSlider.getValue() === 0) {
                ac!!.out.gain = 0.0f
            } else {
                ac!!.out.gain = 0.08f
                mainGain!!.setGain(volumeSlider.getValue())

            }//Otherwise set the gain to the value of the volumeSlider
        }

    }

    /**
     * The updateRMS method gets the RMS value of an audio signal
     * and constantly updates the progress bar in a thread running a
     * while loop.
     *
     */
    fun updateRMS() {
        //Construct a thread
        thread = object : Thread() {
            /**
             * The run method executes the RMS audio signal update
             * in a separate thread.
             */
            override fun run() {
                //Loop while the audio context is running
                while (ac!!.isRunning) {
                    //Get the value of the rms and scale the float by 10000
                    val value = rms!!.value * 10000
                    //Update the volume progress bar with the rms value (synchronized access to it)
                    synchronized(this) {
                        volume.setValue(value.toInt())

                    }
                    //If the rms level is above a certain threshold change the color
                    //to let the user know the signal is too loud
                    if (value.toInt() >= 3000) {
                        volume.setForeground(Color.RED)
                    } else {
                        volume.setForeground(jmuGold)
                    }

                }

            }

        }
        thread!!.start()        //Start the thread to update the volume level.


    }

    /**
     * The resetControls method is called when the files drop down has focus,
     * and handles resetting filters and toggles off the playback buttons.
     */
    fun resetControls() {
        ac!!.stop()
        sp!!.reset()

        EQPanel.resetFilters()
        EQPanel.resetPresets()
        volume.setValue(0)
        playbutton.setSelected(false)
        pausebutton.setSelected(false)
        stopbutton.setSelected(false)
    }

    companion object {
        /**
         *
         */
        private val serialVersionUID = 1L
        /**
         * Getter for the mastergain object
         * @return A Gain object
         */
        var mainGain: Gain? = null
            private set            //Gain object for controlling the gain of an audio signal
    }
}
