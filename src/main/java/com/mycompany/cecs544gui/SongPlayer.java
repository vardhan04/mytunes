//
//package com.mycompany.cecs544gui;
//
//import com.mpatric.mp3agic.Mp3File;
//import javax.sound.sampled.*;
//import javazoom.jl.decoder.*;
//import javazoom.jl.player.*;
//import javazoom.jl.player.advanced.*;
//import java.io.*;
//import java.lang.reflect.Field;
//import java.util.logging.*;
//
//public class SongPlayer {
//    private static final Logger LOGGER = Logger.getLogger(SongPlayer.class.getName());
//
//    private String currentSongPath;
//    private boolean paused = false;
//    private boolean playing = false;
//    private Thread playbackThread;
//    private InputStream audioInputStream;
//    private Bitstream mp3Bitstream;
//    private Decoder decoder;
//    private JavaSoundAudioDevice audio;
//    private long pausedFrameCount = 0;
//    private SourceDataLine sourceLine;
//    private FloatControl volumeControl;
//    private volatile float volumeMultiplier = 0.5f;
//
//  public synchronized void play(String filePath) {
//    if (!paused || !filePath.equals(currentSongPath)) {
//        stop();
//        pausedFrameCount = 0;
//    }
//
//    try {
//        currentSongPath = filePath;
//        audioInputStream = new BufferedInputStream(new FileInputStream(currentSongPath));
//        mp3Bitstream = new Bitstream(audioInputStream);
//        decoder = new Decoder();
//
//        if (audio == null) {
//            audio = (JavaSoundAudioDevice) createAudioDevice();
//            audio.open(decoder);
//        }
//
//        playbackThread = new Thread(() -> {
//            try {
//                boolean done = false;
//                while (!done && !Thread.currentThread().isInterrupted()) {
//                    if (paused) {
//                        Thread.sleep(100);
//                        continue;
//                    }
//
//                    Header frameHeader = mp3Bitstream.readFrame();
//                    if (frameHeader == null) {
//                        done = true;
//                    } else {
//                        SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, mp3Bitstream);
//                        adjustVolume(output); // Adjust volume here
//
//                        synchronized (this) {
//                            if (audio != null) {
//                                audio.write(output.getBuffer(), 0, output.getBufferLength());
//                            }
//                        }
//                        mp3Bitstream.closeFrame();
//                        pausedFrameCount++;
//                    }
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            } catch (Exception e) {
//                LOGGER.log(Level.SEVERE, "Playback Error", e);
//            } finally {
//                cleanup();
//            }
//        });
//
//        playbackThread.start();
//        paused = false;
//        playing = true;
//    } catch (Exception e) {
//        LOGGER.log(Level.SEVERE, "Initializing audio player Error", e);
//    }
//}
//
//
//  
//private AudioDevice createAudioDevice() throws JavaLayerException {
//        try {
//            return FactoryRegistry.systemRegistry().createAudioDevice();
//        } catch (JavaLayerException e) {
//            LOGGER.log(Level.WARNING, "Failed to create system audio", e);
//            return new JavaSoundAudioDevice();
//        }
//    }
//
//
//    private void cleanup() {
//        try {
//            if (audio != null) {
//                audio.flush();
//            }
//            closeAudio();
//            closeStreams();
//        } catch (Exception e) {
//            LOGGER.log(Level.WARNING, "Clearing stream Error", e);
//        } finally {
//            playing = false;
//        }
//    }
//
//    public synchronized void stop() {
//        if (playbackThread != null) {
//            playbackThread.interrupt();
//            try {
//                playbackThread.join(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//        cleanup();
//        pausedFrameCount = 0;
//        paused = false;
//        playing = false;
//    }
//
//    private void closeAudio() {
//        if (audio != null) {
//            synchronized (this) {
//                audio.close();
//                audio = null;
//            }
//        }
//    }
//
//    private void closeStreams() {
//        try {
//            if (audioInputStream != null) {
//                audioInputStream.close();
//            }
//            if (mp3Bitstream != null) {
//                mp3Bitstream.close();
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.WARNING, "Closing stream Error", e);
//        }
//    }
//
//    public void pause() {
//        paused = true;
//        playing = false;
//    }
//
//    public void unpause() {
//        paused = false;
//        playing = true;
//    }
//
//    public boolean isPlaying() {
//        return playing;
//    }
//
////    public void setVolume(float volume) {
////    if (volumeControl != null) {
////        float min = volumeControl.getMinimum();
////        float max = volumeControl.getMaximum();
////        float volumeValue = min + (volume * (max - min));
////        volumeControl.setValue(volumeValue);
////    } else {
////        LOGGER.log(Level.WARNING, "Volume control is not initialized.");
////    }
////}
//    
//    private void adjustVolume(SampleBuffer output) {
//    short[] buffer = output.getBuffer();
//    for (int i = 0; i < buffer.length; i++) {
//        buffer[i] = (short) (buffer[i] * volumeMultiplier);
//    }
//}
//
//public void setVolume(float volume) {
//    this.volumeMultiplier = Math.max(0.0f, Math.min(1.0f, volume)); // Clamp between 0 and 1
//}
//
//    public float getVolume() {
//        return this.volumeMultiplier;
//    }
//    
//    public int getSongDuration(String filePath) {
//    try {
//        Mp3File mp3File = new Mp3File(filePath);
//        if (mp3File.hasId3v2Tag()) {
//            // Duration is in milliseconds; convert it to seconds
//            return (int) mp3File.getLengthInSeconds();
//        } else if (mp3File.hasId3v1Tag()) {
//            // Duration is in milliseconds; convert it to seconds
//            return (int) mp3File.getLengthInSeconds();
//        }
//    } catch (Exception e) {
//        e.printStackTrace();
//    }
//    return 0; // Return 0 if duration cannot be determined
//}
//}


