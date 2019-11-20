//***************************************************************************
// Kevin Kim
// Audio Visualizer
//
// Represents the screen on the rgb matrix
//***************************************************************************

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;

import javax.swing.JApplet;

public class AudioVisualizer extends JApplet implements Runnable {

	private boolean[][] grid;
	private final static int SIZE = 20;
	public static Rainbow rainbow;
	private final static int DIMENSION = 32;
	private ArrayList<Color> colorRow;

	//--------------------------------------------------------------
	//sets up the solver and the grid
	//--------------------------------------------------------------
	public AudioVisualizer()
	{
		this.setSize(DIMENSION * SIZE, DIMENSION * SIZE); // sets the size of the app
		rainbow = new Rainbow();
		grid = new boolean[DIMENSION][DIMENSION];
		rainbow.size = 40;// make it much lower when actually animating
		colorRow = new ArrayList<Color>();
		Thread t = new Thread(this);
		t.start();
	}

	//--------------------------------------------------------------
	//paints the finished grid out
	//--------------------------------------------------------------
	public void paint (Graphics g)
	{
		Image temp = createImage(this.getWidth(), this.getHeight());
		Graphics page = temp.getGraphics();
		page.setColor(Color.black); // background
		page.fillRect(0, 0, this.getWidth(), this.getHeight());
		for (int j = 0; j < grid[0].length; j++) // width
		{
			// Using arraylist so you only need to make 1 new color at a time
			if (j == 0 && colorRow.size() >= DIMENSION) // To save memory
			{
				colorRow.remove(0);
				colorRow.add(rainbow.nextColor());
			}
			else if (colorRow.size() < DIMENSION)
			{
				colorRow.add(rainbow.nextColor());
			}
			page.setColor(colorRow.get(j));
			for (int i = 0; i < grid.length; i++) // height
			{
				//if (grid[i][j]) //Represents if the pixel is on or off, remove the comment when ready
				{
					page.fillRect(j * SIZE + 1, i * SIZE + 1, SIZE - 1, SIZE - 1);
				}
			}
		}
		g.drawImage(temp, 0, 0, this);
	}
	
	//--------------------------------------------------------------
	//Enables that part of the "screen"
	//--------------------------------------------------------------
	public void turnPixelOn(int x, int y)
	{
		grid[x][y] = true;
	}

	//--------------------------------------------------------------
	//Loops the paint
	//--------------------------------------------------------------
	public void run() {
		while (true) {
			try {
				Thread.sleep(20);//20
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			repaint();
		}
	}
}
