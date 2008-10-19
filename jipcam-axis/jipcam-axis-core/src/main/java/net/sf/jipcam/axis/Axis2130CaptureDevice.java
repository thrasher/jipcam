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
 * Axis 2130 supports: 704x480, 352x240 It doesn't seem to support 176x112 very
 * well... or we can't get it to work.
 * 
 * @author Jason Thrasher <a
 *         href="mailto:jason@coachthrasher.com">jason@coachthrasher.com</a>
 */
public class Axis2130CaptureDevice extends Axis2100CaptureDevice {
	public static String PRODUCT_FULL_NAME = "AXIS 2130 PTZ Network Camera";

	/**
	 * root.BRAND.PRODNBR="2130"
	 */
	public static String MODEL = "2130";

	protected static String DEFAULT_DEVICE_NAME = PRODUCT_FULL_NAME;

	protected String mDeviceName = DEFAULT_DEVICE_NAME;

	public Axis2130CaptureDevice(MediaLocator locator)
			throws MalformedURLException {
		super(locator);
	}

	public Axis2130CaptureDevice(String deviceName, MediaLocator locator)
			throws MalformedURLException {
		super(deviceName, locator);
	}

	public Axis2130CaptureDevice(String deviceName, String hostName, int port) {
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
		Format[] formats = new Format[3];
		formats[0] = new JPEGFormat(new Dimension(704, 480),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		formats[1] = new JPEGFormat(new Dimension(352, 240),
				Format.NOT_SPECIFIED, Format.byteArray, (float) 30,
				Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);
		// formats[2] = new JPEGFormat(new Dimension(176, 112),
		// Format.NOT_SPECIFIED, Format.byteArray, (float) 10,
		// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED);

		return new CaptureDeviceInfo(mDeviceName, mLocator, formats);
	}

	/**
	 * testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length != 2) {
				System.out.println("Usage: Axis2130CaptureDevice HOST PORT");
				System.exit(1);
			}

			// fetch program arguments
			String host = args[0];
			int port = Integer.parseInt(args[1]);

			// add this device to the JMF registry
			Axis2130CaptureDevice cam = new Axis2130CaptureDevice(null, host,
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