//package com.mycompany.cecs544gui;
//
//import com.mpatric.mp3agic.Mp3File;
//import javax.sound.sampled.*;
//import javazoom.jl.decoder.*;
//import javazoom.jl.player.*;
//import javazoom.jl.player.advanced.*;
//import java.io.*;
//import java.util.logging.*;
//
//public class SongPlayer {
//    private static final Logger LOGGER = Logger.getLogger(SongPlayer.class.getName());
//
//    private String currentSongPath;
//    private boolean paused = false;
//    private boolean playing = false;
//    private Thread playbackThread;
//    private InputStream audioInputStream;
//    private Bitstream mp3Bitstream;
//    private Decoder decoder;
//    private JavaSoundAudioDevice audio;
//    private long pausedFrameCount = 0;
//    private volatile float volumeMultiplier = 0.5f;
//
//    public synchronized void play(String filePath) {
//        // If the current song is different or we aren't paused, stop the current playback and reset everything
//        if (!paused || !filePath.equals(currentSongPath)) {
//            stop();
//            pausedFrameCount = 0;
//        }
//
//        try {
//            currentSongPath = filePath;
//            audioInputStream = new BufferedInputStream(new FileInputStream(currentSongPath));
//            mp3Bitstream = new Bitstream(audioInputStream);
//            decoder = new Decoder();
//
//            // Always create a new audio device for playback to avoid errors
//            audio = (JavaSoundAudioDevice) createAudioDevice();
//            audio.open(decoder);
//
//            playbackThread = new Thread(() -> {
//                try {
//                    boolean done = false;
//                    while (!done && !Thread.currentThread().isInterrupted()) {
//                        if (paused) {
//                            Thread.sleep(100);
//                            continue;
//                        }
//
//                        Header frameHeader = mp3Bitstream.readFrame();
//                        if (frameHeader == null) {
//                            done = true;
//                        } else {
//                            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, mp3Bitstream);
//                            adjustVolume(output); // Adjust volume here
//
//                            synchronized (this) {
//                                if (audio != null) {
//                                    audio.write(output.getBuffer(), 0, output.getBufferLength());
//                                }
//                            }
//                            mp3Bitstream.closeFrame();
//                            pausedFrameCount++;
//                        }
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                } catch (Exception e) {
//                    LOGGER.log(Level.SEVERE, "Playback Error", e);
//                } finally {
//                    cleanup();
//                }
//            });
//
//            playbackThread.start();
//            paused = false;
//            playing = true;
//        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "Initializing audio player Error", e);
//        }
//    }
//
//    private AudioDevice createAudioDevice() throws JavaLayerException {
//        try {
//            return FactoryRegistry.systemRegistry().createAudioDevice();
//        } catch (JavaLayerException e) {
//            LOGGER.log(Level.WARNING, "Failed to create system audio", e);
//            return new JavaSoundAudioDevice();
//        }
//    }
//
//    private void cleanup() {
//        try {
//            if (audio != null) {
//                audio.flush();
//            }
//            closeAudio();
//            closeStreams();
//        } catch (Exception e) {
//            LOGGER.log(Level.WARNING, "Clearing stream Error", e);
//        } finally {
//            playing = false;
//        }
//    }
//
//    public synchronized void stop() {
//        if (playbackThread != null) {
//            playbackThread.interrupt();
//            try {
//                playbackThread.join(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//        cleanup();
//        pausedFrameCount = 0;
//        paused = false;
//        playing = false;
//    }
//
//    private void closeAudio() {
//        if (audio != null) {
//            synchronized (this) {
//                audio.close();
//                audio = null;
//            }
//        }
//    }
//
//    private void closeStreams() {
//        try {
//            if (audioInputStream != null) {
//                audioInputStream.close();
//            }
//            if (mp3Bitstream != null) {
//                mp3Bitstream.close();
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.WARNING, "Closing stream Error", e);
//        }
//    }
//
//    public void pause() {
//        paused = true;
//        playing = false;
//    }
//
//    public void unpause() {
//        paused = false;
//        playing = true;
//    }
//
//    public boolean isPlaying() {
//        return playing;
//    }
//
//    private void adjustVolume(SampleBuffer output) {
//        short[] buffer = output.getBuffer();
//        for (int i = 0; i < buffer.length; i++) {
//            buffer[i] = (short) (buffer[i] * volumeMultiplier);
//        }
//    }
//
//    public void setVolume(float volume) {
//        this.volumeMultiplier = Math.max(0.0f, Math.min(1.0f, volume)); // Clamp between 0 and 1
//    }
//
//    public float getVolume() {
//        return this.volumeMultiplier;
//    }
//
//    public int getSongDuration(String filePath) {
//        try {
//            Mp3File mp3File = new Mp3File(filePath);
//            if (mp3File.hasId3v2Tag() || mp3File.hasId3v1Tag()) {
//                // Duration is in seconds
//                return (int) mp3File.getLengthInSeconds();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return 0; // Return 0 if duration cannot be determined
//    }
//}

