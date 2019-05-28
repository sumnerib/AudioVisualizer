package masteringVisualizations;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jaudiolibs.audioservers.javasound.JavasoundAudioServer;
import org.jaudiolibs.beads.AudioServerIO;

import visual.statik.sampled.ImageFactory;
import io.ResourceFinder;
import net.beadsproject.beads.analysis.featureextractors.Power;
import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Clip;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.OnePoleFilter;
import net.beadsproject.beads.ugens.RMS;
import net.beadsproject.beads.ugens.SamplePlayer;

/**
 * The AudioControlPanel builds the controls panel for audio playback and
 * volume control and meter of the audio signal.
 * @author Joey Arbogast
 * 
 *
 */

public class AudioControlPanel extends JPanel implements ActionListener,ChangeListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ResourceFinder finder;	//A resource finder for getting audio files and images
	
	//Color and fonts used in the application
	private Color jmuPurple = new Color(69,0,132);
	private Color jmuGold = new Color(203,182,119);
	private Font font, fontVolume, title;

	private int currVol, prevVol;	//
	
	//These are all the JComponents used for audio control
	private JSlider volumeSlider;
	private JProgressBar volume;
	private JComboBox<String> files;
	private JToggleButton playbutton, pausebutton, stopbutton;

	//These are from the Beads library
	private AudioContext ac;				//An audio context
	private SamplePlayer sp;				//The audio player that takes sampled content
	private Power power;					//This is used for computing the RMS level	
	private RMS rms;						//This computes the RMS level of an audio signal
	private static Gain masterGain;			//Gain object for controlling the gain of an audio signal
	
	private final int WIDTH;				//Width dimension of the panel
	private final int HEIGHT;				//Height dimension of the panel
	private final int MAX_HEIGHT;
	
	private Thread thread;
	
	/**
	 * This constructor creates the audio control panel and adds the components to it.
	 * @param width		JPanel width
	 * @param max_height	JPanel maxium height
	 * @param height	Jpanel Height
	 */
	public AudioControlPanel(int width, int max_height, int height){
		super();	
		WIDTH = width;
		HEIGHT = height;
		MAX_HEIGHT = max_height;
		
		//Create the fonts
		font = new Font("Times New Roman",Font.BOLD,(int)(WIDTH * 0.03));
		fontVolume = new Font("Times New Roman",Font.BOLD,(int)(WIDTH * 0.03));
		title = new Font("Times New Roman",Font.BOLD,(int)(WIDTH * 0.03));
		
		//Set the layout, bounds and background color of the JPanel
		setLayout(null);
		setBounds(0,0, WIDTH, HEIGHT);
		setBackground(jmuPurple);
		
		//Get a resource finder instance and load the images for the play, pause and stop buttons
    	finder = ResourceFinder.createInstance(this);
    	ImageFactory imgFactory = new ImageFactory(finder);
    	Image pIcon = imgFactory.createBufferedImage("/img/playButton.png", 4);
    	Image pauseIcon = imgFactory.createBufferedImage("/img/pauseButton.png",4);
    	Image stopIcon = imgFactory.createBufferedImage("/img/stopButton.png",4);
    	//Creates an icon from the image and scales it according to the screen dimensions
    	Icon play = new ImageIcon(pIcon.getScaledInstance((int)(WIDTH * 0.085), (int)(WIDTH * 0.085), 0));
    	Icon pause = new ImageIcon(pauseIcon.getScaledInstance((int)(WIDTH * 0.085), (int)(WIDTH * 0.085), 0));
    	Icon stop = new ImageIcon(stopIcon.getScaledInstance((int)(WIDTH * 0.085), (int)(WIDTH * 0.085), 0));
    	
    	//Construct the RMS level meter (JProgressBar)
    	volume = new JProgressBar(JProgressBar.VERTICAL,0,3000);
    	volume.setValue(0);
    	volume.setBounds((int)(this.getBounds().getMaxX() - (int)WIDTH*0.1) - 10, 5, (int)(WIDTH * 0.1),
    			(int)this.getBounds().getHeight() - 25);
    	volume.setStringPainted(true);
    	volume.setForeground(jmuGold);
    	volume.setString("");
    	//Create a slider for the volume control
    	volumeSlider = new JSlider(JSlider.VERTICAL,0,10,5);
    	volumeSlider.addChangeListener(this);
    	volumeSlider.setMajorTickSpacing(1);
    	volumeSlider.setSnapToTicks(true);
    	volumeSlider.setPaintTicks(true);
    	volumeSlider.setPaintLabels(true);
    	volumeSlider.setFont(fontVolume);
    	volumeSlider.setBorder(new LineBorder(jmuGold,1,true));
    	volumeSlider.setBackground(jmuPurple);
    	volumeSlider.setBounds((int)(volume.getBounds().getMinX() - (int)(WIDTH * 0.15) - 10), 
    			5, (int)(WIDTH * 0.15), (int)this.getBounds().getHeight() - 25);
    	volumeSlider.setForeground(jmuGold);
    	
    	//Create the file selector drop down box
     	files = buildDropDown();
    	files.setBounds(10, HEIGHT - 200, 200, 20);
    	
    	/* Audio playback buttons */
    	playbutton = new JToggleButton(play);
    	playbutton.setBounds(5,(int)files.getBounds().getMaxY() + 100,60,60);
    	playbutton.addActionListener(this);
    	
    	pausebutton = new JToggleButton(pause);
    	pausebutton.setBounds(65,(int)files.getBounds().getMaxY() + 100,60,60);
    	pausebutton.addActionListener(this);
    	
    	
    	stopbutton = new JToggleButton(stop);
    	stopbutton.setBounds(125,(int)files.getBounds().getMaxY() + 100,60,60);
    	stopbutton.addActionListener(this);
		
    	//Create a label for the volume control
    	JLabel volumeLabel = new JLabel("Volume",SwingConstants.CENTER);
    	JLabel rmsLabel = new JLabel("RMS",SwingConstants.CENTER);
    	volumeLabel.setFont(font);
    	volumeLabel.setForeground(jmuGold);
    	volumeLabel.setBounds((int)(volumeSlider.getBounds().getCenterX() - volumeLabel.getWidth()/2) - 25,
    			volumeSlider.getHeight() + 3 ,(int)(WIDTH * 0.1),20);
    	
    	rmsLabel.setFont(font);
    	rmsLabel.setForeground(jmuGold);
    	rmsLabel.setBounds((int)(volume.getBounds().getCenterX() - volume.getWidth()/2), 
    			volume.getHeight() + 3,(int)(WIDTH * 0.1), 20);
    	
    	
    	//Initialize the sample player
    	samplePlayerInit();
    	
    	//Add all the components to the JPanel
    	add(volumeSlider);
    	add(volumeLabel);
    	add(rmsLabel);
    	add(files);
    	add(volume);
    	add(playbutton);
    	add(pausebutton);
    	add(stopbutton);
    
	}
	/**
	 * The samplePlayerInit method initializes all of the sample player
	 * items needed for audio playback for Beads.
	 */
	public void samplePlayerInit(){
		//Store the selected audio file name
		String	audioFile = files.getSelectedItem().toString();
		//Get an inputstream using the resourcefinder and the audio file name
		InputStream sourceStream = finder.findInputStream("/audio/"+audioFile);
		
		Sample sample = null;
		try {
			sample = new Sample(sourceStream);		//Create a sample from the inputstream
		
		} catch (IOException e1) {
	
			System.out.println("could not find " + audioFile );
			e1.printStackTrace();
		} catch (UnsupportedAudioFileException e) {

			e.printStackTrace();
		}
		
		
		ac = new AudioContext();				//Construct an AudioContext object
		sp = new SamplePlayer(ac, sample);		//Create a sampleplayer from the audio context and the sample as input
		

		masterGain = new Gain(ac, 1, 0.0f);		//Create a gain object for the master gain of the audio context
	
		masterGain.addInput(sp);				//Chain the sample player to the input of the master gain
		ac.out.addInput(masterGain);			//Add the gain object as an input to the gain of the audio context
		ac.out.setGain(0.06f);					//Set the initial gain of the audio context.
		masterGain.setGain(volumeSlider.getValue() );		//set mastergain by getting the value from the slider
	    sp.setKillOnEnd(false);					//Do not kill the sample after playback gets to end of the sample
	    
	    rms = new RMS(ac,2,1024);				//Create an RootMeanSquare object, with the audio context, channels and memory size
	    rms.addInput(ac.out);					//Add the audio context as an input to the rms object
	    ac.out.addDependent(rms);				//Make the rms a dependant of the audio context gain
	}
	
	/**
	 * The buildDropDown method builds the file selector drop down box and returns it.
	 * @return  JComboBox The combo box of the audio files list
	 */
	public JComboBox<String> buildDropDown(){
		String audioFiles[] ={"veilofshadows.au","orchpiece_2.au", 
				"underminers-drumloop.au","veilofshadows-outro.au"};

		JComboBox<String> fileSelect = new JComboBox<String>(audioFiles);
		fileSelect.addActionListener(this);
		return fileSelect;
	
	}
	
	
	/**
	 * Performs actions based on button clicks
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		//Whenever any button is clicked, get the selected audio file in the dropdown box.
	    String	audioFile = files.getSelectedItem().toString();
	    //Get an input stream of the audio sample
	    InputStream sourceStream = finder.findInputStream("/audio/"+audioFile);

		
		Sample sample = null;
		try {
			sample = new Sample(sourceStream);		//Create a sample from the audio file
		} catch (IOException e1){
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}catch (UnsupportedAudioFileException e2) {
			e2.printStackTrace();
		}
		
		//If the files drop down box has focus, reset all of the audio playback and filters
		if(files.hasFocus())
		{
			resetControls();
			sp.setSample(sample);
		}
	
		//Playbutton pressed
		if(e.getSource().equals(playbutton))
		{
			//Make sure the audio context is not null and that the audio context is not currently running
			if(ac != null && !ac.isRunning())
			{
				ac.start();			//Start the audio context
//				currVol = volumeSlider.getValue();	//Store the current value of the volume slider
//				prevVol = currVol;		//Make the previous value of the volume slider the previous value
				updateRMS();			//Start the updateRMS method
				stopbutton.setSelected(false);
				pausebutton.setSelected(false);
			}	
			
		}
		//If the stop button is pressed then stop audio playback
		else if(e.getSource().equals(pausebutton))
		{
			ac.stop();
			playbutton.setSelected(false);
			stopbutton.setSelected(false);
			
		}
		//If the stop button is pressed, stop playback and reset the position to the beginning of the audio sample
		else if(e.getSource().equals(stopbutton))
		{
			ac.stop();
			sp.reset();
			ac.reset();
			volume.setValue(0);
			playbutton.setSelected(false);
			pausebutton.setSelected(false);
			
			//Join the thread back up
			try {
				thread.join();
				volume.setValue(0);
			
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * stateChanged listens for changes in the volume slider control and adjust the
	 * gain of the audio context.
	 */
	@Override
	public void stateChanged(ChangeEvent e)
	{
	
		if(e.getSource().equals(volumeSlider))
		{
			//If the value of the slider is 0 then mute the audio
			if(volumeSlider.getValue() == 0){
				ac.out.setGain(0.0f);
			}
			//Otherwise set the gain to the value of the volumeSlider
			else{
				ac.out.setGain(0.08f);
				masterGain.setGain(volumeSlider.getValue());	
				
			}
		}
	
	}

	/**
	 * The updateRMS method gets the RMS value of an audio signal
	 * and constantly updates the progress bar in a thread running a 
	 * while loop.
	 * 
	 */
	public void updateRMS()
	{
			//Construct a thread
		 	thread = new Thread(){
			 	/**
			 	 * The run method executes the RMS audio signal update
			 	 * in a separate thread.
			 	 */
				public void run()
				{
					//Loop while the audio context is running
					while(getAC().isRunning())
					{
						//Get the value of the rms and scale the float by 10000
						float value = rms.getValue() * 10000;
						//Update the volume progress bar with the rms value (synchronized access to it)
						synchronized(this){
							volume.setValue((int)value);
							
						}
						//If the rms level is above a certain threshold change the color
						//to let the user know the signal is too loud
						if((int)value >= 3000){
							volume.setForeground(Color.RED);
						}
						else{
							volume.setForeground(jmuGold);
						}
						
					}
					
				}
			
		};
		thread.start();		//Start the thread to update the volume level.
		
		
	}
	
	/**
	 * The resetControls method is called when the files drop down has focus,
	 * and handles resetting filters and toggles off the playback buttons.
	 */
	public void resetControls()
	{
		ac.stop();
		sp.reset();
	
		EQPanel.resetFilters();
		EQPanel.resetPresets();
		volume.setValue(0);
		playbutton.setSelected(false);
		pausebutton.setSelected(false);
		stopbutton.setSelected(false);
	}
	/**
	 * Getter for the AudioContext
	 * 
	 * @return the AudioContext
	 */
	public AudioContext getAC()
	{
		return ac;
	}
	
	/**
	 * Getter for the AudioContext
	 * 
	 * @return the AudioContext
	 */
	public SamplePlayer getSP()
	{
		return sp;
	}
	/**
	 * Getter for the mastergain object
	 * @return A Gain object
	 */
	public static Gain getMainGain(){
		return masterGain;
	}
}
