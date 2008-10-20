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

import java.net.MalformedURLException;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.MediaLocator;

/**
 * Describe an Axis207MW CaptureDevice.
 * 
 * @author David Bird
 */
public class Axis207MWCaptureDevice extends Axis207MCaptureDevice {
	/**
	 * The full product name as returned from an API query for the Axis
	 * parameter: root.BRAND.PRODFULLNAME
	 */
	public static String PRODUCT_FULL_NAME = "AXIS 207MW Network Camera";

	public Axis207MWCaptureDevice(MediaLocator locator)
			throws MalformedURLException {
		super(locator);
		DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;
		mDeviceName = DEFAULT_DEVICE_NAME;
	}

	public Axis207MWCaptureDevice(String deviceName, MediaLocator locator)
			throws MalformedURLException {
		super(deviceName, locator);
		DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;
		mDeviceName = DEFAULT_DEVICE_NAME;
	}

	public Axis207MWCaptureDevice(String deviceName, String hostName, int port) {
		super(deviceName, hostName, port);
		DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;
		mDeviceName = DEFAULT_DEVICE_NAME;
	}

	/**
	 * Register this device.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length != 2) {
				System.out.println("Usage: Axis207MWCaptureDevice HOST PORT");
				System.exit(1);
			}

			// fetch program arguments
			String host = args[0];
			int port = Integer.parseInt(args[1]);

			// add this device to the JMF registry
			Axis207MWCaptureDevice cam = new Axis207MWCaptureDevice(null, host,
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
