package masteringVisualizations;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.beadsproject.beads.core.AudioContext;
import visual.VisualizationView;
import visual.dynamic.described.Stage;

/**
 * JPanel that allows switches between visualizations
 * 
 * @author Isaac Sumner
 *
 */
public class FrequencyVizPanel extends JPanel implements ActionListener
{
	private JComboBox<String> cbox;
	private AudioAnimationStage stage;
	private int width;
	private int height;
	private VisualizationView view;
	
	public FrequencyVizPanel(AudioContext ac, int width, int height)
	{
		super();
		this.width = width;
		this.height = height;
		setLayout(null);
		setBackground(new Color(69,0,132));
		
		// Setup the default Visualization
		stage = new AudioAnimationStage(ac, 1, width, height - 10, 0);
		view = stage.getView();
		view.setBounds(0, 0, width , height - 20);
		add(view);
		stage.start();
		
		// Create the combo box to select the animation
		String[] animations = {"Spectrum", "Stalactite", "Heartbeat"};
		cbox = new JComboBox<String>(animations);
		cbox.addActionListener(this);
		cbox.setBounds(5, height - 20, 125, 20);
		add(cbox);
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		int anim = cbox.getSelectedIndex();
		stage.setAnimationType(anim);
	}
}
