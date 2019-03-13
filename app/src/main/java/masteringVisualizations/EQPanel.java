package masteringVisualizations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import io.ResourceFinder;
import net.beadsproject.beads.analysis.featureextractors.FFT;
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum;
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks;
import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.ugens.BiquadFilter;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.OnePoleFilter;
import net.beadsproject.beads.ugens.SamplePlayer;
import visual.statik.sampled.ImageFactory;

/**
 * The EQPanel class handles building the EQ panel, controls and listeners
 * @author Joey Arbogast
 *
 */
public class EQPanel extends JPanel implements ActionListener, ChangeListener {
	
	private ResourceFinder finder;
	
	//Color and fonts for the panel
	private Color jmuPurple = new Color(69,0,132);
	private Color jmuGold = new Color(203,182,119);
	private Font font;
	private Font fontVolume;
	private Font title;
	private Font labelsFont;
	
	private Hashtable<Integer, JComponent> labelTable;		//Used for custom labels on the EQ sliders
	
	//Jcomponents for the controls of the EQ panel
	private SpinnerNumberModel filterFreqLPF, filterFreqHPF;
	private JSpinner lpfSpinner, hpfSpinner;
	private JPanel sliderPanel, eqPanel;
	private static JComboBox<String> presetBox;
	
	/**These are static so the reset method can be called in the AudioControlPanel class */
	private static JSlider s250,s800,s25,s8;		//Sliders for each EQ frequency
	private static JToggleButton lpfButton, hpfButton,highShelfButton, lowShelfButton;		//Toggle buttons for the filters
	
	//Used to tell which state the toggle buttons are in
	private static int lpfOn =0;					
	private static int hpfOn =0;
	private static int hShelfOn = 0;
	private static int lShelfOn = 0;
	
	//Gain objects, Glide objects and BiquadFilters for high and low shelfs and low/high pass
	private static Gain lpfGain, hpfGain, gain250, gain800, gain25, gain8, highShelfGain, lowShelfGain;	
	private static Glide lpfGlide, hpfGlide, hshelfGlide, lshelfGlide;
	private static BiquadFilter lowShelf, highShelf, lpf,hpf;
		
	private BiquadFilter peakFilter250, peakFilter800, peakFilter25, peakFilter8;		//The actual peak filters for the EQ


	public AudioContext ac;			//Audio context retrieved from the Audio control panel
	public SamplePlayer sp;			//The sample player from the Audio control panel
	
	private final int WIDTH;		//Width for the JPanel
	private final int HEIGHT;		//Height for the JPanel
	
	
	/**
	 * This constructor builds the EQ Panel
	 * @param ac		//An audio context
	 * @param sp		//A sample player
	 * @param width		//The width for the JPanel	
	 * @param height	//The height for the JPanel
	 */
	public EQPanel(AudioContext ac, SamplePlayer sp, 
								 int width, int height)
	{
		super();
		WIDTH = width;
		HEIGHT = height;
		setBackground(jmuGold);
		setLayout(null);
		setBounds(0,0,WIDTH,HEIGHT/2);
		setBorder(new LineBorder(jmuPurple,5));
		
		//Create the fonts and scale the font size based on the Width
		font = new Font("Times New Roman",Font.BOLD,(int)(WIDTH * .04));
		fontVolume = new Font("Times New Roman",Font.BOLD,(int)(WIDTH *0.04));
		title = new Font("Times New Roman",Font.BOLD,(int)(WIDTH * 0.04));
		labelsFont = new Font("Times New Roman",Font.BOLD,(int)(WIDTH * 0.02 + 2));
		
		//Set the audio context and sample player
		this.ac = ac;
		this.sp = sp;
	
		//Build the preset combo box and EQ slider panel
		buildPresetBox();
		buildEQSliders();
		
		//Initialize all of the filters
		initFilters();
		
		//Add the EQ panel to the main panel
		add(eqPanel);

	}
	
	/**
	 * The buildPresetBox method builds the drop down box for the EQ presets
	 */
	private void buildPresetBox(){
		presetBox = new JComboBox<String>();
		presetBox.setMaximumSize(new Dimension(100,20));
		presetBox.addActionListener(this);
		
		String [] presets = {"no preset", "Rock", "Metal", "Telephone", "Low End", "Brighten"};
		for(int i = 0; i < presets.length; i++){
			presetBox.addItem(presets[i]);
		}
		
	}
	
