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

package net.sf.jipcam.axis;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;


/**
 * An input stream with a built-in parser for MjpegFrame data structures.
 * This parser attempts to optimize reads from the input stream by taking
 * advantage of the Content-Length MJPEG frame header, if it exists.
 *
 * This class should be used as a direct parser for MJPEG streams from files
 * or the network.  On a P3, 1.8 GHz CPU each 240x352 pixel frame takes less
 * than 1 ms to parse in optimized mode, or about 3 ms to parse if the
 * Content-Length can't be found.  This is only a rough estimate of performance!
 *
 * Warning: if the underlying stream is read by any other process it will cause
 * frame data loss by this parser, and possible jpeg data corruption.  No other
 * threads should be reading from the stream that is wrapped by this class.
 *
 * @author Jason Thrasher
 */
public class MjpegInputStream extends DataInputStream {
	private static Logger mLog = Logger.getLogger(MjpegInputStream.class); //logging mechanism
	protected int mSequence = 0;
	protected int mContentLength = -1;
	protected boolean isContentLengthAvailable = false;
	protected boolean isFirstPass = true;

	/**
	* Wrap the given input stream with the MjpegInputStream.  Internal buffers
	 * are created automatially.
	 *
	* @param in stream to read and parse MJPEG frames from
	*/
	public MjpegInputStream(InputStream in) {
		super(new BufferedInputStream(in, MjpegFormat.FRAME_MAX_LENGTH));
	}

	/**
	* Read the next MjpegFrame from the stream.
	 *
	* @return the next MJPEG frame.
	* @throws IOException if there is an error while reading data
	*/
	public MjpegFrame readMjpegFrame() throws IOException {
		//mark the start of the frame
		mark(MjpegFormat.FRAME_MAX_LENGTH);

		//get length of header
		int headerLen = MjpegFormat.getStartOfSequence(this,
				JpegFormat.SOI_MARKER); //position of first byte of the jpeg

		if (isFirstPass) {
			//attempt to parse content length
			isFirstPass = false; //do this once
			reset();

			byte[] header = new byte[headerLen];
			readFully(header);

			try {
				mContentLength = MjpegFormat.parseContentLength(header);
				isContentLengthAvailable = true; //flag for more efficientcy
			} catch (NumberFormatException nfe) {
				mLog.warn(
					"couldn't parse content length from header on first pass");
				isContentLengthAvailable = false;
			}
		}

		reset();

		if (isContentLengthAvailable) {
			//the fast way
			byte[] header = new byte[headerLen];
			readFully(header);

			try {
				mContentLength = MjpegFormat.parseContentLength(header);
			} catch (NullPointerException npe) {
				mLog.warn(
					"couldn't parse content length, failover to jpeg EOF search");
				mContentLength = MjpegFormat.getEndOfSeqeunce(this,
						JpegFormat.EOF_MARKER); //position of first byte after the jpeg
			}

			//(JpegFormat.EOF_MARKER); //position of first byte after the jpeg
		} else {
			//the slow way, because we have to test (if/then) every byte!
			mContentLength = MjpegFormat.getEndOfSeqeunce(this,
					JpegFormat.EOF_MARKER); //position of first byte AFTER the jpeg
		}

		//create frame array
		byte[] frameData = new byte[headerLen + mContentLength];
		reset();
		readFully(frameData);

		return new MjpegFrame(frameData, mContentLength, mSequence++);
	}

	/**
	* Unit test against an MJPG raw file from the Axis camera.
	 *
	* @param args
	*/
	public static void main(String[] args) {
		long elapsed;
		long start = System.currentTimeMillis();
		int count = 0;

		try {
			File file = new File("D:/dev/axis/data/lost_girl.mjpeg");
			MjpegInputStream in = new MjpegInputStream(new FileInputStream(file));
			MjpegFrame frame = null;

			while ((frame = in.readMjpegFrame()) != null) {
				FileOutputStream out = new FileOutputStream(new File("jpeg-" +
							count + ".jpg"));
				out.write(frame.getJpegBytes());
				out.close();
				count++;
			}
		} catch (EOFException eof) {
			System.out.println("reached end of file");
		} catch (Exception e) {
			e.printStackTrace();
		}

		elapsed = System.currentTimeMillis() - start;
		System.out.println("elapsed = " + elapsed);
		System.out.println("frame count = " + count);
		System.out.println("fps = " + ((count * 1000) / elapsed));
	}
}
