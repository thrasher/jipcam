/**
 * jipCam : The Java IP Camera Project
 * Copyright (C) 2005-2006 Jason Thrasher
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.sf.jipcam.axis.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import net.sf.jipcam.axis.CameraAPI;
import net.sf.jipcam.axis.MjpegFrameParser;
import net.sf.jipcam.axis.MjpegParserEvent;
import net.sf.jipcam.axis.MjpegParserListener;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

/**
 * This frame rate monitor is a tool to track the frame count and the frame
 * rate. The FPS (frame rate) calculation doesn't begin until the first frame is
 * processed.
 * 
 * JMF is not used required for this tool.
 * 
 * @author Jason Thrasher
 */
public class FramerateMonitor implements MjpegParserListener {
    protected static Options options = null;// command line options

    // private Timer timer = new Timer();
    private int mFrameCount;

    private long mStartTime; // last timestamp in ms

    /**
     * Create a new monitor initalized with zero FPS and Count.
     */
    public FramerateMonitor() {
        mFrameCount = 0;
        mStartTime = 0L;
    }

    /**
     * Handle the next frame. No processing is performed on the frame. Counts
     * are updated, and the timer is started.
     * 
     * @param frame
     */
    public void onMjpegParserEvent(MjpegParserEvent event) {
        // increment the count
        mFrameCount++;

        // initalize the start time if needed
        if (mStartTime == 0) {
            mStartTime = System.currentTimeMillis();
        }
    }

    /**
     * Get the average frames per second since the monitor started listening.
     * 
     * @return the frame rate in units of frames per second.
     */
    public int getFps() {
        long elapsed = System.currentTimeMillis() - mStartTime;

        return (int) ((1000L * mFrameCount) / elapsed);
    }

    /**
     * Number of frames recieved since the last reset.
     * 
     * @return
     */
    public int getCount() {
        return mFrameCount;
    }

    /**
     * Get the number of milliseconds that have elapsed since reading the first
     * frame, or since the last reset.
     * 
     * @return
     */
    public long getTime() {
        return (mStartTime == 0) ? 0
                : (System.currentTimeMillis() - mStartTime);
    }

    /**
     * Reset the counts, and the timers to zero.
     */
    public void reset() {
        mFrameCount = 0;
        mStartTime = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        try {
            // create Options object
            options = new Options();

            // add options
            options.addOption(OptionBuilder.withArgName("w").withLongOpt(
                    "width").withDescription("image width").hasArg(true)
                    .isRequired(false).create("w"));
            options.addOption(OptionBuilder.withArgName("h").withLongOpt(
                    "height").withDescription("image height").hasArg()
                    .isRequired(false).create("h"));
            options.addOption(OptionBuilder.withArgName("f").withLongOpt("fps")
                    .withDescription("frames per second").hasArg().isRequired(
                            false).create("f"));

            // options.addOption(OptionBuilder.withArgName("m").withLongOpt("mjpeg")
            // .withDescription("mjpeg source file")
            // .hasArg().isRequired().create("m"));
            OptionGroup group = new OptionGroup();
            group.addOption(OptionBuilder.withLongOpt("mjpeg-file")
                    .withDescription("Axis MJPEG raw file").hasArg()
                    .create("m"));
            group.addOption(OptionBuilder.withLongOpt("camera-url")
                    .withDescription("Axis camera url").hasArg().isRequired()
                    .create("c"));
            group
                    .addOption(OptionBuilder.withLongOpt("jmf-url")
                            .withDescription(
                                    "jmf url like rtp:// or vfw:// or http://")
                            .hasArg().isRequired().create("j"));
            group.setRequired(true);
            options.addOptionGroup(group);

            CommandLine cmd = null;

            try {
                CommandLineParser parser = new BasicParser();
                cmd = parser.parse(options, args);
                // } catch (ParseException pe) {
            } catch (Exception pe) {
                if (pe instanceof MissingOptionException) {
                    System.out.println("missing required option: "
                            + pe.getMessage());
                } else if (pe instanceof UnrecognizedOptionException) {
                    System.out.println("unknown option: " + pe.getMessage());
                }

                printUsage();
            }
            // verify that com.eyeqinc.axis protocol prefix is registered
            // boolean test = DataSource.registerProtocolPrefix();
            InputStream in = null;

            try {
                if (group.getSelected().endsWith("j")) {
                    // use the JMF api to play the URL
                    String jmfUrlS = cmd.getOptionValue("j");
                    URL url = new URL(jmfUrlS);
                    in = new CameraAPI(url).getMjpegCgi(-1, -1, -1, -1, -1,
                            false, false, false, false, -1, true, -1, -1, -1,
                            false, false, -1);
                } else {
                    // validate parameters
                    if (!cmd.hasOption("w") || !cmd.hasOption("h")
                            || !cmd.hasOption("f")) {
                        System.out
                                .println("Missing one or more required options for Axis MJPEG: -w -h -f");
                        printUsage();
                    }

                    // set parameters
                    int videoWidth = Integer.parseInt(cmd.getOptionValue("w"));
                    int videoHeight = Integer.parseInt(cmd.getOptionValue("h"));
                    int fps = Integer.parseInt(cmd.getOptionValue("f"));

                    if (group.getSelected().endsWith("m")) {
                        // find the mjpeg file
                        String mjpegFile = cmd.getOptionValue("m");
                        in = new FileInputStream(mjpegFile);
                    } else if (group.getSelected().endsWith("c")) {
                        // use the camera api
                        String urlS = cmd.getOptionValue("c");
                        URL url = new URL(urlS);
                        in = new CameraAPI(url).getMjpegCgi(videoWidth,
                                videoHeight, -1, -1, -1, false, false, false,
                                false, -1, true, -1, -1, fps, true, false, -1);
                    }
                }
            } catch (FileNotFoundException fnfe) {
                System.out.println(fnfe.getMessage());
                System.exit(2);
            } catch (MalformedURLException murle) {
                System.out.println(murle.getMessage());
                System.exit(3);
            } catch (IOException ioe) {
                System.out.println("IO Error: " + ioe.getMessage());
                System.exit(4);
            }

            // begin the test
            FramerateMonitor monitor = new FramerateMonitor();
            MjpegFrameParser parser = new MjpegFrameParser(in);
            parser.addMjpegParserListener(monitor);
            parser.start(); // spawn thread

            // run for 60 seconds
            for (int i = 0; i < 60; i++) {
                while (true) {
                    Thread.sleep(1000);
                    System.out.println("count=" + monitor.getCount() + " FPS="
                            + monitor.getFps());
                    monitor.reset(); // reset the count
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    protected static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("FramerateMonitor", options);
        System.exit(1);
    }

}