	/**
	 * The buildEQSliders method builds the panel with the EQ controls
	 */
	public void buildEQSliders()
	{
		//Create a JPanel for the EQ
		eqPanel = new JPanel();
		eqPanel.setBackground(jmuGold);
		eqPanel.setLayout(null);
		eqPanel.setBounds(0,0,WIDTH,HEIGHT);
		eqPanel.setBorder(new LineBorder(jmuPurple,5));
		
		//Set up the label tabel for the EQ sliders
		labelTable = new Hashtable<Integer,JComponent>();
		labelTable.put(new Integer(9), new JLabel("  +9 dB"));
		labelTable.put(new Integer(6), new JLabel("  +6 dB"));
		labelTable.put(new Integer(3), new JLabel("  +3 dB"));
		labelTable.put(new Integer(0), new JLabel("   0 dB"));
		labelTable.put(new Integer(-3), new JLabel("  -3 dB"));
		labelTable.put(new Integer(-6), new JLabel("  -6 dB"));
		labelTable.put(new Integer(-9), new JLabel("  -9 dB"));
		
		//Loop through the label table and set the color and font for each label
		for(int i =-9; i < 10;){
			labelTable.get(i).setFont(labelsFont);
			labelTable.get(i).setForeground(jmuPurple);
			i += 3;
		}
		
		//Load in the EQ image
		finder = ResourceFinder.createInstance(this);
    	ImageFactory imgFactory = new ImageFactory(finder);
    	Image eqIcon= imgFactory.createBufferedImage("/img/eqText.png", 2);
    	
    	//Scale the Icon based on the Width and Height of the Panel
    	Icon eqImg = new ImageIcon(eqIcon.getScaledInstance((int)(WIDTH *0.15),(int)(HEIGHT * 0.15), 0));
		
    	//Create another panel for the slider controls
		sliderPanel = new JPanel();
		sliderPanel.setLayout(null);
		sliderPanel.setBounds(20,20,WIDTH - 40, HEIGHT -40);
		

		//Create labels for the EQ image, and the frequencies for the sliders
		JLabel panelTitle = new JLabel(eqImg);
    	JLabel f250,f800,f25,f8, hertz1,hertz2;
    	
    	f250 = new JLabel("250 Hz");
    	f250.setForeground(jmuPurple);
    	f800 = new JLabel("800 Hz");
    	f800.setForeground(jmuPurple);
    	f25 = new JLabel("2.5 kHz");
    	f25.setForeground(jmuPurple);
    	f8 = new JLabel("8 kHz");
    	f8.setForeground(jmuPurple);
    	hertz1 = new JLabel("Hz");
    	hertz2 = new JLabel("Hz");
    	hertz1.setFont(labelsFont);
    	hertz2.setFont(labelsFont);
    	hertz1.setForeground(jmuPurple);
    	hertz2.setForeground(jmuPurple);
    	
    	//Set the bounds of the hertz labels
    	hertz1.setBounds((int)sliderPanel.getBounds().getWidth()  - 20, 
				30,(int)(WIDTH * 0.15),30);
    	hertz2.setBounds((int)sliderPanel.getBounds().getWidth()  - 20, 
				hertz1.getY() + 30,(int)(WIDTH * 0.15),30);
    	

    	//Create all of the sliders for the EQ control
		s250 = new JSlider(JSlider.VERTICAL,-9,9,0);
		s250.setMajorTickSpacing(3);
		s250.setPaintLabels(true);
		s250.setPaintTicks(true);
		s250.setSnapToTicks(true);
		s250.setLabelTable(labelTable);
		s250.addChangeListener(this);
		s250.setBounds(1,(int)sliderPanel.getBounds().getMinY() + 25,(int)(WIDTH *0.15),
				(int)(sliderPanel.getBounds().getMaxY()/1.5));
		s250.setBorder(new LineBorder(jmuGold,1,true));
		//Set the position of the frequency label for 250Hz
		f250.setBounds(((int)s250.getBounds().getCenterX()- f250.getWidth()/2) - 15, 
				(int)sliderPanel.getHeight() - 16,(int)(WIDTH * 0.08),20);
		f250.setFont(labelsFont);
		/******Create slider and label for 800 hz *************/
		s800 = new JSlider(JSlider.VERTICAL,-9,9,0);
		s800.setMajorTickSpacing(3);
		s800.setPaintLabels(true);
		s800.setPaintTicks(true);
		s800.setSnapToTicks(true);
		s800.addChangeListener(this);
		s800.setForeground(jmuPurple);
		s800.setBounds((int)s250.getBounds().getMaxX() + 1,(int)sliderPanel.getBounds().getMinY() + 25,(int)(WIDTH *0.15),
				(int)(sliderPanel.getBounds().getMaxY()/1.5));
		s800.setLabelTable(labelTable);
		s800.setBorder(new LineBorder(jmuGold,1,true));
		f800.setBounds(((int)s800.getBounds().getCenterX() - f800.getWidth()/2) - 15,
				(int)sliderPanel.getHeight() - 16,(int)(WIDTH * 0.08),20);
		f800.setFont(labelsFont);
		/********Create slider and label for 2.5 kHz ******************/
		s25= new JSlider(JSlider.VERTICAL,-9,9,0);
		s25.setMajorTickSpacing(3);
		s25.setPaintLabels(true);
		s25.setPaintTicks(true);
		s25.setSnapToTicks(true);
		s25.addChangeListener(this);
		s25.setForeground(jmuPurple);
		s25.setBounds((int)s800.getBounds().getMaxX() + 1,
				(int)sliderPanel.getBounds().getMinY() + 25,(int)(WIDTH *0.15),
		(int)(sliderPanel.getBounds().getMaxY()/1.5));
		s25.setLabelTable(labelTable);
		s25.setBorder(new LineBorder(jmuGold,1,true));
		f25.setBounds(((int)s25.getBounds().getCenterX() - f25.getWidth()/2) - 15,
				(int)sliderPanel.getHeight() - 16,(int)(WIDTH * 0.08),20);
		f25.setFont(labelsFont);
		/************** Create slider and label for 8 kHz ******************/
		s8= new JSlider(JSlider.VERTICAL,-9,9,0);
		s8.setMajorTickSpacing(3);
		s8.setPaintLabels(true);
		s8.setPaintTicks(true);
		s8.setSnapToTicks(true);
		s8.addChangeListener(this);
		s8.setForeground(jmuPurple);
		s8.setBounds((int)s25.getBounds().getMaxX() +1,
				(int)sliderPanel.getBounds().getMinY() + 25,(int)(WIDTH *0.15),
				(int)(sliderPanel.getBounds().getMaxY()/1.5));
		s8.setLabelTable(labelTable);
		s8.setBorder(new LineBorder(jmuGold,1,true));
		
		f8.setBounds(((int)s8.getBounds().getCenterX() - f8.getWidth()/2) - 15,
				(int)sliderPanel.getHeight() - 16,(int)(WIDTH * 0.08),20);
		f8.setFont(labelsFont);
		
		//Set the position of the EQ image label
		panelTitle.setBounds(((int)sliderPanel.getWidth()/2 - panelTitle.
				getWidth()/2) - 20, 15, (int)(WIDTH *(.15)), (int)(HEIGHT * (0.15)));
		
		//Create spinners for the Low pass and High pass frequency cuttoffs
		hpfSpinner = new JSpinner();
		lpfSpinner = new JSpinner();
		filterFreqLPF = new SpinnerNumberModel(5000,1200, 20000,100);
		filterFreqHPF = new SpinnerNumberModel(100,50,1000,10);
		//Create a minimum spinner dimension and set the spinner sizes to it
		Dimension minSpinner = new Dimension(55,30);
		lpfSpinner.setMaximumSize(minSpinner);
		
		//Set up all of the attributes for the spinners
		lpfSpinner.setModel(filterFreqLPF);
		lpfSpinner.setBounds( (int)hertz1.getX() - 60, 
				hertz1.getY(),(int)lpfSpinner.getMaximumSize().getWidth(),(int)lpfSpinner.getMaximumSize().getHeight());
		
		hpfSpinner.setModel(filterFreqHPF);
		hpfSpinner.setBounds((int)hertz2.getBounds().getMinX() - 60,
				hertz2.getY(),55,30);
		
		//Create toggle buttons for low and high pass, and low/high shelf
		lpfButton = new JToggleButton("LPF");
		hpfButton = new JToggleButton("HPF");
		lpfButton.addActionListener(this);
		hpfButton.addActionListener(this);
		
		lpfButton.setMaximumSize(new Dimension(60,30));
		hpfButton.setMaximumSize(new Dimension(60,30));
		lpfButton.setBounds((int)lpfSpinner.getBounds().getMinX() - 60 ,
				(int)lpfSpinner.getBounds().getY(),(int)lpfButton.getMaximumSize().getWidth(),
				(int)lpfButton.getMaximumSize().getHeight());
		hpfButton.setBounds((int)hpfSpinner.getBounds().getMinX() - 60,
				(int)hpfSpinner.getBounds().getY(),(int)hpfButton.getMaximumSize().getWidth(),
				(int)hpfButton.getMaximumSize().getHeight());
		lpfButton.setFont(labelsFont);
		hpfButton.setFont(labelsFont);
		lpfButton.setForeground(jmuPurple);
		hpfButton.setForeground(jmuPurple);
		
		
		highShelfButton = new JToggleButton("H-Shelf");
		highShelfButton.setForeground(jmuPurple);
		highShelfButton.setFont(labelsFont);
		lowShelfButton = new JToggleButton("L-Shelf");
		lowShelfButton.setForeground(jmuPurple);
		lowShelfButton.setFont(labelsFont);
		lowShelfButton.addActionListener(this);
		highShelfButton.addActionListener(this);
		
		highShelfButton.setPreferredSize(new Dimension(90,30));
		lowShelfButton.setPreferredSize(new Dimension(90,30));
		highShelfButton.setHorizontalAlignment(SwingConstants.LEFT);
		lowShelfButton.setHorizontalAlignment(SwingConstants.LEFT);
		
		// Set the bounds for buttons and preset box
		highShelfButton.setBounds((int)hpfButton.getBounds().getX(), 
				(int)hpfButton.getBounds().getMaxY() + 10,(int)highShelfButton.getMaximumSize().getWidth(),
				(int)highShelfButton.getMaximumSize().getHeight());
		
		lowShelfButton.setBounds((int)hpfButton.getBounds().getX(), 
				(int)highShelfButton.getBounds().getMaxY() + 2, (int)lowShelfButton.getMaximumSize().getWidth(),
				(int)lowShelfButton.getMaximumSize().getHeight());
		
		presetBox.setBounds((int)lowShelfButton.getBounds().getX(), 
				(int)lowShelfButton.getBounds().getY() + 40, presetBox.getMaximumSize().width,
				presetBox.getMaximumSize().height);
		

		//Add all of the components to the slider panel and then add the slider panel to EQpanel
		sliderPanel.add(s250);
		sliderPanel.add(f250);
		sliderPanel.add(s800);
		sliderPanel.add(f800);
		sliderPanel.add(s25);
		sliderPanel.add(f25);
		sliderPanel.add(s8);
		sliderPanel.add(f8);
		sliderPanel.add(hertz1);
		sliderPanel.add(hertz2);
		sliderPanel.add(lpfButton);
		sliderPanel.add(hpfButton);
		sliderPanel.add(highShelfButton);
		sliderPanel.add(lowShelfButton);
		sliderPanel.add(lpfSpinner);
		sliderPanel.add(hpfSpinner);
		sliderPanel.add(presetBox);
		eqPanel.add(panelTitle);
	
		eqPanel.add(sliderPanel);
	}
	
