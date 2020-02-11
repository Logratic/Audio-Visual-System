from samplebase import SampleBase


class simpleMatrix(SampleBase):
    def __init__(self, *args, **kwargs):
        super(simpleMatrix, self).__init__(*args, **kwargs)

    def run(self):
        self.offscreen_canvas = self.matrix.CreateFrameCanvas()
        continuum = 0

        while True:
            self.usleep(5 * 1000)       # Seems to be a sleep function, board turns off after 5000 ticks in this case
            continuum += 1              # increments continuum
            continuum %= 3 * 255        # This causes continuum to reset to 0 once it gets to 765 (order is ... 763, 764, 0, 1, ...)

            red = 0                     # initialize colors to 0
            green = 0
            blue = 0

            if continuum <= 255:        # green is 0, blue starts at 255 and decreases, red starts at 0 and increases
                c = continuum
                blue = 255 - c
                red = c
            elif continuum > 255 and continuum <= 511:
                c = continuum - 256     # c goes from 0 to 255.  blue is 0, red starts at 255 and decreases, green starts at 0 and increases
                red = 255 - c
                green = c
            else:
                c = continuum - 512     # c goes from 0 to 252 (modulo cuts off early)
                green = 255 - c         # blue goes from 0 to 252, green starts at 255 and decreases to 3, red is 0
                blue = c

            self.offscreen_canvas.Fill(red, green, blue)        # this command fills the board with a single color with red/green/blue arguements
            self.offscreen_canvas = self.matrix.SwapOnVSync(self.offscreen_canvas)

# Main function
if __name__ == "__main__":
    mat = simpleMatrix()
    if (not mat.process()):
mat.print_help()
