//***************************************************************************
// Kevin Kim
// Audio Visualizer
//
// Represents the screen on the rgb matrix
//***************************************************************************
import java.awt.Color;
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
	short[] heights;
	int amplitude = 0;
	int heightFrameLength = 0;
	private final static long WAIT_MICROSECONDS = 40000;
	HashMap<Integer, Short> frequencies;
	private final static boolean volumeBased = false;
	int[] outputHeights;

	//--------------------------------------------------------------
	//sets up the solver and the grid
	//--------------------------------------------------------------
	public void init()
	{
		File song = null;
		try
		{
			song = new File(System.getProperty("user.dir") + "\\Shelter.wav");
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
		rainbow.size = 10;// make it much lower when actually animating
		colorRow = new ArrayList<Color>();
		Thread t = new Thread(this);
		t.start();
	}

	public void convertAudioToShort(File song)
	{
		byte[] temp = new byte[0];
		short[] shorts = new short[0];
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(song);
			temp = new byte[(int)audioInputStream.getFrameLength()];
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
		heights = new short[monoShorts.length];
		for (int i = 0; i < shorts.length - 2; i+=2)
		{
			int location = i/2;
			short short1 = shorts[i];
			short short2 = shorts[i + 1];
			if (short1 < 0)
			{
				short1 *= -1;
			}
			if (short2 < 0)
			{
				short2 *= -1;
			}
			monoShorts[location] = (short) ((short1 + short2) / 2f);
			//int amt = (int)monoShorts[location];
			heights[location] = monoShorts[location];//amt;
			total += monoShorts[location];//amt;
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
			boolean goingUp = false;
			short tempHighest = 0;
			int peakCounter = 0;
			for (int i = 0; i < heightFrameLength; i++)
			{
				short amt = heights[(int) (i + j * heightFrameLength)];
				if (amt > tempHighest)
				{
					goingUp = true;
					tempHighest = amt;
				}
				else if (goingUp && amt < tempHighest)
				{
					peakCounter++;
					sum += tempHighest;
					goingUp = false;
					tempHighest = 0;
				}
			}
			if (goingUp)
			{
				peakCounter++;
				sum += tempHighest;
			}
			tempHeights[j] = (short) (sum/(short)peakCounter/ (amplitude * 4));
		}
		
		for (int i = 0; i < tempHeights.length; i++)
		{
			outputHeights[i] = (int)tempHeights[i];
		}
	}
	
	public void visualizeFrequencyBased()
	{
		int max = 0;
		short freqWidth = 1;//256 / DIMENSION;
		boolean passedFirstPeak = false;
		outputHeights = new int[(int)(totalTime * DIMENSION)];
		short[] tempHeights = new short[(int)(totalTime * DIMENSION)];
		for (int j = 0; j < totalTime; j++)
		{
			frequencies = new HashMap<Integer, Short>();
			boolean goingUp = false;
			short tempHighest = -1;
			int freqLength = 0;
			for (int i = 0; i < heightFrameLength; i++)
			{
				freqLength++;
				short amt = heights[(int) (i + j * heightFrameLength)];
				if (amt > tempHighest)
				{
					goingUp = true;
					tempHighest = amt;
				}
				else if (goingUp && amt < tempHighest)
				{
					if (passedFirstPeak)
					{
						if (!frequencies.containsKey(freqLength))
						{
							frequencies.put(freqLength, (short)0);
						}
						frequencies.put(freqLength, (short) (frequencies.get(freqLength) + 1));
					}
					else
					{
						passedFirstPeak = true;
					}
					goingUp = false;
					tempHighest = 0;
					freqLength = 0;
				}
				if (freqLength > max)
				{
					max = freqLength;
				}
			}

			for (Entry<Integer, Short> entry : frequencies.entrySet())
			{
				for (int i = 0; i < DIMENSION; i++)
				{
					Integer key = entry.getKey();
					if (key >= i * freqWidth && key < (i + 1) * freqWidth)
					{
						short value = entry.getValue();
						value /= 4;//(short)(Math.log(value) * 4);
						value = (short) Math.pow(value, (i + 1)/3);
						tempHeights[(int)(i + j * DIMENSION) - 2] += value;
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
		while (true) {
			try {
				lastTime = (int) (clip.getMicrosecondPosition() / WAIT_MICROSECONDS);
				// account for delay from processing the data. Also maybe from slow Java GUI
				if (volumeBased)
				{
					updateImageVB();
				}
				else
				{
					updateImageFB();
				}
				if (lastTime + DIMENSION >= outputHeights.length)
				{
					break;
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
