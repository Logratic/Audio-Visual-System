//***************************************************************************
// Kevin Kim
// Audio Visualizer
//
// Represents the screen on the rgb matrix
//***************************************************************************
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.JApplet;


public class AudioVisualizer extends JApplet implements Runnable {

	private boolean[][] grid;
	private final static int SIZE = 20;
	public static Rainbow rainbow;
	private final static int DIMENSION = 32;
	private ArrayList<Color> colorRow;
	Clip clip;
	long totalTime;
	float[] heights;
	int amplitude = 0;
	int heightFrameLength = 0;
	private final static long WAIT_MICROSECONDS = 40000;
	HashMap<Integer, Long> frequencies;
	private final static boolean volumeBased = false;
	int[] outputHeights;

	//--------------------------------------------------------------
	//sets up the solver and the grid
	//--------------------------------------------------------------
	public void init()
	{
		String songName = "Glow";
		File song = null;
		try
		{
			song = new File(System.getProperty("user.dir") + "\\" + songName + ".wav");
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(song);
			clip = AudioSystem.getClip();
			clip.open(audioInputStream);
			//clip.setMicrosecondPosition(clip.getMicrosecondLength() / 10 * 9);
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			gainControl.setValue(-20.0f); // Reduce volume by 20 decibels.
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
		}

		convertAudioToShort(song);
		
		System.out.println("Length: " + clip.getMicrosecondLength());
		totalTime = (long)clip.getMicrosecondLength() / WAIT_MICROSECONDS; // casting so it never rounds to int early
		System.out.println(totalTime + " " + clip.getMicrosecondLength() + " " + heights.length);
		heightFrameLength = (int)(heights.length/totalTime); // how long each frame is

		if (volumeBased)
		{
			visualizeVolumeBased();
		}
		else
		{
			visualizeFrequencyBased();
		}

		heights = null; // Save Memory
		
		this.setSize(DIMENSION * SIZE + 1, DIMENSION * SIZE + 1); // sets the size of the app
		rainbow = new Rainbow();
		grid = new boolean[DIMENSION][DIMENSION];
		rainbow.size = 20;//WAIT_MICROSECONDS / 1333;// make it much lower when actually animating
		colorRow = new ArrayList<Color>();
		Thread t = new Thread(this);
		Frame c1 = (Frame)this.getParent().getParent();
	    c1.setTitle(songName);
		t.start();
	}

	public void convertAudioToShort(File song)
	{
		byte[] temp = new byte[0];
		short[] shorts = new short[0];
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(song);
			temp = new byte[(int)audioInputStream.available()];
			audioInputStream.read(temp);
			shorts = new short[temp.length/2];
			// to turn bytes to shorts as either big endian or little endian. 
			ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		// maybe keep them stereo and draw them from the middle going outward or outward going in
		long total = 0;
		short[] monoShorts = new short[shorts.length / 2];
		heights = new float[monoShorts.length];
		for (int i = 0; i < shorts.length - 2; i+=2)
		{
			int location = i/2;
			short short1 = shorts[i];
			short short2 = shorts[i + 1];
			if (short1 < 0)
			{
				short1 += 1f; // since negative shorts are larger than positive
				short1 *= -1f;
			}
			if (short2 < 0)
			{
				short2 += 1f;
				short2 *= -1f;
			}
			if (short1 < 0 || short2 < 0)
			{
				System.out.println(short1 + " " + short2);
			}
			heights[location] = (((float)short1 + (float)short2) / 2f);
			if (heights[location] < 0)
			{
				//System.out.println(heights[location] + " " + short1 + " " + short2);
				heights[location] = 32767;
			}
			total += heights[location];
		}

		amplitude = (int) ((total / (long)(monoShorts.length)) / (long)DIMENSION);
		System.out.println(total + " " + monoShorts.length + " " + DIMENSION + " " + amplitude);
	}
	
	public void visualizeVolumeBased()
	{
		outputHeights = new int[(int)totalTime];
		short[] tempHeights = new short[(int) totalTime];
		for (int j = 0; j < totalTime; j++)
		{
			long sum = 0;
			for (int i = 0; i < heightFrameLength; i++)
			{
				sum += heights[(int) (i + j * heightFrameLength)];
			}
			tempHeights[j] = (short) (sum/(short)heightFrameLength/ (amplitude * 4));
		}
		
		for (int i = 0; i < tempHeights.length; i++)
		{
			outputHeights[i] = (int)tempHeights[i];
		}
	}
	
