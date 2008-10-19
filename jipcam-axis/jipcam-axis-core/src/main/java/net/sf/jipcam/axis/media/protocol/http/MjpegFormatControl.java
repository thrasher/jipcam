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

import java.awt.Component;
import java.util.ArrayList;

import javax.media.Format;
import javax.media.control.FormatControl;
import javax.media.format.JPEGFormat;
import javax.media.format.VideoFormat;


/**
 * Format control class for MJPEG CaptureDevices, and DataSources.
 *
 * @author Jason Thrasher
 */
public class MjpegFormatControl implements FormatControl {
	//	private Format[] mFormats = new Format[2];
	private ArrayList mFormatList = new ArrayList();

	//	private int mFmtIdx = 0; //format index - the currently configured format
	private boolean mIsTrackEnabled = true; //is this track enabled?
	private Format mCurrentFormat = null; //the current format

	public MjpegFormatControl() {
	}

	/**
	 * Add a format to this control's list of supported formats.
	* The first call to add a format will set the default format to the same one.
	* @param format
	*/
	public void addFormat(Format format) {
		//reject a bad format
		if (format == null) {
			return;
		}

		//only add unique formats
		if (!mFormatList.contains(format)) {
			mFormatList.add(format);
		}

		//set default, if not already done
		if (mCurrentFormat == null) {
			mCurrentFormat = format;
		}
	}

	/**
	 * Get the Component associated with this Control object.
	 * For example, this method might return a slider for volume control or
	 * a panel containing radio buttons for CODEC control.
	 * The getControlComponent method can return null if there is no
	 * GUI control for this Control.
	 */
	public Component getControlComponent() {
		return null; //no gui component
	}

	/**
	 * Obtain the format that this object is set to.
	 * @return the current format.
	 */
	public Format getFormat() {
		return mCurrentFormat;
	}

	/**
	 * Sets the data format.
	 * The method returns null if the format is not supported.
	 * Otherwise, it returns the format that's actually set.
	 * <p>
	 * However in some situations, returning a non-null
	 * format does not necessarily mean that the format is supported
	 * since determining the supported formats may be state dependent
	 * or simply too costly.  In such cases, the setFormat call will
	 * succeed but the object may fail in another state-transition
	 * operation such as when the object is being initialized.
	 * <p>
	 * Alternatively, the getSupportedFormats method can be used to
	 * query for the list of supported formats.  The resulting list
	 * can be used to screen for the valid formats before setting
	 * that on the object.
	 *
	 * @return null if the format is not supported; otherwise return
	 * the format that's actually set.
	 */
	public Format setFormat(Format format) {
		if (format instanceof VideoFormat) {
			VideoFormat vf = (VideoFormat) format;

			//first pass, check for equality
			for (int i = 0; i < mFormatList.size(); i++) {
				if (((JPEGFormat) mFormatList.get(i)).equals(vf)) {
					//					mFmtIdx = i;
					mCurrentFormat = (JPEGFormat) mFormatList.get(i);

					return getFormat();
				}
			}

			//second pass, first match wins
			for (int i = 0; i < mFormatList.size(); i++) {
				if (((JPEGFormat) mFormatList.get(i)).matches(vf)) {
					//					mFmtIdx = i;
					mCurrentFormat = (JPEGFormat) mFormatList.get(i);

					return getFormat();
				}
			}
		}

		//default, format has not been changed
		return getFormat();
	}

	/**
	 * Lists the possible input formats supported by this plug-in.
	 * @return an array of the supported formats
	 */
	public Format[] getSupportedFormats() {
		Format[] formats = new Format[mFormatList.size()];
		System.arraycopy(mFormatList.toArray(), 0, formats, 0, formats.length);

		return formats;
	}

	/**
	 * Return the state of the track.
	 * @return A boolean telling whether or not the track is enabled.
	 */
	public boolean isEnabled() {
		return mIsTrackEnabled;
	}

	/**
	 * Enable or disable the track.
	 * @param enabled true if the track is to be enabled.
	 */
	public void setEnabled(boolean enabled) {
		mIsTrackEnabled = enabled;
	}
}
