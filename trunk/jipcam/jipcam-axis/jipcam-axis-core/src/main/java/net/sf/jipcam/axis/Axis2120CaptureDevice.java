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

import java.awt.Dimension;
import java.net.MalformedURLException;

import javax.media.CaptureDeviceInfo;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.format.JPEGFormat;

/**
 * Define an Axis 2120 camera.
 * 
 * http://www2.axis.com/files/datasheet/2120/2120ds.pdf
 * 
 * @author Jason Thrasher
 */
public class Axis2120CaptureDevice extends Axis2100CaptureDevice {
	public static String PRODUCT_FULL_NAME = "AXIS 2120 Network Camera";

	public static String MODEL = "2120";

	public Axis2120CaptureDevice(MediaLocator locator)
			throws MalformedURLException {
		super(locator);
	}

	public Axis2120CaptureDevice(String deviceName, MediaLocator locator)
			throws MalformedURLException {
		super(deviceName, locator);
	}

	public Axis2120CaptureDevice(String deviceName, String hostName, int port) {
		super(deviceName, hostName, port);
	}

	/**
	 * Return the CaptureDeviceInfo object that describes this device. The
	 * device's supported formats are determined from this information. Not all
	 * supported formats are returned, only a set of ideal formats.
	 * 
	 * @return The CaptureDeviceInfo object that describes this device.
	 */
	public CaptureDeviceInfo getCaptureDeviceInfo() {
		// register the device
		Format[] formats = new Format[2];
		formats[0] = new JPEGFormat(new Dimension(352, 240),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 30,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[1] = new JPEGFormat(new Dimension(704, 480),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);

		return new CaptureDeviceInfo(mDeviceName, mLocator, formats);
	}
}