	/**
	 * The initFilters method intialize all of the Beads filters
	 * used for manipulating frequencies.
	 */
	public void initFilters()
	{
		
		/* Low Pass High pass filters*/
		lpf = new BiquadFilter(ac,2,BiquadFilter.LP);
		hpf = new BiquadFilter(ac,2,BiquadFilter.HP);

		
		//Instantiate Biquad filters for low and high shelf
		lowShelf = new BiquadFilter(ac,2,BiquadFilter.LOW_SHELF);
		lowShelf.setFrequency(60.0f).setQ(2).setGain(5.0f);
		highShelf = new BiquadFilter(ac, 2, BiquadFilter.HIGH_SHELF);
		highShelf.setFrequency(8000.0f).setQ(2).setGain(3.0f);
		
		
		//add the sample player as an input to the filters
		lpf.addInput(sp);
		hpf.addInput(sp);
		lowShelf.addInput(sp);
		highShelf.addInput(sp);
		
		//Create Glide objects and Gains for the filters.  Glides make the transition to a gain value
		//smoother over time, this eliminates pops when you click the button.
		lpfGlide = new Glide(ac, 0.1f, 5);
		hpfGlide = new Glide(ac, 0.1f, 5);
		lpfGain = new Gain(ac, 2, lpfGlide);
		hpfGain = new Gain(ac, 2, hpfGlide);
		//Add the low/high pass filters as inputs to the Glides
		lpfGain.addInput(lpf);
		hpfGain.addInput(hpf);
		
		//Create glids for high/low shelves
		hshelfGlide = new Glide(ac,0.1f,50);
		lshelfGlide = new Glide(ac,0.1f,50);
		lowShelfGain = new Gain(ac, 2, lshelfGlide);
		highShelfGain = new Gain(ac, 2, hshelfGlide);
		
		//Add the shelfs as inputs to the Gains
		lowShelfGain.addInput(lowShelf);
		highShelfGain.addInput(highShelf);
		
	
		//Add the gain objects as inputs the audio contexts Gain.
		ac.out.addInput(lowShelfGain);
		ac.out.addInput(highShelfGain);
		ac.out.addInput(lpfGain);
		ac.out.addInput(hpfGain);
		
	
		
		/******************Peak Filters*************************/
		
		//********************250 Hz **********************
		peakFilter250 = new BiquadFilter(ac, 2, BiquadFilter.PEAKING_EQ);
		peakFilter250.setFrequency(250.0f);
		peakFilter250.setQ(1.0f);		//sets the bandwidth of the filter
		peakFilter250.setGain(0.0f);
		peakFilter250.addInput(sp);	
		//Gain object to control frequency gain
		gain250 = new Gain(ac,2,0.0f);
		gain250.addInput(peakFilter250);
		
		//**************************800 Hz ************************
		peakFilter800 = new BiquadFilter(ac, 2, BiquadFilter.PEAKING_EQ);
		peakFilter800.setFrequency(800.0f);
		peakFilter800.setQ(1.0f);			//The bandwidth
		peakFilter800.setGain(0.0f);
		peakFilter800.addInput(sp);
		//Gain to control frequency gain
		gain800 = new Gain(ac,2,0.0f);
		gain800.addInput(peakFilter800);
		
		//********************2.5kHz ********************************
		peakFilter25 = new BiquadFilter(ac, 2, BiquadFilter.PEAKING_EQ);
		peakFilter25.setFrequency(800.0f);
		peakFilter25.setQ(1.0f);			//Sets the bandwidth of the filter
		peakFilter25.setGain(0.0f);
		peakFilter25.addInput(sp);
		//Gain to control frequency gain
		gain25 = new Gain(ac,2,0.0f);
		gain25.addInput(peakFilter25);
		
		//*************************8 kHz ********************************
		peakFilter8 = new BiquadFilter(ac, 2, BiquadFilter.PEAKING_EQ);
		peakFilter8.setFrequency(800.0f);
		peakFilter8.setQ(1.0f);				//sets the bandwidth of the filter
		peakFilter8.setGain(0.0f);
		peakFilter8.addInput(sp);
		//Gain to control frequency gains
		gain8 = new Gain(ac,2,0.0f);
		gain8.addInput(peakFilter8);
		
		/*** Add the Gains for the frequencies to the audio context output ***/
		ac.out.addInput(gain800);
		ac.out.addInput(gain250);
		ac.out.addInput(gain25);
		ac.out.addInput(gain8);

	}
	
