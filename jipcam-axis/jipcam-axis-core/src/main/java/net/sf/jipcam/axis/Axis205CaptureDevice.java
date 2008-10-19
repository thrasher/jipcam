/*
 * Axis205CaptureDevice.java (2007)
 *
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Unpublished - rights reserved under the Copyright Laws of the United States.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this document. In
 * particular, and without limitation, these intellectual property rights may
 * include one or more of the U.S. patents listed at http://www.sun.com/patents
 * and one or more additional patents or pending patent applications in the
 * U.S. and in other countries.
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 *
 * U.S. Government Rights - Commercial software. Government users are subject
 * to the Sun Microsystems, Inc. standard license agreement and applicable
 * provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties. Sun, Sun
 * Microsystems, the Sun logo, Java, Jini, Solaris and Sun Ray are trademarks
 * or registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively
 * licensed through X/Open Company, Ltd.
 */

/**
 * This code based on:
 *
 * jipCam : The Java IP Camera Project
 * Copyright (C) 2005-2007 Jason Thrasher
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

import net.sf.jipcam.axis.media.protocol.http.DataSource;

import java.awt.Dimension;

import java.io.IOException;

import java.net.MalformedURLException;

import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.control.FormatControl;
import javax.media.format.JPEGFormat;
import javax.media.protocol.CaptureDevice;

/**
 * Describe an Axis205 CaptureDevice.
 * 
 * @author Nigel Simpson
 */
public class Axis205CaptureDevice extends DataSource implements CaptureDevice {
	/**
	 * The full product name as returned from an API query for the Axis
	 * parameter: root.BRAND.PRODFULLNAME
	 */
	public static String PRODUCT_FULL_NAME = "AXIS 205 Network Camera";
	protected static String DEFAULT_PROTOCOL = "http";
	protected static String DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;
	protected String mDeviceName = DEFAULT_DEVICE_NAME;

	public Axis205CaptureDevice(MediaLocator locator)
			throws MalformedURLException {
		this(null, locator);
	}

	public Axis205CaptureDevice(String deviceName, MediaLocator locator)
			throws MalformedURLException {
		this(deviceName, locator.getURL().getHost(), locator.getURL().getPort());
	}

	/**
	 * Create an Axis 206M device. The device is initially disconnected.
	 * 
	 * @param name
	 * @param hostName
	 * @param port
	 */
	public Axis205CaptureDevice(String deviceName, String hostName, int port) {
		super(
				new MediaLocator(DEFAULT_PROTOCOL + "://" + hostName + ":"
						+ port));

		// validate the name
		if (deviceName != null) {
			mDeviceName = deviceName;
		}
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
		// 640x480,320x240,160x120
		Format[] formats = new Format[3];
		formats[0] = new JPEGFormat(new Dimension(160, 120),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[1] = new JPEGFormat(new Dimension(320, 240),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[2] = new JPEGFormat(new Dimension(640, 480),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);

		return new CaptureDeviceInfo(mDeviceName, mLocator, formats);
	}

	/**
	 * Register this CaptureDevice in the JMFRegistry of available capture
	 * devices. This will allow the device to be found via it's NAME, and then
	 * it's formats selected as needed.
	 * 
	 * @throws IOException
	 */
	public void registerDevice() throws IOException {
		// add the device
		CaptureDeviceManager.addDevice(getCaptureDeviceInfo());

		// commit it to the registry
		CaptureDeviceManager.commit();
	}

	/**
	 * Returns an array of FormatControl objects. Each of them can be used to
	 * set and get the format of each capture stream. This method can be used
	 * before connect to set and get the capture formats.
	 */
	public FormatControl[] getFormatControls() {
		return new FormatControl[] { mFormatControl };
	}

	/**
	 * Register this device.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length != 2) {
				System.out.println("Usage: Axis205CaptureDevice HOST PORT");
				System.exit(1);
			}
			
			// fetch program arguments
			String host = args[0];
			int port = Integer.parseInt(args[1]);

			// add this device to the JMF registry
			Axis205CaptureDevice cam = new Axis205CaptureDevice(null, host,
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
