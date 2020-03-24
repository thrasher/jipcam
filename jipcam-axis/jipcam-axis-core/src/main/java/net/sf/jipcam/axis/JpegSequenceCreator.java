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

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.NumberFormat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Listen for new MJPEG frames, and save them as a sequence of JPEG files.
 *
 * @author Jason Thrasher
 */
public class JpegSequenceCreator implements MjpegParserListener {
	private int mCount = 0;
	private String mPrefix = ""; //prefix for all jpeg files
	private int mMaxImages = 0; //max count of images to parse
	private NumberFormat mFormat; //count formatter
	private Logger mLog;

	/**
	 * Create a new JPEG file writing sequencer.
	 *
	 * @param maxImages estimated to parse - this is used to character-pad the output filenames.  Use -1 if the image count is unknown.
	 * @param filePrefix to use for all jpeg images.
	 */
	public JpegSequenceCreator(String filePrefix, int maxImages) {
		mLog = LogManager.getLogger(this.getClass());

		mPrefix = filePrefix;

		mMaxImages = maxImages;

		mFormat = NumberFormat.getNumberInstance(); //.getIntegerInstance();

		if (mMaxImages >= 0) {
			mFormat.setMinimumIntegerDigits(Integer.toString(maxImages).length());
			mFormat.setMaximumIntegerDigits(Integer.toString(maxImages).length());
		}

		mLog.log(Level.DEBUG, "ready to create JPEG files");
	}

	/*
	 * Listen for async events from the parser and write them to files
	 * @param frame
	 */
	public void onMjpegParserEvent(MjpegParserEvent event) {
		//manage counts, break if needed
		mCount++;

		if ((mMaxImages != -1) && (mCount > mMaxImages)) {
			mLog.log(Level.WARN, "Max files reached, ignoring JPEG data");

			return;
		}

		String num = mFormat.format(mCount);
		String nextFile = mPrefix + num + ".jpg";
		mLog.log(Level.INFO, "creating " + nextFile);

		try {
			OutputStream jpegOut = new FileOutputStream(nextFile);

			jpegOut.write(event.getMjpegFrame().getJpegBytes());

			jpegOut.close();
		} catch (Exception e) {
			e.printStackTrace(); //catch-all
		}
	}
}
