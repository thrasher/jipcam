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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import javax.media.CachingControl;
import javax.media.CachingControlEvent;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.PackageManager;
import javax.media.Player;
import javax.media.RealizeCompleteEvent;
import javax.media.StartEvent;
import javax.media.Time;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.jipcam.axis.media.protocol.http.DataSource;

import org.apache.log4j.Logger;


public class MjpegPanel extends JPanel implements ControllerListener {
	static JFrame frame;
	Logger mLog = Logger.getLogger(this.getClass());

	// media Player
	Player player = null;

	// component in which video is playing
	Component visualComponent = null;

	// controls gain, position, start, stop
	Component controlComponent = null;

	// displays progress during download
	Component progressBar = null;
	boolean firstTime = true;
	long CachingSize = 0L;
	Panel panel = null;
	int controlPanelHeight = 0;
	int videoWidth = 352;
	int videoHeight = 240;
	float mFps = 10f; //frames per second
	DataSource ds;

	//	InputStream mIn;
	URL mUrl;

	public MjpegPanel(URL url) {
		super();
		mUrl = url;
		init();
	}

	public Dimension getPreferredSize() {
		//		return new Dimension(ds.getWidth(), ds.getHeight());
		return panel.getPreferredSize();
	}

	/**
	 * Read the applet file parameter and create the media
	 * player.
	 */
	public void init() {
		setLayout(null);
		setBackground(Color.white);
		panel = new Panel();
		panel.setLayout(null);
		add(panel);
		panel.setBounds(0, 0, videoWidth, videoHeight);

		try {
			// Create an instance of a player for this media
			try {
				//				DataSource ds = new DataSource();
				ds = new DataSource(new MediaLocator(mUrl));
				ds.connect();

				//ds.start();
				player = Manager.createPlayer(ds); //force the datasource
			} catch (NoPlayerException e) {
				System.out.println(e);

				//				Fatal("Could not create player for " + mrl);
			}

			// Add ourselves as a listener for a player's events
			player.addControllerListener(this);

			//		} catch (MalformedURLException e) {
			//			Fatal("Invalid media file URL!");
		} catch (IOException e) {
			Fatal("IO exception creating player");
		}

		// This applet assumes that its start() calls 
		// player.start(). This causes the player to become
		// realized. Once realized, the applet will get
		// the visual and control panel components and add
		// them to the Applet. These components are not added
		// during init() because they are long operations that
		// would make us appear unresposive to the user.
	}

	/**
	 * Start media file playback. This function is called the
	 * first time that the Applet runs and every
	 * time the user re-enters the page.
	 */
	public void start() {
		//$ System.out.println("Applet.start() is called");
		// Call start() to prefetch and start the player.
		if (player != null) {
			player.start();
		}
	}

	/**
	 * Stop media file playback and release resource before
	 * leaving the page.
	 */
	public void stop() {
		//$ System.out.println("Applet.stop() is called");
		if (player != null) {
			player.stop();
			player.deallocate();
		}
	}

	public void destroy() {
		//$ System.out.println("Applet.destroy() is called");
		player.close();
	}

	/**
	 * This controllerUpdate function must be defined in order to
	 * implement a ControllerListener interface. This
	 * function will be called whenever there is a media event
	 */
	public synchronized void controllerUpdate(ControllerEvent event) {
		//mLog.log(Level.INFO, event.toString());
		// If we're getting messages from a dead player, 
		// just leave
		if (player == null) {
			return;
		}

		// When the player is Realized, get the visual 
		// and control components and add them to the Applet
		if (event instanceof RealizeCompleteEvent) {
			if (progressBar != null) {
				panel.remove(progressBar);
				progressBar = null;
			}

			int width = videoWidth;
			int height = 0;

			if (controlComponent == null) {
				if ((controlComponent = player.getControlPanelComponent()) != null) {
					controlPanelHeight = controlComponent.getPreferredSize().height;
					panel.add(controlComponent);
					height += controlPanelHeight;
				}
			}

			if (visualComponent == null) {
				if ((visualComponent = player.getVisualComponent()) != null) {
					panel.add(visualComponent);

					Dimension videoSize = visualComponent.getPreferredSize();
					videoWidth = videoSize.width;
					videoHeight = videoSize.height;
					width = videoWidth;
					height += videoHeight;
					visualComponent.setBounds(0, 0, width, height);

					//mLog.log(Level.INFO, "added visual comp" + visualComponent.getPreferredSize());
				}
			}

			panel.setBounds(0, 0, width, height);

			if (controlComponent != null) {
				controlComponent.setBounds(0, videoHeight, width,
					controlPanelHeight);
				controlComponent.invalidate();
			}
		} else if (event instanceof StartEvent) {
			//mLog.log(Level.INFO, visualComponent.getPreferredSize());
			//panel.invalidate();	//request to be layed out again
			//invalidate();
			frame.pack(); //needs to be called after we know the exact video size
		} else if (event instanceof CachingControlEvent) {
			if (player.getState() > Controller.Realizing) {
				return;
			}

			// Put a progress bar up when downloading starts, 
			// take it down when downloading ends.
			CachingControlEvent e = (CachingControlEvent) event;
			CachingControl cc = e.getCachingControl();

			// Add the bar if not already there ...
			if (progressBar == null) {
				if ((progressBar = cc.getControlComponent()) != null) {
					panel.add(progressBar);
					panel.setSize(progressBar.getPreferredSize());
					validate();
				}
			}
		} else if (event instanceof EndOfMediaEvent) {
			// We've reached the end of the media; rewind and
			// start over
			player.setMediaTime(new Time(0));
			player.start();
		} else if (event instanceof ControllerErrorEvent) {
			// Tell TypicalPlayerApplet.start() to call it a day
			player = null;
			Fatal(((ControllerErrorEvent) event).getMessage());
		} else if (event instanceof ControllerClosedEvent) {
			panel.removeAll();
		}
	}

	void Fatal(String s) {
		// Applications will make various choices about what
		// to do here. We print a message
		System.err.println("FATAL ERROR: " + s);
		throw new Error(s); // Invoke the uncaught exception

		// handler System.exit() is another
		// choice.
	}

	/**
	 * Utility method to register the mjpeg data type with the JMF.
	 *
	 */
	private void updateProtocolPrefix() {
		//programatically add mjpeg: protocol handler to the list
		Vector protocolPrefix = PackageManager.getProtocolPrefixList(); //unchecked cast on compile is OK!
		String myPackagePrefix = new String("com.eyeqinc");

		// Add new package prefix to end of the package prefix list.
		if (!protocolPrefix.contains(myPackagePrefix)) {
			protocolPrefix.addElement(myPackagePrefix);
		}

		PackageManager.setProtocolPrefixList(protocolPrefix);

		// Save the changes to the package prefix list.
		PackageManager.commitProtocolPrefixList();
	}

	public static void main(String[] args) {
		try {
			frame = new JFrame("MjpegViewer");

			final MjpegPanel mjpegPanel = new MjpegPanel(new URL(
						"http://127.0.0.1/"));
			frame.getContentPane().add("Center", mjpegPanel);
			frame.setSize(400, 300);

			frame.pack(); //makes the frame shrink to minimum size
			frame.setLocation(100, 100);
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent event) {
						mjpegPanel.stop();

						//						mjpegPanel.destroy();
						System.exit(0);
					}
				});
			mjpegPanel.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