//
//package com.mycompany.cecs544gui;
//
//import com.mpatric.mp3agic.Mp3File;
//import javazoom.jl.decoder.*;
//import javazoom.jl.player.*;
//import javazoom.jl.player.advanced.*;
//
//import javax.sound.sampled.*;
//import java.io.*;
//import java.util.logging.*;
//
//public class SongPlayer {
//    private static final Logger LOGGER = Logger.getLogger(SongPlayer.class.getName());
//
//    private String currentSongPath;
//    private boolean paused = false;
//    private boolean playing = false;
//    private Thread playbackThread;
//    private InputStream audioInputStream;
//    private Bitstream mp3Bitstream;
//    private Decoder decoder;
//    private JavaSoundAudioDevice audio;
//    private long pausedFrameCount = 0;
//    private volatile float volumeMultiplier = 0.5f;
//
//    public synchronized void play(String filePath) {
//        // If the current song is different or we aren't paused, stop the current playback and reset everything
//        if (!paused || !filePath.equals(currentSongPath)) {
//            stop();
//            pausedFrameCount = 0;
//        }
//
//        try {
//            currentSongPath = filePath;
//            audioInputStream = new BufferedInputStream(new FileInputStream(currentSongPath));
//            mp3Bitstream = new Bitstream(audioInputStream);
//            decoder = new Decoder();
//
//            // Always create a new audio device for playback to avoid errors
//            audio = (JavaSoundAudioDevice) createAudioDevice();
//            audio.open(decoder);
//
//            playbackThread = new Thread(() -> {
//                try {
//                    boolean done = false;
//                    while (!done && !Thread.currentThread().isInterrupted()) {
//                        if (paused) {
//                            Thread.sleep(100);
//                            continue;
//                        }
//
//                        Header frameHeader = mp3Bitstream.readFrame();
//                        if (frameHeader == null) {
//                            done = true;
//                        } else {
//                            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, mp3Bitstream);
//                            adjustVolume(output); // Adjust volume here
//
//                            synchronized (this) {
//                                if (audio != null) {
//                                    audio.write(output.getBuffer(), 0, output.getBufferLength());
//                                }
//                            }
//                            mp3Bitstream.closeFrame();
//                            pausedFrameCount++;
//                        }
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                } catch (Exception e) {
//                    LOGGER.log(Level.SEVERE, "Playback Error", e);
//                } finally {
//                    cleanup();
//                }
//            });
//
//            playbackThread.start();
//            paused = false;
//            playing = true;
//        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "Initializing audio player Error", e);
//        }
//    }
//
//    private AudioDevice createAudioDevice() throws JavaLayerException {
//        try {
//            return FactoryRegistry.systemRegistry().createAudioDevice();
//        } catch (JavaLayerException e) {
//            LOGGER.log(Level.WARNING, "Failed to create system audio", e);
//            return new JavaSoundAudioDevice();
//        }
//    }
//
//    private void cleanup() {
//        try {
//            if (audio != null) {
//                audio.flush();
//            }
//            closeAudio();
//            closeStreams();
//        } catch (Exception e) {
//            LOGGER.log(Level.WARNING, "Clearing stream Error", e);
//        } finally {
//            playing = false;
//        }
//    }
//
//    public synchronized void stop() {
//        if (playbackThread != null) {
//            playbackThread.interrupt();
//            try {
//                playbackThread.join(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//        cleanup();
//        pausedFrameCount = 0;
//        paused = false;
//        playing = false;
//    }
//
//    private void closeAudio() {
//        if (audio != null) {
//            synchronized (this) {
//                audio.close();
//                audio = null;
//            }
//        }
//    }
//
//    private void closeStreams() {
//        try {
//            if (audioInputStream != null) {
//                audioInputStream.close();
//            }
//            if (mp3Bitstream != null) {
//                mp3Bitstream.close();
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.WARNING, "Closing stream Error", e);
//        }
//    }
//
//    public void pause() {
//        paused = true;
//        playing = false;
//    }
//
//    public void unpause() {
//        paused = false;
//        playing = true;
//    }
//
//    public boolean isPlaying() {
//        return playing;
//    }
//
//    private void adjustVolume(SampleBuffer output) {
//        short[] buffer = output.getBuffer();
//        for (int i = 0; i < buffer.length; i++) {
//            buffer[i] = (short) (buffer[i] * volumeMultiplier);
//        }
//    }
//
//    public void setVolume(float volume) {
//        this.volumeMultiplier = Math.max(0.0f, Math.min(1.0f, volume)); // Clamp between 0 and 1
//    }
//
//    public float getVolume() {
//        return this.volumeMultiplier;
//    }
//
//    public int getSongDuration(String filePath) {
//        try {
//            Mp3File mp3File = new Mp3File(filePath);
//            if (mp3File.hasId3v2Tag() || mp3File.hasId3v1Tag()) {
//                // Duration is in seconds
//                return (int) mp3File.getLengthInSeconds();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return 0; // Return 0 if duration cannot be determined
//    }
//}