	public void visualizeFrequencyBased()
	{
		float magnify = (float)amplitude * (float)WAIT_MICROSECONDS / 200f;//156.25f;
		int highest = 0;
		float freqWidth = 1;//64/DIMENSION;
		float modifier = 0f;
		outputHeights = new int[(int)(totalTime * DIMENSION)];
		short[] tempHeights = new short[(int)(totalTime * DIMENSION)];
		int prevLocation = 0;
		long sum = 0;
		if (freqWidth <= 2) // impossible to get a freq of 1 or 0
		{
			modifier = 3f - freqWidth;
			System.out.println(modifier);
		}
		
		for (int j = 0; j < totalTime; j++)
		{
			frequencies = new HashMap<Integer, Long>();
			for (int i = 0; i < heightFrameLength; i++)
			{
				int location = (int) (i + j * heightFrameLength);
				float amt = heights[location];
				
				sum += amt;
				if (location > 1 && heights[location - 1] > amt && amt < heights[location + 1])// lowest point
				{
					int freqLength = location - prevLocation;
					prevLocation = location;
				
					if (!frequencies.containsKey(freqLength))
					{
						frequencies.put(freqLength, (long)0);
						if (freqLength > highest)
						{
							System.out.println(freqLength);
							highest = freqLength;
						}
					}
					frequencies.put(freqLength, (long) (frequencies.get(freqLength) + sum));
					sum = 0;
				}
				else if (amt == 0)
				{
					prevLocation = location;
				}
			}

			for (Entry<Integer, Long> entry : frequencies.entrySet())
			{// maybe count how many are at each frequency to find a high occurrence
				Integer key = entry.getKey();
				for (int i = 0; i < DIMENSION; i++)
				{
					if (key >= i * freqWidth && key < (i + 1) * freqWidth)
					{
						long value = entry.getValue();
						/*value /= 4;//(short)(Math.log(value) * 4);
						value = (short) Math.pow(value, (i + 1)/3);*/
						tempHeights[(int)(i + j * DIMENSION - modifier)] += value / magnify;
						break;
					}
				}
			}
		}
		
		for (int i = 0; i < tempHeights.length; i++)
		{
			outputHeights[i] = (int)tempHeights[i];
		}
	}
	
	public void normalizeHeights()
	{
		for (int j = 0; j < totalTime; j++)
		{
			for (int i = 0; i < heightFrameLength; i++)
			{
				int amt = outputHeights[(int) (i + j * heightFrameLength)];
			}
			
		}
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
				if (grid[i][j]) //Represents if the pixel is on or off, remove the comment when ready
				{
					page.fillRect(j * SIZE + 1, i * SIZE + 1, SIZE - 1, SIZE - 1);
				}
			}
		}
		g.drawImage(temp, 0, 0, this);
	}


	// frequencies
	public void updateImageFB()
	{
		for (int i = 0; i < DIMENSION; i++)
		{
			turnPixelOn(i, outputHeights[lastTime * DIMENSION + i]);
		}
	}

	//VB = volume based
	public void updateImageVB()
	{
		for (int i = 0; i < DIMENSION; i++)
		{
			turnPixelOn(i, outputHeights[lastTime + i]);
		}
	}

	//--------------------------------------------------------------
	//Enables that part of the "screen"
	//--------------------------------------------------------------
	public void turnPixelOn(int x, int maxY)
	{
		if (maxY > DIMENSION)
		{
			maxY = DIMENSION;
		}
		for (int y = 0; y < DIMENSION; y++)
		{
			if (y < maxY)
			{
				grid[DIMENSION - 1 - y][x] = true;
			}
			else
			{
				grid[DIMENSION - 1 - y][x] = false;
			}
		}
	}

	int lastTime = -1;
	//--------------------------------------------------------------
	//Loops the paint
	//--------------------------------------------------------------
	public void run() {
		clip.start();
		long sleepTimer = WAIT_MICROSECONDS/1000;
		System.out.println(outputHeights.length + " " + totalTime);
		while (true) {
			try {
				lastTime = (int) (clip.getMicrosecondPosition() / WAIT_MICROSECONDS);
				
				if ((!volumeBased && lastTime * DIMENSION >= outputHeights.length) || (volumeBased && lastTime == outputHeights.length))
				{
					break;
				}
				
				// account for delay from processing the data. Also maybe from slow Java GUI
				if (volumeBased)
				{
					updateImageVB();
				}
				else
				{
					updateImageFB();
				}
				
				Thread.sleep(sleepTimer);//20
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
			repaint();
		}
	}
}
