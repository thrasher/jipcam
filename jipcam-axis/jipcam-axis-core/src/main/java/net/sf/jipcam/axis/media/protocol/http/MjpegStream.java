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

package net.sf.jipcam.axis.media.protocol.http;

import java.io.IOException;
import java.io.InputStream;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferStream;

import net.sf.jipcam.axis.MjpegFrame;
import net.sf.jipcam.axis.MjpegInputStream;


/**
 * Entry point of MJPEG frame data into the JMF system.  A parser is created
 * on the given InputStream that will read MjpegFrame data.
 *
 * @author Jason Thrasher
 */
public class MjpegStream implements PullBufferStream {
	MjpegInputStream mIn = null;
	Format mFormat;
	boolean ended = false;

	/**
	* Create the Motion JPEG stream from the given inputstream and referenceing
	 * the given format.  Note that the format is passed here for reference only
	 * and is not used to modify the inputstream to comply with the format.
	 *
	* @param in
	* @param format
	*/
	public MjpegStream(InputStream in, Format format) {
		mIn = new MjpegInputStream(in);
		mFormat = format;
	}

	/**
	 * Find out if data is available now. Returns true if a call to read would block for data.
	 * @return true if a call to read would block for data.
	 */
	public boolean willReadBlock() {
		return false;
	}

	/**
	 * Block and read a buffer from the stream. buffer should be non-null.
	 * This is called from the Processor to read a frame worth of video data.
	* @param buf to put the raw data into
	* @throws IOException Thrown if an error occurs while reading.
	*/
	public void read(Buffer buf) throws IOException {
		MjpegFrame frame;

		// Check if we've finished all the frames.
		try {
			frame = mIn.readMjpegFrame();
		} catch (IOException ioe) {
			// We are done.  Set EndOfMedia.
			System.err.println("Done reading all images. " + ioe.getMessage());
			buf.setEOM(true);
			buf.setOffset(0);
			buf.setLength(0);
			ended = true; //set flag for method

			return;
		}

		byte[] data = null; //a pointer

		// Check the input buffer type & size.
		if (buf.getData() instanceof byte[]) {
			data = (byte[]) buf.getData(); //set the pointer
		}

		// Check to see the given buffer is big enough for the frame.
		if ((data == null) || (data.length < frame.getContentLength())) {
			data = new byte[(int) frame.getContentLength()];
			buf.setData(data);
		}

		// Read the entire JPEG image from the frame.
		System.arraycopy(frame.getJpegBytes(), 0, data, 0,
			frame.getContentLength());
		buf.setOffset(0);
		buf.setLength(frame.getContentLength());
		buf.setFormat(mFormat);
		buf.setFlags(buf.getFlags() | buf.FLAG_KEY_FRAME);
		buf.setSequenceNumber(frame.getSequence());
	}

	/**
	 * Get the format type of the data that this source stream provides.
	*
	* @return a JPEGFormat
	*/
	public Format getFormat() {
		return mFormat;
	}

	public ContentDescriptor getContentDescriptor() {
		return new ContentDescriptor(ContentDescriptor.RAW);
	}

	public long getContentLength() {
		return LENGTH_UNKNOWN;
	}

	public boolean endOfStream() {
		return ended;
	}

	public Object[] getControls() {
		return new Object[0];
	}

	public Object getControl(String type) {
		return null;
	}
}
