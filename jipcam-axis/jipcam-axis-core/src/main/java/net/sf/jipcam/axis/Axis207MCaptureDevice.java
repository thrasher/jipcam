/**
 * jipCam : The Java IP Camera Project
 * Copyright (C) 2005-2008 Jason Thrasher
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
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.format.JPEGFormat;

/**
 * Describe an Axis207M CaptureDevice.
 * 
 * @author David Bird
 */
public class Axis207MCaptureDevice extends Axis207CaptureDevice {
	/**
	 * The full product name as returned from an API query for the Axis
	 * parameter: root.BRAND.PRODFULLNAME
	 */
	public static String PRODUCT_FULL_NAME = "AXIS 207M Network Camera";

	public Axis207MCaptureDevice(MediaLocator locator)
			throws MalformedURLException {
		super(locator);
		DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;
		mDeviceName = DEFAULT_DEVICE_NAME;
	}

	public Axis207MCaptureDevice(String deviceName, MediaLocator locator)
			throws MalformedURLException {
		super(deviceName, locator);
		DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;
		mDeviceName = DEFAULT_DEVICE_NAME;
	}

	public Axis207MCaptureDevice(String deviceName, String hostName, int port) {
		super(deviceName, hostName, port);
		DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;
		mDeviceName = DEFAULT_DEVICE_NAME;
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
		// 1280x1024,1280x960,1280x720,640x480,640x360,320x240
		Format[] formats = new Format[11];
		formats[0] = new JPEGFormat(new Dimension(240, 180),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[1] = new JPEGFormat(new Dimension(320, 180),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[2] = new JPEGFormat(new Dimension(320, 240),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[3] = new JPEGFormat(new Dimension(480, 270),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[4] = new JPEGFormat(new Dimension(480, 360),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[5] = new JPEGFormat(new Dimension(640, 360),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[6] = new JPEGFormat(new Dimension(640, 480),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[7] = new JPEGFormat(new Dimension(1280, 480),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[8] = new JPEGFormat(new Dimension(1280, 720),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[9] = new JPEGFormat(new Dimension(1280, 960),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[10] = new JPEGFormat(new Dimension(1280, 1024),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);

		return new CaptureDeviceInfo(mDeviceName, mLocator, formats);
	}

	/**
	 * Register this device.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length != 2) {
				System.out.println("Usage: Axis207MCaptureDevice HOST PORT");
				System.exit(1);
			}

			// fetch program arguments
			String host = args[0];
			int port = Integer.parseInt(args[1]);

			// add this device to the JMF registry
			Axis207MCaptureDevice cam = new Axis207MCaptureDevice(null, host,
					port);
			cam.registerDevice(); // add this device to the JMF registry

			// get all the capture devices
			Vector devices = CaptureDeviceManager.getDeviceList(null);
			CaptureDeviceInfo cdi;

			if ((devices != null) && (devices.size() > 0)) {
				int deviceCount = devices.size();

				for (int i = 0; i < deviceCount; i++) {
					cdi = (CaptureDeviceInfo) devices.elementAt(i);
					System.out.println(cdi.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
