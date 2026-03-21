package com.noty215.notycaption.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class AudioProcessor {
    private static final Logger logger = Logger.getLogger(AudioProcessor.class.getName());

    public static AudioFormat getAudioFormat(File audioFile) throws UnsupportedAudioFileException, IOException {
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        return audioStream.getFormat();
    }

    public static boolean isFormatSupported(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        return AudioSystem.isLineSupported(info);
    }

    public static AudioFormat getPCMFormat(AudioFormat sourceFormat) {
        return new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.getSampleRate(),
            16,
            sourceFormat.getChannels(),
            sourceFormat.getChannels() * 2,
            sourceFormat.getSampleRate(),
            false
        );
    }

    public static AudioInputStream convertToPCM(AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = getPCMFormat(sourceFormat);
        return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
    }

    public static double[] getAudioSamples(File audioFile) throws Exception {
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioStream.getFormat();
        
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            audioStream = convertToPCM(audioStream);
            format = audioStream.getFormat();
        }
        
        int bytesPerFrame = format.getFrameSize();
        int frameLength = (int) audioStream.getFrameLength();
        int totalSamples = frameLength * format.getChannels();
        
        byte[] audioBytes = new byte[frameLength * bytesPerFrame];
        audioStream.read(audioBytes);
        
        double[] samples = new double[totalSamples];
        for (int i = 0; i < totalSamples; i++) {
            int sampleIndex = i * 2;
            int sample = ((audioBytes[sampleIndex + 1] & 0xFF) << 8) | (audioBytes[sampleIndex] & 0xFF);
            samples[i] = sample / 32768.0;
        }
        
        audioStream.close();
        return samples;
    }

    public static void saveAudioSamples(double[] samples, int sampleRate, int channels, File outputFile) 
            throws Exception {
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        
        byte[] audioBytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            short sample = (short) (samples[i] * 32767);
            audioBytes[i * 2] = (byte) (sample & 0xFF);
            audioBytes[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream audioStream = new AudioInputStream(bais, format, samples.length / channels);
        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outputFile);
        audioStream.close();
        
        logger.info("Audio saved to: " + outputFile.getAbsolutePath());
    }

    public static double[] resample(double[] samples, int originalRate, int targetRate) {
        if (originalRate == targetRate) return samples;
        
        double ratio = (double) targetRate / originalRate;
        int newLength = (int) (samples.length * ratio);
        double[] resampled = new double[newLength];
        
        for (int i = 0; i < newLength; i++) {
            double position = i / ratio;
            int index = (int) position;
            if (index >= samples.length - 1) {
                resampled[i] = samples[samples.length - 1];
            } else {
                double fraction = position - index;
                resampled[i] = samples[index] * (1 - fraction) + samples[index + 1] * fraction;
            }
        }
        
        return resampled;
    }

    public static void normalizeAudio(double[] samples, double targetPeak) {
        double maxAbs = 0;
        for (double sample : samples) {
            maxAbs = Math.max(maxAbs, Math.abs(sample));
        }
        
        if (maxAbs > 0) {
            double gain = targetPeak / maxAbs;
            for (int i = 0; i < samples.length; i++) {
                samples[i] *= gain;
            }
        }
    }

    public static double[] applyNoiseReduction(double[] samples, int sampleRate, double noiseFloor) {
        double[] processed = new double[samples.length];
        double[] window = new double[512];
        
        for (int i = 0; i < samples.length; i++) {
            processed[i] = samples[i];
        }
        
        // Simple noise gate
        for (int i = 0; i < samples.length; i++) {
            if (Math.abs(processed[i]) < noiseFloor) {
                processed[i] = 0;
            }
        }
        
        return processed;
    }
}