import java.awt.Color;


public class Rainbow {
	private double r, g, b;
	private int step;
	public double size;
	private boolean positive = true;

	//--------------------------------------------------------------
	//sets up the rainbow
	//--------------------------------------------------------------
	public Rainbow() {
		size = 1; // default size, the size can be between 0 non-inclusive and 255 inclusive (Or 0.0-1.0 if working with another Color standard)
		g = b = 0;
		r = 255; // Starts color as red, change to 1.0 if working with another color standard
		step = 0;
	}

	//--------------------------------------------------------------
	//Updates the given color variable to the next iteration
	//--------------------------------------------------------------
	private double updateColor(double col)
	{
		if (positive)
		{
			col += size; // increases the color if it started at 0
		}
		else
		{
			col -= size; // decreases the color if it started at 255
		}

		if (col <= 0 || col >= 255)
		{ // Why does java Math not have a clamp method
			col = col < 0 ? 0 : col;
			col = col > 255 ? 255 : col;
			step++; // Goes to the next step in the rainbow fade
			positive = !positive; // since each step adds or subtracts from the variable
			if (step >= 3) // loops the steps
			{
				step = 0;
			}
		}

		return col;
	}

	//--------------------------------------------------------------
	//updates the rainbow
	//--------------------------------------------------------------
	public Color nextColor()
	{
		// Switch case can be switched to if, else if, and else
		switch (step) // 3 steps as the g r and b separately go up and down
		{
		case 0:
			g = updateColor(g);
			break;

		case 1:
			r = updateColor(r);
			break;

		case 2:
			b = updateColor(b);
			break;

		default: // in case someone changes the code and messes up the steps
			step = 0;
			nextColor();
			break;
		}

		return new Color((int)r, (int)g, (int)b);
	}

}
