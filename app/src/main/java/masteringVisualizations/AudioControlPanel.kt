package masteringVisualizations

import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Control
import javax.sound.sampled.FloatControl
import javax.sound.sampled.Line
import javax.sound.sampled.LineEvent
import javax.sound.sampled.LineListener
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.Mixer.Info
import javax.sound.sampled.UnsupportedAudioFileException
import javax.swing.AbstractButton
import javax.swing.ButtonModel
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.JToggleButton
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.UIManager
import javax.swing.border.LineBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

import org.jaudiolibs.audioservers.javasound.JavasoundAudioServer
import org.jaudiolibs.beads.AudioServerIO

import visual.statik.sampled.ImageFactory
import io.ResourceFinder
import net.beadsproject.beads.analysis.featureextractors.Power
import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.data.Sample
import net.beadsproject.beads.data.SampleManager
import net.beadsproject.beads.ugens.Clip
import net.beadsproject.beads.ugens.Gain
import net.beadsproject.beads.ugens.Glide
import net.beadsproject.beads.ugens.OnePoleFilter
import net.beadsproject.beads.ugens.RMS
import net.beadsproject.beads.ugens.SamplePlayer

/**
 * The AudioControlPanel builds the controls panel for audio playback and
 * volume control and meter of the audio signal.
 * @author Joey Arbogast
 */

class AudioControlPanel
/**
 * This constructor creates the audio control panel and adds the components to it.
 * @param width        JPanel width
 * @param max_height    JPanel maxium height
 * @param height    Jpanel Height
 */
    (
    private val WIDTH: Int                //Width dimension of the panel
    , private val MAX_HEIGHT: Int, private val HEIGHT: Int                //Height dimension of the panel
) : JPanel(), ActionListener, ChangeListener {
    private val finder: ResourceFinder    //A resource finder for getting audio files and images

    //Color and fonts used in the application
    private val jmuPurple = Color(69, 0, 132)
    private val jmuGold = Color(203, 182, 119)
    private val font: Font
    private val fontVolume: Font
    private val title: Font

    private val currVol: Int = 0
    private val prevVol: Int = 0    //

    //These are all the JComponents used for audio control
    private val volumeSlider: JSlider
    private val volume: JProgressBar
    private val files: JComboBox<String>
    private val playbutton: JToggleButton
    private val pausebutton: JToggleButton
    private val stopbutton: JToggleButton

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

        //Create the fonts
        font = Font("Times New Roman", Font.BOLD, (WIDTH * 0.03).toInt())
        fontVolume = Font("Times New Roman", Font.BOLD, (WIDTH * 0.03).toInt())
        title = Font("Times New Roman", Font.BOLD, (WIDTH * 0.03).toInt())

        //Set the layout, bounds and background color of the JPanel
        setLayout(null)
        setBounds(0, 0, WIDTH, HEIGHT)
        setBackground(jmuPurple)

        //Get a resource finder instance and load the images for the play, pause and stop buttons
        finder = ResourceFinder.createInstance(this)
        val imgFactory = ImageFactory(finder)
        val pIcon = imgFactory.createBufferedImage("/img/playButton.png", 4)
        val pauseIcon = imgFactory.createBufferedImage("/img/pauseButton.png", 4)
        val stopIcon = imgFactory.createBufferedImage("/img/stopButton.png", 4)
        //Creates an icon from the image and scales it according to the screen dimensions
        val play = ImageIcon(pIcon.getScaledInstance((WIDTH * 0.085).toInt(), (WIDTH * 0.085).toInt(), 0))
        val pause = ImageIcon(pauseIcon.getScaledInstance((WIDTH * 0.085).toInt(), (WIDTH * 0.085).toInt(), 0))
        val stop = ImageIcon(stopIcon.getScaledInstance((WIDTH * 0.085).toInt(), (WIDTH * 0.085).toInt(), 0))

        //Construct the RMS level meter (JProgressBar)
        volume = JProgressBar(JProgressBar.VERTICAL, 0, 3000)
        volume.setValue(0)
        volume.setBounds(
            (this.getBounds().getMaxX() - WIDTH * 0.1) as Int - 10, 5, (WIDTH * 0.1).toInt(),
            this.getBounds().getHeight() as Int - 25
        )
        volume.setStringPainted(true)
        volume.setForeground(jmuGold)
        volume.setString("")
        //Create a slider for the volume control
        volumeSlider = JSlider(JSlider.VERTICAL, 0, 10, 5)
        volumeSlider.addChangeListener(this)
        volumeSlider.setMajorTickSpacing(1)
        volumeSlider.setSnapToTicks(true)
        volumeSlider.setPaintTicks(true)
        volumeSlider.setPaintLabels(true)
        volumeSlider.setFont(fontVolume)
        volumeSlider.setBorder(LineBorder(jmuGold, 1, true))
        volumeSlider.setBackground(jmuPurple)
        volumeSlider.setBounds(
            (volume.getBounds().getMinX() - (WIDTH * 0.15).toInt() - 10) as Int,
            5, (WIDTH * 0.15).toInt(), this.getBounds().getHeight() as Int - 25
        )
        volumeSlider.setForeground(jmuGold)

        //Create the file selector drop down box
        files = buildDropDown()
        files.setBounds(10, HEIGHT - 200, 200, 20)

        /* Audio playback buttons */
        playbutton = JToggleButton(play)
        playbutton.setBounds(5, files.getBounds().getMaxY() as Int + 100, 60, 60)
        playbutton.addActionListener(this)

        pausebutton = JToggleButton(pause)
        pausebutton.setBounds(65, files.getBounds().getMaxY() as Int + 100, 60, 60)
        pausebutton.addActionListener(this)


        stopbutton = JToggleButton(stop)
        stopbutton.setBounds(125, files.getBounds().getMaxY() as Int + 100, 60, 60)
        stopbutton.addActionListener(this)

        //Create a label for the volume control
        val volumeLabel = JLabel("Volume", SwingConstants.CENTER)
        val rmsLabel = JLabel("RMS", SwingConstants.CENTER)
        volumeLabel.setFont(font)
        volumeLabel.setForeground(jmuGold)
        volumeLabel.setBounds(
            (volumeSlider.getBounds().getCenterX() - volumeLabel.getWidth() / 2) as Int - 25,
            volumeSlider.getHeight() + 3, (WIDTH * 0.1).toInt(), 20
        )

        rmsLabel.setFont(font)
        rmsLabel.setForeground(jmuGold)
        rmsLabel.setBounds(
            (volume.getBounds().getCenterX() - volume.getWidth() / 2) as Int,
            volume.getHeight() + 3, (WIDTH * 0.1).toInt(), 20
        )


        //Initialize the sample player
        samplePlayerInit()

        //Add all the components to the JPanel
        add(volumeSlider)
        add(volumeLabel)
        add(rmsLabel)
        add(files)
        add(volume)
        add(playbutton)
        add(pausebutton)
        add(stopbutton)

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