	/*
	 * This method resets all of the filters to their default settings
	 * The method is static so it can be called from the AudioControlPanel
	 * class without instantiating an instance.
	 */
	public static void resetFilters()
	{
		
		lpfGlide.setValue(0.0f);
		hpfGlide.setValue(0.0f);
		lpfOn = 0;
		hpfOn = 0;
		lpfButton.setSelected(false);
		hpfButton.setSelected(false);
		lowShelfButton.setSelected(false);
		highShelfButton.setSelected(false);
		s250.setValue(0);
		s800.setValue(0);
		s25.setValue(0);
		s8.setValue(0);
		gain250.setGain(0.0f);
		gain800.setGain(0.0f);
		gain25.setGain(0.0f);
		gain8.setGain(0.0f);
		lshelfGlide.setValue(0.0f);
		hshelfGlide.setValue(0.0f);
	
	}
	
	/**
	 * This method resets the preset box to the default "no preset"
	 * when a different song is selected in the file selector.  
	 * Called from AudioControlPanel
	 */
	public static void resetPresets(){
		presetBox.setSelectedItem("no preset");
	}
	
	/**
	 * changePresets handles setting the EQ panel to different
	 * presets when selected
	 */
	public void changePresets(){
		String preset = presetBox.getSelectedItem().toString();
		
		if(preset.equals("no preset"))
		{
			/* Only set the "no preset" if the audio context is running
			*  I made this check, because it was setting it to no preset at
			*  startup and would not allow me to change it to any other 
			*  preset 
			*/
			if(ac.isRunning())
			{
				resetFilters();
			}
		}
		//Handle preset for Rock
		else if(preset.equals("Rock")){
			resetFilters();
			s250.setValue(6);
			s800.setValue(-6);
			s25.setValue(6);
			s8.setValue(6);
			
			lpfSpinner.setValue(13000);
			lpfButton.doClick();
			lowShelfButton.doClick();
			
		}
		//Handle preset for Metal
		else if(preset.equals("Metal")){
			resetFilters();
			s250.setValue(6);
			s800.setValue(-9);
			s25.setValue(-3);
			s8.setValue(6);
		
			lpfSpinner.setValue(16000);
			lpfButton.doClick();
			lowShelfButton.doClick();
			highShelfButton.doClick();
		}
		//Handle preset for Telephone
		else if(preset.equals("Telephone")){
			resetFilters();
			s250.setValue(-9);
			s800.setValue(9);
			s25.setValue(-6);
			s8.setValue(-9);
			
			lpfSpinner.setValue(2000);
			lpfButton.doClick();
			hpfSpinner.setValue(50);
			hpfButton.doClick();
		}
		//Handle preset for Low end
		else if(preset.equals("Low End")){
			resetFilters();
			s250.setValue(9);
			s800.setValue(-6);
			s25.setValue(-6);
			s8.setValue(-9);
			lowShelfButton.doClick();
			lpfSpinner.setValue(5000);
			lpfButton.doClick();
		}
		//Handle preset for Brighten
		else{
			resetFilters();
			s250.setValue(-6);
			s800.setValue(-6);
			s25.setValue(3);
			s8.setValue(9);
			hpfSpinner.setValue(300);
			hpfButton.doClick();
			highShelfButton.doClick();
		}
			
		
	}
	
