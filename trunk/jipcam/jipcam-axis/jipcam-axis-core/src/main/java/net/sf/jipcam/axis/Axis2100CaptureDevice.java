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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.control.FormatControl;
import javax.media.format.JPEGFormat;
import javax.media.protocol.CaptureDevice;

import net.sf.jipcam.axis.media.protocol.http.DataSource;

/**
 * Describe an Axis2100 CaptureDevice.
 * 
 * @author Jason Thrasher
 */
public class Axis2100CaptureDevice extends DataSource implements CaptureDevice {
	/**
	 * The full product name as returned from an API query for the Axis
	 * parameter: root.BRAND.PRODFULLNAME
	 */
	public static String PRODUCT_FULL_NAME = "AXIS 2100 Network Camera";

	public static String MODEL = "2100";

	protected static String DEFAULT_PROTOCOL = "http";

	protected static String DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;

	protected String mDeviceName = DEFAULT_DEVICE_NAME;

	/**
	 * The CaptureDeviceManager uses the device name as the key, so connections
	 * to more than one physical camera of the same type should use a
	 * constructor that uses the "deviceName" parameter. This will prevent
	 * multiple registrations, and make it easier to find the device.
	 * 
	 * @param locator
	 * @throws MalformedURLException
	 */
	public Axis2100CaptureDevice(MediaLocator locator)
			throws MalformedURLException {
		this(null, locator);
	}

	public Axis2100CaptureDevice(String deviceName, MediaLocator locator)
			throws MalformedURLException {
		this(deviceName, locator.getURL().getHost(), locator.getURL().getPort());
	}

	/**
	 * Create an Axis 2100 device. The device is initially disconnected.
	 * 
	 * @param name
	 * @param hostName
	 * @param port
	 */
	public Axis2100CaptureDevice(String deviceName, String hostName, int port) {
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
	 * Note: the default format used by JMF will be the Format at index "0". So,
	 * classes extending this one should put the most ideal default streamable
	 * format at index "0". This may be a lower resolution value than the
	 * maximum for the camera.
	 * 
	 * @return The CaptureDeviceInfo object that describes this device.
	 */
	public CaptureDeviceInfo getCaptureDeviceInfo() {
		// register the device
		Format[] formats = new Format[2];
		formats[0] = new JPEGFormat(new Dimension(320, 240),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[1] = new JPEGFormat(new Dimension(640, 480),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);

		return new CaptureDeviceInfo(mDeviceName, mLocator, formats);
	}

	/**
	 * Register this CaptureDevice in the JMFRegistry of available capture
	 * devices. This will allow the device to be found via it's NAME, and then
	 * it's formats selected as needed.
	 * 
	 * @return true if the object is added successfully, false if it is not.
	 * @throws IOException
	 *             If the registry could not be committed to disk due to an IO
	 *             error.
	 */
	public boolean registerDevice() throws IOException {
		boolean isRegistered = false;

		// get a list of all currently registered devices
		Vector devices = CaptureDeviceManager.getDeviceList(null);

		if (devices == null || devices.size() == 0) {
			// add the device, only if it doesn't already exist
			isRegistered = CaptureDeviceManager
					.addDevice(getCaptureDeviceInfo());

			// commit it to the registry
			CaptureDeviceManager.commit();
		} else {
			// is it already registered?
			CaptureDeviceInfo info = getCaptureDeviceInfo();// more efficient
			for (int i = 0; i < devices.size(); i++) {
				if (info.equals((CaptureDeviceInfo) devices.elementAt(i))) {
					isRegistered = true;
					break;
				}
			}

			// register it if not already in the registry
			if (!isRegistered) {
				// device not found, so register it
				// add the device, only if it doesn't already exist
				isRegistered = CaptureDeviceManager
						.addDevice(getCaptureDeviceInfo());

				// commit it to the registry
				CaptureDeviceManager.commit();
			}
		}

		return isRegistered;
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
	 * Resource bundles are used to retrieve the default properties used for any
	 * given camera. Each camera model number is treated as a different
	 * language.
	 * 
	 * @return the default properties for this camera
	 */
	public static Properties getPropertyDefaults() {
		String name = "net.sf.jipcam.axis.AxisCamera";
		ClassLoader loader = ClassLoader.getSystemClassLoader();

		// find the resource bundle
		ResourceBundle bundle;
		try {
			if (loader == null) {
				bundle = PropertyResourceBundle.getBundle(name, new Locale(
						MODEL));
			} else {
				bundle = PropertyResourceBundle.getBundle(name, new Locale(
						MODEL), loader);
			}
		} catch (MissingResourceException e) {
			// Then, try to load from context classloader
			bundle = PropertyResourceBundle.getBundle(name, new Locale(MODEL),
					Thread.currentThread().getContextClassLoader());
		}

		// convert the bundle into a properties object
		Properties defProps = new Properties();
		for (Enumeration<String> keys = bundle.getKeys(); keys
				.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			final String value = bundle.getString(key);

			defProps.put(key, value);
		}
		return defProps;
	}

	/**
	 * Register this device.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			// fetch program arguments
			String host = args[0];
			int port = Integer.parseInt(args[1]);

			// add this device to the JMF registry
			Axis2100CaptureDevice cam = new Axis2100CaptureDevice(null, host,
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
