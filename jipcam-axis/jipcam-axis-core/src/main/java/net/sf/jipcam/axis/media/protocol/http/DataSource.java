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

package net.sf.jipcam.axis.media.protocol.http;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Vector;

import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.PackageManager;
import javax.media.Time;
import javax.media.format.JPEGFormat;
import javax.media.protocol.CaptureDevice;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;

import net.sf.jipcam.axis.Axis205CaptureDevice;
import net.sf.jipcam.axis.Axis206MCaptureDevice;
import net.sf.jipcam.axis.Axis207CaptureDevice;
import net.sf.jipcam.axis.Axis207MCaptureDevice;
import net.sf.jipcam.axis.Axis207MWCaptureDevice;
import net.sf.jipcam.axis.Axis2100CaptureDevice;
import net.sf.jipcam.axis.Axis2120CaptureDevice;
import net.sf.jipcam.axis.Axis2130CaptureDevice;
import net.sf.jipcam.axis.CameraAPI;

import org.apache.log4j.Logger;

/**
 * A DataSource that reads Motion JPEG data into the JMF architecture. Only the
 * Video stream is supported.
 * 
 * @author Jason Thrasher <a
 *         href="mailto:jason@coachthrasher.com">jason@coachthrasher.com</a>
 * 
 */
public class DataSource extends PullBufferDataSource {
	private MjpegStream[] streams; // only contains one video stream
	protected MediaLocator mLocator = null; // source locator
	private InputStream mIn = null;
	protected MjpegFormatControl mFormatControl;
	private static Logger mLog = Logger.getLogger(DataSource.class);
	/**
	 * JMF plugin identifier
	 */
	public static final String PACKAGE_PROTOCOL_PREFIX = "net.sf.jipcam.axis";

	// Camera API parameters
	protected String mApiUsername = null;
	protected String mApiPassword = null;

	/**
	 * Empty constructor is used when locating the DS via
	 * Manager.createDataSource([MediaLocator | URL]).
	 */
	public DataSource() {
		streams = new MjpegStream[1]; // only one stream is supported
		mFormatControl = new MjpegFormatControl(); // create an empty control
	}

	public DataSource(int width, int height, int frameRate, InputStream in) {
		this();

		// override the format with info supplied
		// mFormatControl = new MjpegFormatControl();
		mFormatControl.addFormat(new JPEGFormat(new Dimension(width, height),
				Format.NOT_SPECIFIED, Format.byteArray, (float) frameRate,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED));

		// if the user is supplying the inputstream we have to trust that the
		// h/w/fps is correct
		mIn = in;
	}

	public DataSource(MediaLocator source) {
		this();
		setLocator(source);
	}

	public void setLocator(MediaLocator source) {
		mLocator = source;
	}

	public MediaLocator getLocator() {
		return mLocator;
	}

	/**
	 * Content type is of RAW since we are sending buffers of video frames
	 * without a container format.
	 */
	public String getContentType() {
		return ContentDescriptor.RAW;
	}

	/**
	 * Open a connection to the source described by the MediaLocator. The
	 * connect method initiates communication with the source. Note that if
	 * Manager.createDataSource(MediaLocator) is used to create this datasource,
	 * it will return a datasource that is already connected - in other words,
	 * this method will have been called.
	 * 
	 * @throws IOException
	 *             if there's a connection problem
	 */
	public void connect() throws IOException {
		// if useing a media locator
		if ((mLocator != null)
				&& mLocator.getProtocol().equalsIgnoreCase("http")) {
			try {
				CameraAPI api = new CameraAPI(mLocator.getURL(), -1,
						mApiUsername, mApiPassword);

				// determine the currently set format
				JPEGFormat f = (JPEGFormat) mFormatControl.getFormat();

				// determine what Format the data is available in
				if (f == null) {
					// based on camera type, determine the formats permitted
					Format[] formats = null;

					if (this instanceof CaptureDevice) {
						// a capture device knows it's formats, find them!
						formats = ((CaptureDevice) this).getCaptureDeviceInfo()
								.getFormats();
					} else {
						// query for product type
						try {
							CaptureDevice device = null;
							String prodName = api.getProductFullName();

							if (prodName != null) {
								System.err.println(prodName);
								if (prodName
										.equalsIgnoreCase(Axis2100CaptureDevice.PRODUCT_FULL_NAME)) {
									device = new Axis2100CaptureDevice(mLocator);
									formats = device.getCaptureDeviceInfo()
											.getFormats();
								} else if (prodName
										.equalsIgnoreCase(Axis2120CaptureDevice.PRODUCT_FULL_NAME)) {
									device = new Axis2120CaptureDevice(mLocator);
									formats = device.getCaptureDeviceInfo()
											.getFormats();
								} else if (prodName
										.equalsIgnoreCase(Axis2130CaptureDevice.PRODUCT_FULL_NAME)) {
									device = new Axis2130CaptureDevice(mLocator);
									formats = device.getCaptureDeviceInfo()
											.getFormats();
								} else if (prodName
										.equalsIgnoreCase(Axis206MCaptureDevice.PRODUCT_FULL_NAME)) {
									device = new Axis206MCaptureDevice(mLocator);
									formats = device.getCaptureDeviceInfo()
											.getFormats();
								} else if (prodName
										.equalsIgnoreCase(Axis207CaptureDevice.PRODUCT_FULL_NAME)) {
									device = new Axis207CaptureDevice(mLocator);
									formats = device.getCaptureDeviceInfo()
											.getFormats();
								} else if (prodName
										.equalsIgnoreCase(Axis207MCaptureDevice.PRODUCT_FULL_NAME)) {
									device = new Axis207MCaptureDevice(mLocator);
									formats = device.getCaptureDeviceInfo()
											.getFormats();
								} else if (prodName
										.equalsIgnoreCase(Axis207MWCaptureDevice.PRODUCT_FULL_NAME)) {
									device = new Axis207MWCaptureDevice(
											mLocator);
									formats = device.getCaptureDeviceInfo()
											.getFormats();
								} else if (prodName
										.equalsIgnoreCase(Axis205CaptureDevice.PRODUCT_FULL_NAME)) {
									device = new Axis205CaptureDevice(mLocator);
									formats = device.getCaptureDeviceInfo()
											.getFormats();
								}
							}
						} catch (Exception e) {
							// ignore if this fails
							e.printStackTrace();
						}
					}

					// add formats to the control
					if (formats != null) {
						// set the available formats
						for (int i = 0; i < formats.length; i++) {
							mFormatControl.addFormat(formats[i]);
						}
					} else {
						// last resort, use a generic JPEG format
						System.err.println("Using default formats.");
						Dimension d = api.getImageDimension();

						// create a very generic JPEGFormat
						f = new JPEGFormat(d, Format.NOT_SPECIFIED,
								Format.byteArray, (float) Format.NOT_SPECIFIED,
								Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);

						// set the current format to camera's default
						mFormatControl.addFormat(f);
					}

					// finally, use the default
					f = (JPEGFormat) mFormatControl.getFormat();
				}

				// create an inputstream requesting the current format
				mIn = api.getMjpegCgi(f.getSize().width, f.getSize().height,
						-1, -1, -1, false, false, false, false, -1, true, -1,
						-1, (int) f.getFrameRate(), true, true, -1);
			} catch (MalformedURLException murle) {
				throw new IOException("Bad MediaLocator URL: "
						+ mLocator.toExternalForm());
			} catch (Exception e) {
				// check for HTTP errors (bad format?) and log as needed
				throw new IOException(e.getMessage());
			}
		}

		// set the actual stream here
		streams[0] = new MjpegStream(mIn, mFormatControl.getFormat());
	}