//
//package com.mycompany.cecs544gui;
//
//import com.mpatric.mp3agic.Mp3File;
//import javax.sound.sampled.*;
//import javazoom.jl.decoder.*;
//import javazoom.jl.player.*;
//import javazoom.jl.player.advanced.*;
//import java.io.*;
//import java.util.logging.*;
//
//public class SongPlayer {
//    private static final Logger LOGGER = Logger.getLogger(SongPlayer.class.getName());
//
//    private String currentSongPath;
//    private boolean paused = false;
//    private boolean playing = false;
//    private Thread playbackThread;
//    private InputStream audioInputStream;
//    private Bitstream mp3Bitstream;
//    private Decoder decoder;
//    private JavaSoundAudioDevice audio;
//    private long pausedFrameCount = 0;
//    private volatile float volumeMultiplier = 0.5f;
//
//    public synchronized void play(String filePath) {
//        // If the current song is different or we aren't paused, stop the current playback and reset everything
//        if (!paused || !filePath.equals(currentSongPath)) {
//            stop();
//            pausedFrameCount = 0;
//        }
//
//        try {
//            currentSongPath = filePath;
//            audioInputStream = new BufferedInputStream(new FileInputStream(currentSongPath));
//            mp3Bitstream = new Bitstream(audioInputStream);
//            decoder = new Decoder();
//
//            // Always create a new audio device for playback to avoid errors
//            audio = (JavaSoundAudioDevice) createAudioDevice();
//            audio.open(decoder);
//
//            playbackThread = new Thread(() -> {
//                try {
//                    boolean done = false;
//                    while (!done && !Thread.currentThread().isInterrupted()) {
//                        if (paused) {
//                            Thread.sleep(100);
//                            continue;
//                        }
//
//                        Header frameHeader = mp3Bitstream.readFrame();
//                        if (frameHeader == null) {
//                            done = true;
//                        } else {
//                            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, mp3Bitstream);
//                            adjustVolume(output); // Adjust volume here
//
//                            synchronized (this) {
//                                if (audio != null) {
//                                    audio.write(output.getBuffer(), 0, output.getBufferLength());
//                                }
//                            }
//                            mp3Bitstream.closeFrame();
//                            pausedFrameCount++;
//                        }
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                } catch (Exception e) {
//                    LOGGER.log(Level.SEVERE, "Playback Error", e);
//                } finally {
//                    cleanup();
//                }
//            });
//
//            playbackThread.start();
//            paused = false;
//            playing = true;
//        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "Initializing audio player Error", e);
//        }
//    }
//
//    private AudioDevice createAudioDevice() throws JavaLayerException {
//        try {
//            return FactoryRegistry.systemRegistry().createAudioDevice();
//        } catch (JavaLayerException e) {
//            LOGGER.log(Level.WARNING, "Failed to create system audio", e);
//            return new JavaSoundAudioDevice();
//        }
//    }
//
//    private void cleanup() {
//        try {
//            if (audio != null) {
//                audio.flush();
//            }
//            closeAudio();
//            closeStreams();
//        } catch (Exception e) {
//            LOGGER.log(Level.WARNING, "Clearing stream Error", e);
//        } finally {
//            playing = false;
//        }
//    }
//
//    public synchronized void stop() {
//        if (playbackThread != null) {
//            playbackThread.interrupt();
//            try {
//                playbackThread.join(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//        cleanup();
//        pausedFrameCount = 0;
//        paused = false;
//        playing = false;
//    }
//
//    private void closeAudio() {
//        if (audio != null) {
//            synchronized (this) {
//                audio.close();
//                audio = null;
//            }
//        }
//    }
//
//    private void closeStreams() {
//        try {
//            if (audioInputStream != null) {
//                audioInputStream.close();
//            }
//            if (mp3Bitstream != null) {
//                mp3Bitstream.close();
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.WARNING, "Closing stream Error", e);
//        }
//    }
//
//    public void pause() {
//        paused = true;
//        playing = false;
//    }
//
//    public void unpause() {
//        paused = false;
//        playing = true;
//    }
//
//    public boolean isPlaying() {
//        return playing;
//    }
//
//    private void adjustVolume(SampleBuffer output) {
//        short[] buffer = output.getBuffer();
//        for (int i = 0; i < buffer.length; i++) {
//            buffer[i] = (short) (buffer[i] * volumeMultiplier);
//        }
//    }
//
//    public void setVolume(float volume) {
//        this.volumeMultiplier = Math.max(0.0f, Math.min(1.0f, volume)); // Clamp between 0 and 1
//    }
//
//    public float getVolume() {
//        return this.volumeMultiplier;
//    }
//
//    public int getSongDuration(String filePath) {
//        try {
//            Mp3File mp3File = new Mp3File(filePath);
//            if (mp3File.hasId3v2Tag() || mp3File.hasId3v1Tag()) {
//                // Duration is in seconds
//                return (int) mp3File.getLengthInSeconds();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return 0; // Return 0 if duration cannot be determined
//    }
//}

