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
	int prevLocation = 0;
	float floatMultiplier;
	short[] songShortArray;
	byte[] songByteArray;
	long totalTime;
	int[] heights;
	int heighest = 0;
	int amplitude = 0;

	//TODO: Adjust the speed, take highest or average short to set the divisor

	//--------------------------------------------------------------
	//sets up the solver and the grid
	//--------------------------------------------------------------
	public void init()
	{
		File song = null;
		try
		{
			song = new File(System.getProperty("user.dir") + "\\Black Sabbath.wav");
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(song);
			clip = AudioSystem.getClip();
			clip.open(audioInputStream);
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			gainControl.setValue(-20.0f); // Reduce volume by 20 decibels.
			clip.start();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
		}

		floatMultiplier = DIMENSION/255f;
		songShortArray = convertAudioToShort(song);
		System.out.println(songShortArray.length);

		totalTime = (long)clip.getMicrosecondLength() / songShortArray.length; // casting float so it never rounds to int early
		this.setSize(DIMENSION * SIZE + 1, DIMENSION * SIZE + 1); // sets the size of the app
		rainbow = new Rainbow();
		grid = new boolean[DIMENSION][DIMENSION];
		rainbow.size = 10;// make it much lower when actually animating
		colorRow = new ArrayList<Color>();
		Thread t = new Thread(this);
		t.start();
	}
	
	public short[] convertAudioToShort(File song)
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
		heights = new int[monoShorts.length];
		int zeroCounter = 0;
		boolean finishedSkipping = false;
		for (int i = 0; i < shorts.length - 2; i+=2)
		{
			int location = i/2;
			monoShorts[location] = (short) ((shorts[i] + shorts[i + 1]) / 2f);
			if (!finishedSkipping)
			{
				if (monoShorts[location] == 0)
				{
					zeroCounter++;
				}
				else
				{
					finishedSkipping = true;
				}
			}
			else
			{
				int amt = Math.abs((int)(monoShorts[location] * floatMultiplier));
				heights[location] = amt;
				if (amt > heighest)
				{
					heighest = amt;
				}
				total += amt;
			}
		}

		amplitude = (int) (total / monoShorts.length) / DIMENSION;
		System.out.println(amplitude);
		//amplitude = heighest/DIMENSION;
		// temp = averageBytes(temp, 2); shouldn't be used
		System.out.println(monoShorts.length + " " + zeroCounter);
		
		shorts = new short[monoShorts.length - zeroCounter];
		int counter = 0;
		for (int i = 0; i < monoShorts.length; i++)
		{
			if (monoShorts[i] == 0)
			{
				shorts[i - counter] = monoShorts[i];
				counter++;
			}
			else
			{
				break;
			}
		}
		
		
		return shorts;

	}

	// should no longer be used
	public byte[] averageBytes(byte[] ogBytes, int amt)
	{
		byte[] adjusted = new byte[ogBytes.length / amt];
		for (int i = 0; i < ogBytes.length - amt; i += amt)
		{
			float average = 0;
			for (int j = 0; j < amt; j++)
			{
				average += Math.abs(ogBytes[i + j]);
			}
			adjusted[i/amt] = (byte) (average/amt);
		}
		return adjusted;
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


	public void updateImage()
	{
		for (int i = prevLocation; i < prevLocation + DIMENSION; i ++)
		{
			int x = (i - prevLocation);
			int y = heights[i];
			y /= amplitude; // used to adjust amplitude
			/*if (y == 0)
			{
				y = heights[i];
			}
			if (y == 0)
			{
				y = songShortArray[i];
				System.out.println(songShortArray[i]);
			}*/
			turnPixelOn(x, y);
		}
		prevLocation++;
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
		while (true) {
			try {
				int tempTime = (int) (clip.getMicrosecondPosition() / totalTime);
				if (tempTime > lastTime)
				{
					lastTime = tempTime;
					updateImage();
				}
				Thread.sleep(20);//20
			} catch (Exception e) {
				e.printStackTrace();
			}
			repaint();
		}
	}
}