	/**
	 * Disconnect from the datasource and clean up.
	 */
	public void disconnect() {
		try {
			// only close the stream if it was opened from a MediaLocator
			if ((mIn != null) && (mLocator != null)) {
				mIn.close();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void start() throws IOException {
	}

	public void stop() {
	}

	/**
	 * Return the ImageSourceStreams.
	 */
	public PullBufferStream[] getStreams() {
		return streams;
	}

	/**
	 * We could have derived the duration from the number of frames and frame
	 * rate. But for the purpose of this program, it's not necessary.
	 */
	public Time getDuration() {
		return DURATION_UNKNOWN;
	}

	public Object[] getControls() {
		return new Object[] { mFormatControl };
	}

	public Object getControl(String type) {
		if (type.equals("javax.media.control.FormatControl")) {
			return mFormatControl;
		}

		return null;
	}

	public void setUsername(String username) {
		mApiUsername = username;
	}

	public void setPassword(String password) {
		mApiPassword = password;
	}

	/**
	 * Register the protocol prefix in JMF that will allow this class to be
	 * discovered via a MediaLocator. The prefix is NOT commited. To commit it,
	 * use: PackageManager.commitProtocolPrefixList();
	 * 
	 * @return true if the package prefix is available, false otherwise
	 */
	public static boolean registerProtocolPrefix() {
		mLog.debug("Attempting to register Axis protocol handler: "
				+ PACKAGE_PROTOCOL_PREFIX);
		// unchecked cast on compile is OK!
		Vector packagePrefix = PackageManager.getProtocolPrefixList();

		if (!packagePrefix.contains(PACKAGE_PROTOCOL_PREFIX)) {
			mLog.debug("Adding new Package Protocol Prefix: "
					+ PACKAGE_PROTOCOL_PREFIX);
			// Add new package prefix to end of the package prefix list.
			packagePrefix.addElement(PACKAGE_PROTOCOL_PREFIX);
			PackageManager.setProtocolPrefixList(packagePrefix);

			// Save the changes to the package prefix list.
			PackageManager.commitProtocolPrefixList();
		} else {
			mLog.debug("Not adding new Package Protocol Prefix: "
					+ PACKAGE_PROTOCOL_PREFIX);
		}

		// unchecked cast on compile is OK!
		packagePrefix = PackageManager.getProtocolPrefixList();

		// return packagePrefix.contains(PACKAGE_PROTOCOL_PREFIX);
		return false;
	}

	public static void main(String[] args) {
		try {
			// unchecked cast on compile is OK!
			Vector packagePrefix = PackageManager.getProtocolPrefixList();

			String mjpeg = new String("com.eyeqinc.axis");

			if (!packagePrefix.contains(mjpeg)) {
				System.out.println("Adding new Package Protocol Prefix: "
						+ mjpeg);

				// Add new package prefix to end of the package prefix list.
				packagePrefix.addElement(mjpeg);
				PackageManager.setProtocolPrefixList(packagePrefix);

				// Save the changes to the package prefix list.
				PackageManager.commitProtocolPrefixList();
			} else {
				System.out.println("Not adding new Package Protocol Prefix: "
						+ mjpeg);
			}

			// print out registered packages
			for (int i = 0; i < packagePrefix.size(); i++) {
				System.out.println("Registered Package Protocol Prefix: "
						+ packagePrefix.get(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
