package masteringVisualizations;

import android.graphics.Paint;
import android.graphics.Rect;
import java.util.Arrays;

import net.beadsproject.beads.analysis.featureextractors.FFT;
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum;
import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter;
import net.beadsproject.beads.core.AudioContext;
import visual.dynamic.described.Stage;
import visual.statik.described.Content;


/**
 * The Stage representing the audio animation.
 * 
 * @author Isaac Sumner
 * @version V2 7.23.18
 *
 */
public class AudioAnimationStage extends Stage  
{
	
	public static int VIEW_WIDTH;
	public static int VIEW_HEIGHT;
	
	private PowerSpectrum ps;
	private AudioContext ac;
	private Content bg;
	private int animationType;
	
	public AudioAnimationStage(AudioContext ac, int arg0, int width,
							   int height, int animationType) {

		super(arg0);
		VIEW_WIDTH = width;
		VIEW_HEIGHT = height;
		this.animationType = animationType;
		bg = new Content(createBackground(), null, Color.BLACK, null);
		add(bg);
		
		this.ac = ac;
		
		ShortFrameSegmenter sfs = new ShortFrameSegmenter(ac);
		sfs.addInput(ac.out);
		sfs.setChunkSize(2048);
		FFT fft = new FFT();
		sfs.addListener(fft);
		
		ps = new PowerSpectrum();
		fft.addListener(ps);
		
		ac.out.addDependent(sfs);
	}
	
	/**
	 * Draws the bars after a new tick occurs
	 */
	public void handleTick(int time)
	{
		clear();
		add(bg);
		
		if (ac.isRunning()) 
		{
			switch (animationType) 
			{
				case 0:
					drawSpectrum();
					break;
				case 1:
					drawStalactite();
					break;
				case 2:
					drawHeartbeat();
					break;
			}
		}
	}
  
  /**
   * Makes a thin bar.
   * 
   * @param x1
   * @param y1
   * @param x2
   * @param y2
   * @return A content representation of a bar
   */
  public static Content createThinBar(Paint p, double x1, double y1, 
  								double x2, double y2, int width) {

  	Path2D.Float path = new Path2D.Float();
  	path.moveTo(x1, y1);
  	path.lineTo(x2, y2);
  	path.lineTo(x2 + width, y2);
  	path.lineTo(x2 + width, y1);
  	path.lineTo(x1, y1);
  	path.closePath();
  	
  	return new Content(path, null, p, null);
  }
  
//  private Content createLineSegment(double x1, double y1, double x2, 
//  																	double y2, int width)
//	{
//		Path2D.Float path = new Path2D.Float();
//		path.moveTo(x1, y2);
//		path.lineTo(x1 + width, y2);
//		path.closePath();
//		
//		return new Content(path, Color.YELLOW, Color.YELLOW, null);
//	}
  
  /**
   * Routine to draw the features
   */
  private void drawSpectrum()
  {
  	// Get the features
  	float[] features = ps.getFeatures();
  	
  	if (features != null)
  	{
    	// Draw the bars
  		int barWidth = 1;
  		barWidth = features.length / VIEW_WIDTH;
  		int leftSide = 0;
  		for(int x = 0; x < VIEW_WIDTH; x++)
  		{
  			// figure out which featureIndex corresponds to this x-
  			// position
  			int featureIndex = (x * features.length) / VIEW_WIDTH;
  			
  			// calculate the bar height for this feature
  			int barHeight = Math.min((int)(features[featureIndex] *
  					VIEW_HEIGHT), VIEW_HEIGHT - 5);
  			
  			// Create the GradientPaint
  			Paint p; 
  			
  			if (barHeight >= 190) // Use a gradient for higher amplitudes
  			{
  				p = new GradientPaint(x, VIEW_HEIGHT, Color.GREEN,
  									x, VIEW_HEIGHT - barHeight, Color.RED);
  			}
  			else if (barHeight >= 130)
  			{
  				p = new GradientPaint(x, VIEW_HEIGHT, Color.GREEN,
							x, VIEW_HEIGHT - barHeight, Color.ORANGE);
  			}
  			else if (barHeight >= 70)
  			{
  				p = new GradientPaint(x, VIEW_HEIGHT, Color.GREEN,
							x, VIEW_HEIGHT - barHeight, Color.YELLOW);
  			}
  			else
  				p = Color.GREEN;
  			
  			// draw a vertical line corresponding to the frequency
  			// represented by this x-position
  			add(createThinBar(p, leftSide, VIEW_HEIGHT, leftSide, 
  												VIEW_HEIGHT - barHeight, barWidth));
  			leftSide += barWidth;
  		}
  	}
 
  }
  
//  private void drawDroplets()
//  {
//  	// Get the features
//  	float[] features = ps.getFeatures();
//  	
//  	if (features != null)
//  	{
//    	// Draw the bars
//  		//int barWidth = VIEW_WIDTH / features.length;
//  		int barWidth = 1;
//  		barWidth = features.length / VIEW_WIDTH;
//  		System.out.println("LENGTH: " + features.length);
//  		int leftSide = 0;
//  		for(int x = 0; x < VIEW_WIDTH; x++)
//  		{
//  			// figure out which featureIndex corresponds to this x-
//  			// position
//  			int featureIndex = (x * features.length) / VIEW_WIDTH;
//  			
//  			// calculate the bar height for this feature
//  			int barHeight = Math.min((int)(features[featureIndex] *
//  					VIEW_HEIGHT), VIEW_HEIGHT - 5);
//  			
//  			// draw a vertical line corresponding to the frequency
//  			// represented by this x-position
//  			add(createLineSegment(leftSide, VIEW_HEIGHT, leftSide, 
//  														VIEW_HEIGHT - barHeight, barWidth));
//  			leftSide += barWidth;
//  		}
//  	}
//  }
  