package com.mycompany.cecs544gui;

import com.mpatric.mp3agic.Mp3File;
import javazoom.jl.decoder.*;
import javazoom.jl.player.*;
import javazoom.jl.player.advanced.*;

import javax.sound.sampled.*;
import java.io.*;
import java.util.logging.*;

public class SongPlayer {
    private static final Logger LOGGER = Logger.getLogger(SongPlayer.class.getName());

    private String currentSongPath;
    private boolean paused = false;
    private boolean playing = false;
    private Thread playbackThread;
    private InputStream audioInputStream;
    private Bitstream mp3Bitstream;
    private Decoder decoder;
    private JavaSoundAudioDevice audio;
    private long pausedFrameCount = 0;
    private volatile float volumeMultiplier = 0.5f;

    public synchronized void play(String filePath) {
        stop(); // Always stop before playing, even if it's the same song
        try {
            Thread.sleep(100); // Small delay to allow resource release
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            currentSongPath = filePath;
            audioInputStream = new BufferedInputStream(new FileInputStream(currentSongPath));
            mp3Bitstream = new Bitstream(audioInputStream);
            decoder = new Decoder();

            // Always create a new audio device for each playback
            audio = (JavaSoundAudioDevice) createAudioDevice();
            audio.open(decoder);

            playbackThread = new Thread(() -> {
                try {
                    boolean done = false;
                    while (!done && !Thread.currentThread().isInterrupted()) {
                        if (paused) {
                            Thread.sleep(100);
                            continue;
                        }

                        Header frameHeader = null;
                        try {
                            frameHeader = mp3Bitstream.readFrame();
                        } catch (BitstreamException e) {
                            LOGGER.log(Level.SEVERE, "Error reading MP3 frame", e);
                            break;
                        }

                        if (frameHeader == null) {
                            done = true;
                        } else {
                            try {
                                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, mp3Bitstream);
                                adjustVolume(output);

                                synchronized (this) {
                                    if (audio != null) {
                                        audio.write(output.getBuffer(), 0, output.getBufferLength());
                                    }
                                }
                                mp3Bitstream.closeFrame();
                                pausedFrameCount++;
                            } catch (DecoderException e) {
                                LOGGER.log(Level.SEVERE, "Error decoding MP3 frame", e);
                                break;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Playback Error", e);
                } finally {
                    cleanup();
                }
            });

            playbackThread.start();
            paused = false;
            playing = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Initializing audio player Error", e);
        }
    }

    private AudioDevice createAudioDevice() throws JavaLayerException {
        try {
            return FactoryRegistry.systemRegistry().createAudioDevice();
        } catch (JavaLayerException e) {
            LOGGER.log(Level.WARNING, "Failed to create system audio", e);
            return new JavaSoundAudioDevice();
        }
    }

    private void cleanup() {
        try {
            if (audio != null) {
                audio.flush();
            }
            closeAudio();
            closeStreams();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Clearing stream Error", e);
        } finally {
            playing = false;
        }
    }

    public synchronized void stop() {
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        cleanup();
        pausedFrameCount = 0;
        paused = false;
        playing = false;
        
        // Ensure audio device is closed
        if (audio != null) {
            audio.close();
            audio = null;
        }
    }

    private void closeAudio() {
        if (audio != null) {
            synchronized (this) {
                audio.close();
                audio = null;
            }
        }
    }

    private void closeStreams() {
        try {
            if (audioInputStream != null) {
                audioInputStream.close();
            }
            if (mp3Bitstream != null) {
                mp3Bitstream.close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Closing stream Error", e);
        }
    }

    public void pause() {
        paused = true;
        playing = false;
    }

    public void unpause() {
        paused = false;
        playing = true;
    }

    public boolean isPlaying() {
        return playing;
    }

    private void adjustVolume(SampleBuffer output) {
        short[] buffer = output.getBuffer();
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (short) (buffer[i] * volumeMultiplier);
        }
    }

    public void setVolume(float volume) {
        this.volumeMultiplier = Math.max(0.0f, Math.min(1.0f, volume)); // Clamp between 0 and 1
    }

    public float getVolume() {
        return this.volumeMultiplier;
    }

    public int getSongDuration(String filePath) {
        try {
            Mp3File mp3File = new Mp3File(filePath);
            if (mp3File.hasId3v2Tag() || mp3File.hasId3v1Tag()) {
                return (int) mp3File.getLengthInSeconds();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0; // Return 0 if duration cannot be determined
    }

    public void repeat() {
        if (currentSongPath != null) {
            stop();
            try {
                Thread.sleep(100); // Small delay before restarting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            play(currentSongPath);
        }
    }
}