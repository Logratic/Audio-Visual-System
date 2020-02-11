#!/usr/bin/env python
from samplebase import SampleBase


class colorMatrix(SampleBase):
	def __init__(self, *args, **kwargs):
		super(SimpleSquare, self).__init__(*args, **kwargs)

	def run(self):
		offset_canvas = self.matrix.CreateFrameCanvas()
		while True:
			for x in range(0, self.matrix.width):
				offset_canvas.SetPixel(1, x, 255, 0, 0)
				
			offset_canvas = self.matrix.SwapOnVSync(offset_canvas)


# Main function
if __name__ == "__main__":
	mat = colorMatrix()
	if (not mat.process()):
mat.print_help()
