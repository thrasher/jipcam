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

package net.sf.jipcam.axis.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;

/**
 * This is a Java Applet that demonstrates how to create a simple media player
 * with a media event listener. It will play the media clip right away and
 * continuously loop.
 * 
 * <!-- Sample HTML <applet code="MjpegApplet" width="352" height="240"> <param
 * name="mjpeg" value="/axis-cgi/mjpg/video.cgi?req_fps=10"> <param name="width"
 * value="352"> <param name="height" value="240"> <param name="fps" value="10">
 * </applet> -->
 */
public class MjpegApplet extends JApplet {
	// boolean firstTime = true;
	// long CachingSize = 0L;
	// Panel panel = null;
	// int controlPanelHeight = 0;
	int videoWidth = 352;

	int videoHeight = 240;

	int mFps = 10; // frames per second

	URL mUrl = null;
	
	InputStream in = null;	// the data stream from the camera

	AxisPlayer mPlayer;

	JPanel mMainPanel = new JPanel(new BorderLayout(0, 0));

	public MjpegApplet() {
		System.out.println("MjpegApplet constructed.");

		// empty constructor for use as Applet
	}

	public MjpegApplet(int width, int height, int fps, URL url) {
		this.videoWidth = width;
		this.videoHeight = height;
		this.mFps = fps;
		mUrl = url;
		jbinit();
	}

	/**
	 * Read the applet file parameter and create the media player.
	 */
	public void init() {
		System.out.println("init");
		try {
			videoWidth = Integer.parseInt(getParameter("width"));
			videoHeight = Integer.parseInt(getParameter("height"));
			mFps = Integer.parseInt(getParameter("fps"));
			mUrl = new URL(getDocumentBase(), getParameter("mjpeg"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("width = " + videoWidth);
		System.out.println("height = " + videoHeight);
		System.out.println("fps = " + mFps);
		System.out.println("url = " + mUrl.toExternalForm());

		jbinit();
	}

	public void jbinit() {
		System.out.println("jbinit");
		setLayout(new BorderLayout());
		setBackground(Color.white);
		setBounds(0, 0, videoWidth, videoHeight);
	}

	/**
	 * Start media file playback. This function is called the first time that
	 * the Applet runs and every time the user re-enters the page.
	 */
	public void start() {
		System.out.println("Applet.start() is called");

		// Call start() to prefetch and start the player.
		try {
			// in = mUrl.openStream(); //will never timeout, and hangs browser

			// use HTTPClient to get stream, because it will implement a proper
			// timeout
			System.out.println("connecting to the server");
			HttpClient mClient = new HttpClient(
					new MultiThreadedHttpConnectionManager());
			int timeout = 20;
			
			HttpConnectionManager conManager = mClient.getHttpConnectionManager();
			HttpConnectionParams  params = conManager.getParams();
			params.setConnectionTimeout(timeout * 1000);
			//mClient.setConnectionTimeout(timeout * 1000); // set the timeout
			
			int iGetResultCode = -1;
			GetMethod get = new GetMethod(mUrl.toExternalForm());
			get.setFollowRedirects(true);

			//make the request
			iGetResultCode = mClient.executeMethod(get);
			System.out.println("server response code: " + iGetResultCode);
			if (iGetResultCode < 400) {
				//get the response stream
				System.out.println("connected!");
				in = new BufferedInputStream(get.getResponseBodyAsStream());
			} else {
				System.out.println("HTTP error: " + get.getStatusText());
				throw new IOException("HTTP " + iGetResultCode + " "
						+ get.getStatusText());
			}
			
			// check success or not
			if (in == null) {
				System.out.println("failed to get MJPEG stream within timeout seconds: " + timeout);
				throw new Exception("failed to get MJPEG stream within timeout seconds: " + timeout);
			}
			
			
			System.out.println("createing and starting the player");
			mPlayer = new AxisPlayer(videoWidth, videoHeight, mFps, in);
			add(mPlayer, BorderLayout.CENTER);
		} catch (Exception e) {
			Fatal(e.getMessage());
		}

		if (mPlayer != null) {
			mPlayer.start();
		}
	}

	/**
	 * Stop media file playback and release resource before leaving the page.
	 */
	public void stop() {
		System.out.println("Applet.stop() is called");
		
		//close the stream properly
		
		if (in != null) {
			System.out.println("closing the stream");
			try {
				in.close();	//close the stream
				in = null;	//gc
			} catch (IOException ioe) {
				System.out.println("IOException while closing the mjpeg stream: " + ioe.getMessage());
			}
		} else {
			System.out.println("stream is already closed (null)");
		}

		if (mPlayer != null) {
			System.out.println("stopping the player");
			mPlayer.stop();
			mPlayer.destroy();
		} else {
			System.out.println("player is not running (null)");
		}
	}

	public void destroy() {
		System.out.println("Applet.destroy() is called");
		mPlayer.destroy();
	}

	void Fatal(String s) {
		// Applications will make various choices about what
		// to do here. We print a message
		System.err.println("FATAL ERROR: " + s);
		throw new Error(s); // Invoke the uncaught exception

		// handler System.exit() is another
		// choice.
	}

	public static void main(String[] args) {
		try {
			MjpegApplet applet = new MjpegApplet(320, 240, 10, new URL(
					"http://127.0.0.1:8080/eyeq/camera-proxy?camid=5"));

			// MjpegApplet applet = new MjpegApplet(352, 240, 10, new
			// File("D:/dev/axis/data/lost_girl.mjpeg").toURL());
			JFrame frame = new JFrame("MjpegApplet Test");
			frame.pack();
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setContentPane(applet);
			frame.setSize(400, 500);

			applet.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
