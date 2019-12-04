

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Visualizer {
    private static float SAMPLE_RATE = 8000;
    private long extendSign(long temp, int bitsPerSample) {
        int extensionBits = 64 - bitsPerSample;
        return (temp << extensionBits) >> extensionBits;
    }
    public Visualizer() throws LineUnavailableException {
        int sampleSizeBits = 16;
        int numChannels = 1; // Mono
        AudioFormat format = new AudioFormat(SAMPLE_RATE, sampleSizeBits, numChannels, true, true);
        TargetDataLine tdl = AudioSystem.getTargetDataLine(format);
        tdl.open(format);
        tdl.start();
        if (!tdl.isOpen()) {
            System.exit(1);         
        } 
        byte[] data = new byte[(int)SAMPLE_RATE*10];
        int read = tdl.read(data, 0, (int)SAMPLE_RATE*10);
        if (read > 0) {
            for (int i = 0; i < read-1; i = i + 2) {
            	/*
                long val = ((data[i] & 0xffL) << 8L) | (data[i + 1] & 0xffL);
                long valf = extendSign(val, 16);*/
                //System.out.println(i + "\t" + valf);
            }
        }
        tdl.close();
    }
    
    public float[] toFloatArray(byte[] byteArray)
    {
        int len = byteArray.length / 4;
        float[] floatArray = new float[len];
        for (int i = 0; i < byteArray.length; i += 4)
        {
        	int asInt = (byteArray[i] & 0xFF) 
                    | ((byteArray[i + 1] & 0xFF) << 8) 
                    | ((byteArray[i + 2] & 0xFF) << 16) 
                    | ((byteArray[i + 3] & 0xFF) << 24);
        	float asFloat = Float.intBitsToFloat(asInt);
        	
            floatArray[i / 4] = asFloat;
        }
        return floatArray;
    }
}
