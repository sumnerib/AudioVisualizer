package masteringVisualizations

import android.graphics.Paint
import android.graphics.drawable.shapes.RectShape
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Path
import android.graphics.drawable.shapes.PathShape
import net.beadsproject.beads.analysis.featureextractors.FFT
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum
import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter
import net.beadsproject.beads.core.AudioContext

import visual.described.Stage
import visual.described.Content

/**
* The Stage representing the audio animation.
*
* @author Isaac Sumner
* @version V3 5.1.19
*
*/
class AudioAnimationStage(ac: AudioContext, arg0: Int, val width: Int,
				val height: Int, animationType:Int) : Stage(arg0) {
					
	private val ps: PowerSpectrum
	var ac: AudioContext
	var animationType: Int = 0

	init{
		this.ac = ac
		this.animationType = animationType
		val sfs = ShortFrameSegmenter(ac)
		sfs.addInput(ac.out)
		sfs.setChunkSize(2048)
		val fft = FFT()
		sfs.addListener(fft)
		ps = PowerSpectrum()
		fft.addListener(ps)
		ac.out.addDependent(sfs)
	}
	/**
	* Draws the bars after a new tick occurs
	*/
	override fun handleTick(time: Int) {
		clear()
		if (ac.isRunning()) {
			drawSpectrum()
			// when (animationType) {
			// 0 -> drawSpectrum()
			// 1 -> drawStalactite()
			// 2 -> drawHeartbeat()
			// }
		}
	}
	// private Content createLineSegment(double x1, double y1, double x2,
	// 																	double y2, int width)
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
	private fun drawSpectrum() {
		// Get the features
		val features = ps.getFeatures()
		if (features == null) return

		var barWidth = features.size / width
		var leftSide = 0
		for (x in 0 until width)
		{
			// figure out which featureIndex corresponds to this x-
			// position
			val featureIndex = (x * features.size) / width
			// calculate the bar height for this feature
			val barHeight = Math.min(((features[featureIndex] * height)).toInt(), height - 5)
			
			// Create the GradientPaint
			var p = Paint()

			// Use a gradient for higher amplitudes
			if (barHeight >= 190) 
				p.setShader(createSpectrumShader(x, barHeight, Color.RED)) 
			else if (barHeight >= 130) 
				p.setShader(createSpectrumShader(x, barHeight, Color.rgb(255, 165, 0)))
			else if (barHeight >= 70) 
				p.setShader(createSpectrumShader(x, barHeight, Color.YELLOW)) 
			else 
				p.setARGB(255, 0, 255, 0)

			// draw a vertical line corresponding to the frequency
			// represented by this x-position
			add(createThinBar(p, leftSide.toFloat(), height.toFloat(), leftSide.toFloat(),
						(height - barHeight).toFloat(), barWidth.toFloat(), barHeight.toFloat()))
			leftSide += barWidth
		}
	}

	// private void drawDroplets()
	// {
	// 	// Get the features
	// 	float[] features = ps.getFeatures();
	// 	
	// 	if (features != null)
	// 	{
	// 	// Draw the bars
	// 		//int barWidth = width / features.length;
	// 		int barWidth = 1;
	// 		barWidth = features.length / width;
	// 		System.out.println("LENGTH: " + features.length);
	// 		int leftSide = 0;
	// 		for(int x = 0; x < width; x++)
	// 		{
	// 			// figure out which featureIndex corresponds to this x-
	// 			// position
	// 			int featureIndex = (x * features.length) / width;
	// 			
	// 			// calculate the bar height for this feature
	// 			int barHeight = Math.min((int)(features[featureIndex] *
	// 					height), height - 5);
	// 			
	// 			// draw a vertical line corresponding to the frequency
	// 			// represented by this x-position
	// 			add(createLineSegment(leftSide, height, leftSide,
	// 														height - barHeight, barWidth));
	// 			leftSide += barWidth;
	// 		}
	// 	}
	// }
	/**
		* Draws the upside-down spectrum animation
		*/
	// private fun drawStalactite() {
	// 	// Get the features
	// 	val features = ps.getFeatures()
	// 	if (features != null)
	// 	{
	// 		// Draw the bars
	// 		var barWidth = 1
	// 		barWidth = features.size / width
	// 		var leftSide = 0
	// 		var prevX = 0
	// 		var prevY = 0
	// 		for (x in 0 until width)
	// 		{
	// 		// figure out which featureIndex corresponds to this x-
	// 		// position
	// 		val featureIndex = (x * features.size) / width
	// 		// calculate the bar height for this feature
	// 		val barHeight = Math.min(((features[featureIndex] * height)).toInt(), height - 5)
	// 		// draw a vertical line corresponding to the frequency
	// 		// represented by this x-position

	// 		val l = Line2D.Float(prevX + barWidth, prevY,
	// 						leftSide, barHeight - height)
	// 		add(Content(l, Color.YELLOW, null, null))
	// 		prevX = leftSide
	// 		prevY = barHeight
	// 		leftSide += barWidth
	// 		}
	// 	}
	// }
	
	// private fun drawHeartbeat() {
	// 	// Get the features
	// 	val features = ps.getFeatures()
	// 	if (features != null)
	// 	{
	// 		// Draw the bars
	// 		var barWidth = 1
	// 		barWidth = features.size / width
	// 		var leftSide = 0
	// 		var prevX = 0
	// 		var prevY = 0
	// 		for (x in 0 until width)
	// 		{
	// 		// figure out which featureIndex corresponds to this x-
	// 		// position
	// 		val featureIndex = (x * features.size) / width
	// 		// calculate the bar height for this feature
	// 		val barHeight = Math.min(((features[featureIndex] * height)).toInt(), height - 5)
	// 		// draw a vertical line corresponding to the frequency
	// 		// represented by this x-position
	// 		val l:Line2D.Float
	// 		if (x == 0)
	// 		{
	// 			l = Line2D.Float(leftSide, height / 2 - barHeight / 2,
	// 							leftSide + 1, height / 2 - barHeight / 2)
	// 		}
	// 		else
	// 		{
	// 			l = Line2D.Float(prevX, prevY, leftSide, height / 2 - barHeight / 2)
	// 			//l = new Line2D.Float(0, height/2, width, height/2);
	// 		}
	// 		prevY = height / 2 - barHeight / 2
	// 		add(Content(l, Color.GREEN, null, null))
	// 		prevX = leftSide
	// 		leftSide += barWidth
	// 		}
	// 	}
	// }

	/**
	* Creates the background shape for the view
	*
	* @return a rectangle representing the view background
	*/
	private fun createBackground() {
		view.setBackgroundColor(Color.BLACK)
	}

	private fun createSpectrumShader(x: Int, barHeight: Int, endColor: Int): LinearGradient {
		return LinearGradient(
			x.toFloat(), 
			height.toFloat(), 
			x.toFloat(), 
			(height - barHeight).toFloat(), 
			Color.GREEN, 
			endColor,
			Shader.TileMode.MIRROR
		)
	}

	companion object {
		var width:Int = 0
		var height:Int = 0
		/**
			* Makes a thin bar.
			*
			* @param x1
			* @param y1
			* @param x2
			* @param y2
			* @return A content representation of a bar
			*/
		fun createThinBar(p: Paint, x1: Float, y1: Float,
					x2: Float, y2: Float, width: Float, height: Float): Content {
			val path = Path()
			path.moveTo(x1, y1)
			path.lineTo(x2, y2)
			path.lineTo(x2 + width, y2)
			path.lineTo(x2 + width, y1)
			path.lineTo(x1, y1)
			path.close()
			return Content(PathShape(path, width, height), null, p)
		}
	}
}