	/*
	 *	This method signals EQ filters to change when a change in the slider is detected 
	 */
	@Override
	public void stateChanged(ChangeEvent e) 
	{
			//Boost gains of freqencies based on the label value x 0.3.
			// This seemed to be the best setting without causing clipping
			if(e.getSource().equals(s250))
			{
				peakFilter250.setGain(s250.getValue());
				gain250.setGain(s250.getValue() * 0.4f);
			}
			else if(e.getSource().equals(s800)){
				peakFilter800.setGain(s800.getValue());
				gain800.setGain(s800.getValue() * 0.4f);
			}
			else if(e.getSource().equals(s25)){
				peakFilter25.setGain(s25.getValue() );
				gain25.setGain(s25.getValue() * 0.4f);
			}
			else if(e.getSource().equals(s8)){
				peakFilter8.setGain(s8.getValue());
				gain8.setGain(s8.getValue() * 0.4f);
			}

	}
	
	/**
	 * This method handles button clicks
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(lpfButton))
		{
			//If the lpf button is currently off
			if(lpfOn == 0)
			{
				//Grab the hertz value from the spinner
				float frequency = filterFreqLPF.getNumber().floatValue();
				//Set the low pass filter frequency to the value from the spinner
				lpf.setFrequency(frequency);
				//Increase the gain of the filter
				lpfGlide.setValue(5.0f);
			
				
				
				//Set toggle on to 1
				lpfOn = 1;
			}
			else
			{
				//Otherwise it's already enabled so disable it by zeroing out everything
				lpf.setFrequency(0.0f);
				
				lpfGlide.setValue(0.0f);
				
				lpfOn = 0;
			}

		}
		else if(e.getSource().equals(hpfButton))
		{
			if(hpfOn == 0)
			{
			
				float frequency = filterFreqHPF.getNumber().floatValue();
				hpf.setFrequency(frequency);
				hpfGlide.setValue(5.0f);
				
			
				
				hpfOn = 1;
			}
			else
			{
				hpf.setFrequency(0.0f);
				
				hpfGlide.setValue(0.0f);
			
			
			
				hpfOn = 0;
			}
		}
		else if(e.getSource().equals(lowShelfButton))
		{
			if(lShelfOn == 0){
				lshelfGlide.setValue(5.0f);
				lShelfOn = 1;
			}
			else{
				lowShelf.setGain(0.0f);
				lshelfGlide.setValue(0.0f);
				lShelfOn = 0;
			}
		}
		else if(e.getSource().equals(highShelfButton))
		{
			if(hShelfOn == 0){
				
				hshelfGlide.setValue(2.0f);
				highShelf.setGain(5.0f);
				hShelfOn = 1;
			}
			else{				
				highShelf.setGain(0.0f);
				hshelfGlide.setValue(0.0f);
				hShelfOn = 0;
			}
		}
		//Calls the changePreset method which handles what to do
		else if(e.getSource().equals(presetBox)){
			changePresets();
		}
	}
}
