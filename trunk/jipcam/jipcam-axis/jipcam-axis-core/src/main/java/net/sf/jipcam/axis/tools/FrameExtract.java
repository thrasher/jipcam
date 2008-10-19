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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import net.sf.jipcam.axis.CameraAPI;
import net.sf.jipcam.axis.MjpegFrameParser;
import net.sf.jipcam.axis.MjpegParserEvent;
import net.sf.jipcam.axis.MjpegParserListener;

/**
 * Utility for extracting sequential frames from an input stream. Command-line
 * wrapper allows URL, offset, and count arguments. Progress is reported with
 * "." and "w" characters.
 * 
 * @author Jason Thrasher
 */
public class FrameExtract implements MjpegParserListener {
    long mFrameNumber = 0; // number of frames read from the stream

    long mCount; // count of frames TO read from stream

    int mStart; // start frame number to copy

    // int mStop = 0;
    OutputStream mOut;

    boolean mStartedWriting = false;

    /**
     * Extract COUNT frames starting at STARTFRAME from the input stream and
     * write them to a new output stream.
     * 
     * @param out
     * @param count
     * @param startFrame
     * @param in
     */
    public FrameExtract(InputStream in, int startFrame, int count,
            OutputStream out) {
        mStart = startFrame;
        mCount = count;
        mOut = out;

        MjpegFrameParser parser = new MjpegFrameParser(in);
        parser.addMjpegParserListener(this);
        System.out.println("Reading " + mCount + " frames starting at frame "
                + mStart);
        parser.parse(); // blocking
    }

    public void onMjpegParserEvent(MjpegParserEvent event) {
        if ((mFrameNumber >= mStart) && (mFrameNumber < mStart + mCount)) {
            mStartedWriting = true;
            // write to file
            try {
                mOut.write(event.getMjpegFrame().getBytes());
                System.out.print("+");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            // flag to exit after we've captured the desired framecount
            if (mStartedWriting) {
                System.exit(0);
            }
            System.out.print("-");
        }

        mFrameNumber++;
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out
                    .println("Usage: java FrameExtract <url> <startframe> <count>");
            System.exit(1);
        }

        try {
            String mUrlS = args[0];
            int startFrame = Integer.parseInt(args[1]);
            int count = Integer.parseInt(args[2]);
            int stopFrame = startFrame + count;

            // where we're connecting to
            System.out.println("Opening connection to: " + mUrlS);
            URL url = new URL(mUrlS);

            // use the camera API to call for the MJPEG stream properly
            CameraAPI api = new CameraAPI(url);
            InputStream in = api.getMjpegCgi(-1, -1, -1, -1, -1, false, false,
                    false, false, -1, true, -1, -1, -1, true, true, -1);

            // what we're making
            String file;
            file = "frames_" + startFrame + "-" + stopFrame + ".mjpeg";

            System.out.println("Creating file: " + file);
            OutputStream out = new FileOutputStream(file);

            FrameExtract frameExtract = new FrameExtract(in, startFrame, count,
                    out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
