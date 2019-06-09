package masteringVisualizations

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Image
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.Hashtable

import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.JToggleButton
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants
import javax.swing.border.LineBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

import io.ResourceFinder
import net.beadsproject.beads.analysis.featureextractors.FFT
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks
import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter
import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.ugens.BiquadFilter
import net.beadsproject.beads.ugens.Gain
import net.beadsproject.beads.ugens.Glide
import net.beadsproject.beads.ugens.OnePoleFilter
import net.beadsproject.beads.ugens.SamplePlayer
import visual.statik.sampled.ImageFactory

/**
 * The EQPanel class handles building the EQ panel, controls and listeners
 * @author Joey Arbogast
 */
class EQPanel(
    var ac: AudioContext            //Audio context retrieved from the Audio control panel
    , var sp: SamplePlayer            //The sample player from the Audio control panel
    ,
    private val WIDTH: Int        //Width for the JPanel
    , private val HEIGHT: Int        //Height for the JPanel
) : JPanel(), ActionListener, ChangeListener {

    private var finder: ResourceFinder? = null

    //Color and fonts for the panel
    private val jmuPurple = Color(69, 0, 132)
    private val jmuGold = Color(203, 182, 119)
    private val font: Font
    private val fontVolume: Font
    private val title: Font
    private val labelsFont: Font

    private var labelTable: Hashtable<Int, JComponent>? = null        //Used for custom labels on the EQ sliders

    //Jcomponents for the controls of the EQ panel
    private var filterFreqLPF: SpinnerNumberModel? = null
    private var filterFreqHPF: SpinnerNumberModel? = null
    private var lpfSpinner: JSpinner? = null
    private var hpfSpinner: JSpinner? = null
    private var sliderPanel: JPanel? = null
    private var eqPanel: JPanel? = null

    private var peakFilter250: BiquadFilter? = null
    private var peakFilter800: BiquadFilter? = null
    private var peakFilter25: BiquadFilter? = null
    private var peakFilter8: BiquadFilter? = null        //The actual peak filters for the EQ


    init {
        setBackground(jmuGold)
        setLayout(null)
        setBounds(0, 0, WIDTH, HEIGHT / 2)
        setBorder(LineBorder(jmuPurple, 5))

        //Create the fonts and scale the font size based on the Width
        font = Font("Times New Roman", Font.BOLD, (WIDTH * .04).toInt())
        fontVolume = Font("Times New Roman", Font.BOLD, (WIDTH * 0.04).toInt())
        title = Font("Times New Roman", Font.BOLD, (WIDTH * 0.04).toInt())
        labelsFont = Font("Times New Roman", Font.BOLD, (WIDTH * 0.02 + 2).toInt())

        //Build the preset combo box and EQ slider panel
        buildPresetBox()
        buildEQSliders()

        //Initialize all of the filters
        initFilters()

        //Add the EQ panel to the main panel
        add(eqPanel)

    }//Set the audio context and sample player

    /**
     * The buildPresetBox method builds the drop down box for the EQ presets
     */
    private fun buildPresetBox() {
        presetBox = JComboBox<String>()
        presetBox!!.setMaximumSize(Dimension(100, 20))
        presetBox!!.addActionListener(this)

        val presets = arrayOf("no preset", "Rock", "Metal", "Telephone", "Low End", "Brighten")
        for (i in presets.indices) {
            presetBox!!.addItem(presets[i])
        }

    }

    /**
     * The buildEQSliders method builds the panel with the EQ controls
     */
    fun buildEQSliders() {
        //Create a JPanel for the EQ
        eqPanel = JPanel()
        eqPanel!!.setBackground(jmuGold)
        eqPanel!!.setLayout(null)
        eqPanel!!.setBounds(0, 0, WIDTH, HEIGHT)
        eqPanel!!.setBorder(LineBorder(jmuPurple, 5))

        //Set up the label tabel for the EQ sliders
        labelTable = Hashtable<Int, JComponent>()
        labelTable!![9] = JLabel("  +9 dB")
        labelTable!![6] = JLabel("  +6 dB")
        labelTable!![3] = JLabel("  +3 dB")
        labelTable!![0] = JLabel("   0 dB")
        labelTable!![-3] = JLabel("  -3 dB")
        labelTable!![-6] = JLabel("  -6 dB")
        labelTable!![-9] = JLabel("  -9 dB")

        //Loop through the label table and set the color and font for each label
        var i = -9
        while (i < 10) {
            labelTable!![i].setFont(labelsFont)
            labelTable!![i].setForeground(jmuPurple)
            i += 3
        }

        //Load in the EQ image
        finder = ResourceFinder.createInstance(this)
        val imgFactory = ImageFactory(finder)
        val eqIcon = imgFactory.createBufferedImage("/img/eqText.png", 2)

        //Scale the Icon based on the Width and Height of the Panel
        val eqImg = ImageIcon(eqIcon.getScaledInstance((WIDTH * 0.15).toInt(), (HEIGHT * 0.15).toInt(), 0))

        //Create another panel for the slider controls
        sliderPanel = JPanel()
        sliderPanel!!.setLayout(null)
        sliderPanel!!.setBounds(20, 20, WIDTH - 40, HEIGHT - 40)


        //Create labels for the EQ image, and the frequencies for the sliders
        val panelTitle = JLabel(eqImg)
        val f250: JLabel
        val f800: JLabel
        val f25: JLabel
        val f8: JLabel
        val hertz1: JLabel
        val hertz2: JLabel

        f250 = JLabel("250 Hz")
        f250.setForeground(jmuPurple)
        f800 = JLabel("800 Hz")
        f800.setForeground(jmuPurple)
        f25 = JLabel("2.5 kHz")
        f25.setForeground(jmuPurple)
        f8 = JLabel("8 kHz")
        f8.setForeground(jmuPurple)
        hertz1 = JLabel("Hz")
        hertz2 = JLabel("Hz")
        hertz1.setFont(labelsFont)
        hertz2.setFont(labelsFont)
        hertz1.setForeground(jmuPurple)
        hertz2.setForeground(jmuPurple)

        //Set the bounds of the hertz labels
        hertz1.setBounds(
            sliderPanel!!.getBounds().getWidth() as Int - 20,
            30, (WIDTH * 0.15).toInt(), 30
        )
        hertz2.setBounds(
            sliderPanel!!.getBounds().getWidth() as Int - 20,
            hertz1.getY() + 30, (WIDTH * 0.15).toInt(), 30
        )


        //Create all of the sliders for the EQ control
        s250 = JSlider(JSlider.VERTICAL, -9, 9, 0)
        s250!!.setMajorTickSpacing(3)
        s250!!.setPaintLabels(true)
        s250!!.setPaintTicks(true)
        s250!!.setSnapToTicks(true)
        s250!!.setLabelTable(labelTable)
        s250!!.addChangeListener(this)
        s250!!.setBounds(
            1, sliderPanel!!.getBounds().getMinY() as Int + 25, (WIDTH * 0.15).toInt(),
            (sliderPanel!!.getBounds().getMaxY() / 1.5) as Int
        )
        s250!!.setBorder(LineBorder(jmuGold, 1, true))
        //Set the position of the frequency label for 250Hz
        f250.setBounds(
            s250!!.getBounds().getCenterX() as Int - f250.getWidth() / 2 - 15,
            sliderPanel!!.getHeight() as Int - 16, (WIDTH * 0.08).toInt(), 20
        )
        f250.setFont(labelsFont)
        /******Create slider and label for 800 hz  */
        s800 = JSlider(JSlider.VERTICAL, -9, 9, 0)
        s800!!.setMajorTickSpacing(3)
        s800!!.setPaintLabels(true)
        s800!!.setPaintTicks(true)
        s800!!.setSnapToTicks(true)
        s800!!.addChangeListener(this)
        s800!!.setForeground(jmuPurple)
        s800!!.setBounds(
            s250!!.getBounds().getMaxX() as Int + 1,
            sliderPanel!!.getBounds().getMinY() as Int + 25,
            (WIDTH * 0.15).toInt(),
            (sliderPanel!!.getBounds().getMaxY() / 1.5) as Int
        )
        s800!!.setLabelTable(labelTable)
        s800!!.setBorder(LineBorder(jmuGold, 1, true))
        f800.setBounds(
            s800!!.getBounds().getCenterX() as Int - f800.getWidth() / 2 - 15,
            sliderPanel!!.getHeight() as Int - 16, (WIDTH * 0.08).toInt(), 20
        )
        f800.setFont(labelsFont)
        /********Create slider and label for 2.5 kHz  */
        s25 = JSlider(JSlider.VERTICAL, -9, 9, 0)
        s25!!.setMajorTickSpacing(3)
        s25!!.setPaintLabels(true)
        s25!!.setPaintTicks(true)
        s25!!.setSnapToTicks(true)
        s25!!.addChangeListener(this)
        s25!!.setForeground(jmuPurple)
        s25!!.setBounds(
            s800!!.getBounds().getMaxX() as Int + 1,
            sliderPanel!!.getBounds().getMinY() as Int + 25, (WIDTH * 0.15).toInt(),
            (sliderPanel!!.getBounds().getMaxY() / 1.5) as Int
        )
        s25!!.setLabelTable(labelTable)
        s25!!.setBorder(LineBorder(jmuGold, 1, true))
        f25.setBounds(
            s25!!.getBounds().getCenterX() as Int - f25.getWidth() / 2 - 15,
            sliderPanel!!.getHeight() as Int - 16, (WIDTH * 0.08).toInt(), 20
        )
        f25.setFont(labelsFont)
        /************** Create slider and label for 8 kHz  */
        s8 = JSlider(JSlider.VERTICAL, -9, 9, 0)
        s8!!.setMajorTickSpacing(3)
        s8!!.setPaintLabels(true)
        s8!!.setPaintTicks(true)
        s8!!.setSnapToTicks(true)
        s8!!.addChangeListener(this)
        s8!!.setForeground(jmuPurple)
        s8!!.setBounds(
            s25!!.getBounds().getMaxX() as Int + 1,
            sliderPanel!!.getBounds().getMinY() as Int + 25, (WIDTH * 0.15).toInt(),
            (sliderPanel!!.getBounds().getMaxY() / 1.5) as Int
        )
        s8!!.setLabelTable(labelTable)
        s8!!.setBorder(LineBorder(jmuGold, 1, true))

        f8.setBounds(
            s8!!.getBounds().getCenterX() as Int - f8.getWidth() / 2 - 15,
            sliderPanel!!.getHeight() as Int - 16, (WIDTH * 0.08).toInt(), 20
        )
        f8.setFont(labelsFont)

        //Set the position of the EQ image label
        panelTitle.setBounds(
            sliderPanel!!.getWidth() as Int / 2 - panelTitle.getWidth() / 2 - 20,
            15,
            (WIDTH * .15).toInt(),
            (HEIGHT * 0.15).toInt()
        )

        //Create spinners for the Low pass and High pass frequency cuttoffs
        hpfSpinner = JSpinner()
        lpfSpinner = JSpinner()
        filterFreqLPF = SpinnerNumberModel(5000, 1200, 20000, 100)
        filterFreqHPF = SpinnerNumberModel(100, 50, 1000, 10)
        //Create a minimum spinner dimension and set the spinner sizes to it
        val minSpinner = Dimension(55, 30)
        lpfSpinner!!.setMaximumSize(minSpinner)

        //Set up all of the attributes for the spinners
        lpfSpinner!!.setModel(filterFreqLPF)
        lpfSpinner!!.setBounds(
            hertz1.getX() as Int - 60,
            hertz1.getY(),
            lpfSpinner!!.getMaximumSize().getWidth() as Int,
            lpfSpinner!!.getMaximumSize().getHeight() as Int
        )

        hpfSpinner!!.setModel(filterFreqHPF)
        hpfSpinner!!.setBounds(
            hertz2.getBounds().getMinX() as Int - 60,
            hertz2.getY(), 55, 30
        )

        //Create toggle buttons for low and high pass, and low/high shelf
        lpfButton = JToggleButton("LPF")
        hpfButton = JToggleButton("HPF")
        lpfButton!!.addActionListener(this)
        hpfButton!!.addActionListener(this)

        lpfButton!!.setMaximumSize(Dimension(60, 30))
        hpfButton!!.setMaximumSize(Dimension(60, 30))
        lpfButton!!.setBounds(
            lpfSpinner!!.getBounds().getMinX() as Int - 60,
            lpfSpinner!!.getBounds().getY() as Int, lpfButton!!.getMaximumSize().getWidth() as Int,
            lpfButton!!.getMaximumSize().getHeight() as Int
        )
        hpfButton!!.setBounds(
            hpfSpinner!!.getBounds().getMinX() as Int - 60,
            hpfSpinner!!.getBounds().getY() as Int, hpfButton!!.getMaximumSize().getWidth() as Int,
            hpfButton!!.getMaximumSize().getHeight() as Int
        )
        lpfButton!!.setFont(labelsFont)
        hpfButton!!.setFont(labelsFont)
        lpfButton!!.setForeground(jmuPurple)
        hpfButton!!.setForeground(jmuPurple)


        highShelfButton = JToggleButton("H-Shelf")
        highShelfButton!!.setForeground(jmuPurple)
        highShelfButton!!.setFont(labelsFont)
        lowShelfButton = JToggleButton("L-Shelf")
        lowShelfButton!!.setForeground(jmuPurple)
        lowShelfButton!!.setFont(labelsFont)
        lowShelfButton!!.addActionListener(this)
        highShelfButton!!.addActionListener(this)

        highShelfButton!!.setPreferredSize(Dimension(90, 30))
        lowShelfButton!!.setPreferredSize(Dimension(90, 30))
        highShelfButton!!.setHorizontalAlignment(SwingConstants.LEFT)
        lowShelfButton!!.setHorizontalAlignment(SwingConstants.LEFT)

        // Set the bounds for buttons and preset box
        highShelfButton!!.setBounds(
            hpfButton!!.getBounds().getX() as Int,
            hpfButton!!.getBounds().getMaxY() as Int + 10, highShelfButton!!.getMaximumSize().getWidth() as Int,
            highShelfButton!!.getMaximumSize().getHeight() as Int
        )

        lowShelfButton!!.setBounds(
            hpfButton!!.getBounds().getX() as Int,
            highShelfButton!!.getBounds().getMaxY() as Int + 2, lowShelfButton!!.getMaximumSize().getWidth() as Int,
            lowShelfButton!!.getMaximumSize().getHeight() as Int
        )

        presetBox!!.setBounds(
            lowShelfButton!!.getBounds().getX() as Int,
            lowShelfButton!!.getBounds().getY() as Int + 40, presetBox!!.getMaximumSize().width,
            presetBox!!.getMaximumSize().height
        )


        //Add all of the components to the slider panel and then add the slider panel to EQpanel
        sliderPanel!!.add(s250)
        sliderPanel!!.add(f250)
        sliderPanel!!.add(s800)
        sliderPanel!!.add(f800)
        sliderPanel!!.add(s25)
        sliderPanel!!.add(f25)
        sliderPanel!!.add(s8)
        sliderPanel!!.add(f8)
        sliderPanel!!.add(hertz1)
        sliderPanel!!.add(hertz2)
        sliderPanel!!.add(lpfButton)
        sliderPanel!!.add(hpfButton)
        sliderPanel!!.add(highShelfButton)
        sliderPanel!!.add(lowShelfButton)
        sliderPanel!!.add(lpfSpinner)
        sliderPanel!!.add(hpfSpinner)
        sliderPanel!!.add(presetBox)
        eqPanel!!.add(panelTitle)

        eqPanel!!.add(sliderPanel)
    }

    /**
     * The initFilters method intialize all of the Beads filters
     * used for manipulating frequencies.
     */
    fun initFilters() {

        /* Low Pass High pass filters*/
        lpf = BiquadFilter(ac, 2, BiquadFilter.LP)
        hpf = BiquadFilter(ac, 2, BiquadFilter.HP)


        //Instantiate Biquad filters for low and high shelf
        lowShelf = BiquadFilter(ac, 2, BiquadFilter.LOW_SHELF)
        lowShelf!!.setFrequency(60.0f).setQ(2f).gain = 5.0f
        highShelf = BiquadFilter(ac, 2, BiquadFilter.HIGH_SHELF)
        highShelf!!.setFrequency(8000.0f).setQ(2f).gain = 3.0f


        //add the sample player as an input to the filters
        lpf!!.addInput(sp)
        hpf!!.addInput(sp)
        lowShelf!!.addInput(sp)
        highShelf!!.addInput(sp)

        //Create Glide objects and Gains for the filters.  Glides make the transition to a gain value
        //smoother over time, this eliminates pops when you click the button.
        lpfGlide = Glide(ac, 0.1f, 5f)
        hpfGlide = Glide(ac, 0.1f, 5f)
        lpfGain = Gain(ac, 2, lpfGlide)
        hpfGain = Gain(ac, 2, hpfGlide)
        //Add the low/high pass filters as inputs to the Glides
        lpfGain!!.addInput(lpf)
        hpfGain!!.addInput(hpf)

        //Create glids for high/low shelves
        hshelfGlide = Glide(ac, 0.1f, 50f)
        lshelfGlide = Glide(ac, 0.1f, 50f)
        lowShelfGain = Gain(ac, 2, lshelfGlide)
        highShelfGain = Gain(ac, 2, hshelfGlide)

        //Add the shelfs as inputs to the Gains
        lowShelfGain!!.addInput(lowShelf)
        highShelfGain!!.addInput(highShelf)


        //Add the gain objects as inputs the audio contexts Gain.
        ac.out.addInput(lowShelfGain)
        ac.out.addInput(highShelfGain)
        ac.out.addInput(lpfGain)
        ac.out.addInput(hpfGain)


        /******************Peak Filters */

        //********************250 Hz **********************
        peakFilter250 = BiquadFilter(ac, 2, BiquadFilter.PEAKING_EQ)
        peakFilter250!!.frequency = 250.0f
        peakFilter250!!.q = 1.0f        //sets the bandwidth of the filter
        peakFilter250!!.gain = 0.0f
        peakFilter250!!.addInput(sp)
        //Gain object to control frequency gain
        gain250 = Gain(ac, 2, 0.0f)
        gain250!!.addInput(peakFilter250)

        //**************************800 Hz ************************
        peakFilter800 = BiquadFilter(ac, 2, BiquadFilter.PEAKING_EQ)
        peakFilter800!!.frequency = 800.0f
        peakFilter800!!.q = 1.0f            //The bandwidth
        peakFilter800!!.gain = 0.0f
        peakFilter800!!.addInput(sp)
        //Gain to control frequency gain
        gain800 = Gain(ac, 2, 0.0f)
        gain800!!.addInput(peakFilter800)

        //********************2.5kHz ********************************
        peakFilter25 = BiquadFilter(ac, 2, BiquadFilter.PEAKING_EQ)
        peakFilter25!!.frequency = 800.0f
        peakFilter25!!.q = 1.0f            //Sets the bandwidth of the filter
        peakFilter25!!.gain = 0.0f
        peakFilter25!!.addInput(sp)
        //Gain to control frequency gain
        gain25 = Gain(ac, 2, 0.0f)
        gain25!!.addInput(peakFilter25)

        //*************************8 kHz ********************************
        peakFilter8 = BiquadFilter(ac, 2, BiquadFilter.PEAKING_EQ)
        peakFilter8!!.frequency = 800.0f
        peakFilter8!!.q = 1.0f                //sets the bandwidth of the filter
        peakFilter8!!.gain = 0.0f
        peakFilter8!!.addInput(sp)
        //Gain to control frequency gains
        gain8 = Gain(ac, 2, 0.0f)
        gain8!!.addInput(peakFilter8)

        /*** Add the Gains for the frequencies to the audio context output  */
        ac.out.addInput(gain800)
        ac.out.addInput(gain250)
        ac.out.addInput(gain25)
        ac.out.addInput(gain8)

    }

    /**
     * changePresets handles setting the EQ panel to different
     * presets when selected
     */
    fun changePresets() {
        val preset = presetBox!!.getSelectedItem().toString()

        if (preset == "no preset") {
            /* Only set the "no preset" if the audio context is running
			*  I made this check, because it was setting it to no preset at
			*  startup and would not allow me to change it to any other
			*  preset
			*/
            if (ac.isRunning) {
                resetFilters()
            }
        } else if (preset == "Rock") {
            resetFilters()
            s250!!.setValue(6)
            s800!!.setValue(-6)
            s25!!.setValue(6)
            s8!!.setValue(6)

            lpfSpinner!!.setValue(13000)
            lpfButton!!.doClick()
            lowShelfButton!!.doClick()

        } else if (preset == "Metal") {
            resetFilters()
            s250!!.setValue(6)
            s800!!.setValue(-9)
            s25!!.setValue(-3)
            s8!!.setValue(6)

            lpfSpinner!!.setValue(16000)
            lpfButton!!.doClick()
            lowShelfButton!!.doClick()
            highShelfButton!!.doClick()
        } else if (preset == "Telephone") {
            resetFilters()
            s250!!.setValue(-9)
            s800!!.setValue(9)
            s25!!.setValue(-6)
            s8!!.setValue(-9)

            lpfSpinner!!.setValue(2000)
            lpfButton!!.doClick()
            hpfSpinner!!.setValue(50)
            hpfButton!!.doClick()
        } else if (preset == "Low End") {
            resetFilters()
            s250!!.setValue(9)
            s800!!.setValue(-6)
            s25!!.setValue(-6)
            s8!!.setValue(-9)
            lowShelfButton!!.doClick()
            lpfSpinner!!.setValue(5000)
            lpfButton!!.doClick()
        } else {
            resetFilters()
            s250!!.setValue(-6)
            s800!!.setValue(-6)
            s25!!.setValue(3)
            s8!!.setValue(9)
            hpfSpinner!!.setValue(300)
            hpfButton!!.doClick()
            highShelfButton!!.doClick()
        }//Handle preset for Brighten
        //Handle preset for Low end
        //Handle preset for Telephone
        //Handle preset for Metal
        //Handle preset for Rock


    }

    /*
	 *	This method signals EQ filters to change when a change in the slider is detected
	 */
    fun stateChanged(e: ChangeEvent) {
        //Boost gains of freqencies based on the label value x 0.3.
        // This seemed to be the best setting without causing clipping
        if (e.getSource().equals(s250)) {
            peakFilter250!!.setGain(s250!!.getValue())
            gain250!!.setGain(s250!!.getValue() * 0.4f)
        } else if (e.getSource().equals(s800)) {
            peakFilter800!!.setGain(s800!!.getValue())
            gain800!!.setGain(s800!!.getValue() * 0.4f)
        } else if (e.getSource().equals(s25)) {
            peakFilter25!!.setGain(s25!!.getValue())
            gain25!!.setGain(s25!!.getValue() * 0.4f)
        } else if (e.getSource().equals(s8)) {
            peakFilter8!!.setGain(s8!!.getValue())
            gain8!!.setGain(s8!!.getValue() * 0.4f)
        }

    }

    /**
     * This method handles button clicks
     */
    fun actionPerformed(e: ActionEvent) {
        if (e.getSource().equals(lpfButton)) {
            //If the lpf button is currently off
            if (lpfOn == 0) {
                //Grab the hertz value from the spinner
                val frequency = filterFreqLPF!!.getNumber().floatValue()
                //Set the low pass filter frequency to the value from the spinner
                lpf!!.setFrequency(frequency)
                //Increase the gain of the filter
                lpfGlide!!.value = 5.0f


                //Set toggle on to 1
                lpfOn = 1
            } else {
                //Otherwise it's already enabled so disable it by zeroing out everything
                lpf!!.frequency = 0.0f

                lpfGlide!!.value = 0.0f

                lpfOn = 0
            }

        } else if (e.getSource().equals(hpfButton)) {
            if (hpfOn == 0) {

                val frequency = filterFreqHPF!!.getNumber().floatValue()
                hpf!!.setFrequency(frequency)
                hpfGlide!!.value = 5.0f



                hpfOn = 1
            } else {
                hpf!!.frequency = 0.0f

                hpfGlide!!.value = 0.0f



                hpfOn = 0
            }
        } else if (e.getSource().equals(lowShelfButton)) {
            if (lShelfOn == 0) {
                lshelfGlide!!.value = 5.0f
                lShelfOn = 1
            } else {
                lowShelf!!.gain = 0.0f
                lshelfGlide!!.value = 0.0f
                lShelfOn = 0
            }
        } else if (e.getSource().equals(highShelfButton)) {
            if (hShelfOn == 0) {

                hshelfGlide!!.value = 2.0f
                highShelf!!.gain = 5.0f
                hShelfOn = 1
            } else {
                highShelf!!.gain = 0.0f
                hshelfGlide!!.value = 0.0f
                hShelfOn = 0
            }
        } else if (e.getSource().equals(presetBox)) {
            changePresets()
        }//Calls the changePreset method which handles what to do
    }

    companion object {
        private var presetBox: JComboBox<String>? = null

        /**These are static so the reset method can be called in the AudioControlPanel class  */
        private var s250: JSlider? = null
        private var s800: JSlider? = null
        private var s25: JSlider? = null
        private var s8: JSlider? = null        //Sliders for each EQ frequency
        private var lpfButton: JToggleButton? = null
        private var hpfButton: JToggleButton? = null
        private var highShelfButton: JToggleButton? = null
        private var lowShelfButton: JToggleButton? = null        //Toggle buttons for the filters

        //Used to tell which state the toggle buttons are in
        private var lpfOn = 0
        private var hpfOn = 0
        private var hShelfOn = 0
        private var lShelfOn = 0

        //Gain objects, Glide objects and BiquadFilters for high and low shelfs and low/high pass
        private var lpfGain: Gain? = null
        private var hpfGain: Gain? = null
        private var gain250: Gain? = null
        private var gain800: Gain? = null
        private var gain25: Gain? = null
        private var gain8: Gain? = null
        private var highShelfGain: Gain? = null
        private var lowShelfGain: Gain? = null
        private var lpfGlide: Glide? = null
        private var hpfGlide: Glide? = null
        private var hshelfGlide: Glide? = null
        private var lshelfGlide: Glide? = null
        private var lowShelf: BiquadFilter? = null
        private var highShelf: BiquadFilter? = null
        private var lpf: BiquadFilter? = null
        private var hpf: BiquadFilter? = null

        /*
	 * This method resets all of the filters to their default settings
	 * The method is static so it can be called from the AudioControlPanel
	 * class without instantiating an instance.
	 */
        fun resetFilters() {

            lpfGlide!!.value = 0.0f
            hpfGlide!!.value = 0.0f
            lpfOn = 0
            hpfOn = 0
            lpfButton!!.setSelected(false)
            hpfButton!!.setSelected(false)
            lowShelfButton!!.setSelected(false)
            highShelfButton!!.setSelected(false)
            s250!!.setValue(0)
            s800!!.setValue(0)
            s25!!.setValue(0)
            s8!!.setValue(0)
            gain250!!.gain = 0.0f
            gain800!!.gain = 0.0f
            gain25!!.gain = 0.0f
            gain8!!.gain = 0.0f
            lshelfGlide!!.value = 0.0f
            hshelfGlide!!.value = 0.0f

        }

        /**
         * This method resets the preset box to the default "no preset"
         * when a different song is selected in the file selector.
         * Called from AudioControlPanel
         */
        fun resetPresets() {
            presetBox!!.setSelectedItem("no preset")
        }
    }
}