  /**
   * Draws the upside-down spectrum animation
   */
  private void drawStalactite()
  {
  	// Get the features
  	float[] features = ps.getFeatures();
  	
  	if (features != null)
  	{
    	// Draw the bars
  		int barWidth = 1;
  		barWidth = features.length / VIEW_WIDTH;
  		int leftSide = 0;
  		int prevX = 0;
  		int prevY = 0;
  		for(int x = 0; x < VIEW_WIDTH; x++)
  		{
  			// figure out which featureIndex corresponds to this x-
  			// position
  			int featureIndex = (x * features.length) / VIEW_WIDTH;
  			
  			// calculate the bar height for this feature
  			int barHeight = Math.min((int)(features[featureIndex] *
  					VIEW_HEIGHT), VIEW_HEIGHT - 5);
  			
  			// draw a vertical line corresponding to the frequency
  			// represented by this x-position
  			
				Line2D.Float l = new Line2D.Float(prevX + barWidth, prevY, 
															leftSide, barHeight - VIEW_HEIGHT);
				add(new Content(l, Color.YELLOW, null, null));
  	
				prevX = leftSide;
  			prevY = barHeight;
  			leftSide += barWidth;
  		}
  	}
  }
  
  private void drawHeartbeat()
  {
  	// Get the features
  	float[] features = ps.getFeatures();
  	
  	if (features != null)
  	{
    	// Draw the bars
  		int barWidth = 1;
  		barWidth = features.length / VIEW_WIDTH;
  		int leftSide = 0;
  		int prevX = 0;
  		int prevY = 0;
  		for(int x = 0; x < VIEW_WIDTH; x++)
  		{
  			// figure out which featureIndex corresponds to this x-
  			// position
  			int featureIndex = (x * features.length) / VIEW_WIDTH;
  			
  			// calculate the bar height for this feature
  			int barHeight = Math.min((int)(features[featureIndex] *
  					VIEW_HEIGHT), VIEW_HEIGHT - 5);
  			
  			// draw a vertical line corresponding to the frequency
  			// represented by this x-position
  			Line2D.Float l;
  			if (x == 0)
  			{
  				l = new Line2D.Float(leftSide, VIEW_HEIGHT/2 - barHeight/2, 
  						leftSide+1, VIEW_HEIGHT/2 - barHeight/2);
  			}
  			else
  			{
  				l = new Line2D.Float(prevX, prevY, leftSide, VIEW_HEIGHT/2 - barHeight/2);
  				//l = new Line2D.Float(0, VIEW_HEIGHT/2, VIEW_WIDTH, VIEW_HEIGHT/2);
  			}
  			prevY = VIEW_HEIGHT/2 - barHeight/2;
  			
            add(new Content(l, Color.GREEN, null, null));
				
            prevX = leftSide;
  			leftSide += barWidth;
  		}
  	}
  }
  
  /**
   * Creates the background shape for the view
   * 
   * @return a rectangle representing the view background
   */
	private Rect createBackground() {
		return new Rect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
	}
	
	/**
	 * Animation type setter
	 * 
	 * @param animationType
	 */
	public void setAnimationType(int animationType)
	{
		this.animationType = animationType;
	}
	
	/**
	 * Animation type getter
	 * 
	 * @return the animation type
	 */
	public int getAnimationType()
	{
		return animationType;
	}
}
