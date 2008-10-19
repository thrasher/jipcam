package net.sf.jipcam.axis;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Vector;

import javax.media.CaptureDeviceManager;

import junit.framework.TestCase;
import net.sf.jipcam.axis.tools.FrameExtract;

public class Axis2130CaptureDeviceTest extends TestCase {
	public Axis2130CaptureDeviceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		// Is JMF working?
		Vector devices = CaptureDeviceManager.getDeviceList(null);
		assertNotNull(devices);
		assertTrue(devices.size() > 0);

		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * dummy test for now
	 */
	public void testRegisterDevice() throws Exception {
		// TODO: test host and port should come from a properties file
		String name = "Test Axis 2130";
		String host = "axis2130.camera.net";
		int port = Integer.parseInt("8080");

		// but can we add the device?
		Axis2130CaptureDevice cam = new Axis2130CaptureDevice(name, host, port);
		assertTrue(cam.registerDevice());
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(CameraAPITest.class);
	}

}
