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

package net.sf.jipcam.axis.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.media.CachingControl;
import javax.media.CachingControlEvent;
import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.RealizeCompleteEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.jipcam.axis.CameraAPI;
import net.sf.jipcam.axis.media.protocol.http.DataSource;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

/**
 * Player for MJPEG data from static files, or from Axis camera urls.
 * 
 * @author Jason Thrasher
 */
public class AxisPlayer extends JPanel implements ControllerListener {
	protected static Options options = null;

	protected static int fps = 0;

	protected static JFrame frame = null;

	protected static int videoWidth = 0;

	protected static int videoHeight = 0;

	protected int height = 0;

	protected int width = 0;

	// media Player
	protected Player player = null;

	// component in which video is playing
	protected Component visualComponent = null;

	// controls gain, position, start, stop
	protected Component controlComponent = null;

	// displays progress during download
	protected Component progressBar = null;

	protected boolean firstTime = true;

	protected long CachingSize = 0L;

	protected JPanel panel = null;

	protected int controlPanelHeight = 0;

	public AxisPlayer(int width, int height, int fps, InputStream in) {
		videoWidth = width;
		videoHeight = height;
		this.fps = fps;

		try {
			// tracker processing
			// Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, Boolean.TRUE);
			DataSource ds = new DataSource(videoWidth, videoHeight, fps, in);
			ds.connect();
			player = Manager.createPlayer(ds);
			player.addControllerListener(this);
		} catch (NoPlayerException e) {
			System.out.println(e);
			Fatal("No player found");
		} catch (IOException e) {
			Fatal("IO exception creating player");
		}

		init();
	}

	public AxisPlayer(MediaLocator locator) {
		try {
			// Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, Boolean.TRUE);
			player = Manager.createPlayer(locator);
			player.addControllerListener(this);
		} catch (NoPlayerException e) {
			System.out.println(e);
			Fatal("No player found");
		} catch (IOException e) {
			Fatal("IO exception creating player");
		}

		init();
	}

	/**
	 * Read the applet file parameter and create the media player.
	 */
	public void init() {
		// JPanel wrapperPanel = new JPanel();
		setLayout(new BorderLayout(0, 0));

		// setBackground(Color.white);
		panel = new JPanel();
		panel.setLayout(null);
		panel.setBounds(0, 0, videoWidth, videoHeight);
		add(panel, BorderLayout.CENTER);
	}

	/**
	 * In order to auto-set the frame size of the application, we need to
	 * publicize our preference.
	 */
	public Dimension getPreferredSize() {
		return new Dimension(width, height);
	}

	/**
	 * Start media file playback.
	 */
	public void start() {
		// Call start() to prefetch and start the player.
		if (player != null) {
			player.start();
		}
	}

	/**
	 * Stop media file playback and release resources.
	 */
	public void stop() {
		if (player != null) {
			player.stop();
			player.deallocate();
		}
	}

	public void destroy() {
		player.close();
	}

	/**
	 * This controllerUpdate function must be defined in order to implement a
	 * ControllerListener interface. This function will be called whenever there
	 * is a media event
	 */
	public synchronized void controllerUpdate(ControllerEvent event) {
		// If we're getting messages from a dead player,
		// just leave
		if (player == null) {
			return;
		}

		// System.out.println("player event: " + event.getClass().getName());
		// When the player is Realized, get the visual
		// and control components and add them to the Applet
		if (event instanceof RealizeCompleteEvent) {
			if (progressBar != null) {
				panel.remove(progressBar);
				progressBar = null;
			}

			// int width = 320;
			height = 0;

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
					visualComponent.setBounds(0, 0, videoWidth, videoHeight);
				}
			}

			panel.setBounds(0, 0, width, height);

			if (controlComponent != null) {
				controlComponent.setBounds(0, videoHeight, width,
						controlPanelHeight);
				controlComponent.invalidate();
			}
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
			System.out.println("End of media reached.");

			// We've reached the end of the media; rewind and
			// start over
			// player.setMediaTime(new Time(0));
			// player.start();
		} else if (event instanceof ControllerErrorEvent) {
			// Tell TypicalPlayerApplet.start() to call it a day
			player = null;
			Fatal(((ControllerErrorEvent) event).getMessage());
		} else if (event instanceof ControllerClosedEvent) {
			panel.removeAll();
		} else if (event instanceof ConfigureCompleteEvent) {
			// allow this processor to function as a player
			// player.setContentDescriptor(null);
		}

		if (frame != null) {
			frame.pack(); // repack the tree as needed
			System.out.println(panel.getSize());
		}
	}

	protected void Fatal(String s) {
		// Applications will make various choices about what
		// to do here. We print a message
		System.err.println("FATAL ERROR: " + s);
		throw new Error(s); // Invoke the uncaught exception

		// handler System.exit() is another
		// choice.
	}

	protected static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("AxisPlayer", options);
		System.exit(1);
	}

	public static void main(String[] args) {
		// create Options object
		options = new Options();

		// add options
		options.addOption(OptionBuilder.withArgName("w").withLongOpt("width")
				.withDescription("image width").hasArg(true).isRequired(false)
				.create("w"));
		options.addOption(OptionBuilder.withArgName("h").withLongOpt("height")
				.withDescription("image height").hasArg().isRequired(false)
				.create("h"));
		options.addOption(OptionBuilder.withArgName("f").withLongOpt("fps")
				.withDescription("frames per second").hasArg()
				.isRequired(false).create("f"));

		// handle authentication
		options.addOption(OptionBuilder.withLongOpt("username")
				.withDescription("Axis camera username").hasArg().create("u"));
		options.addOption(OptionBuilder.withLongOpt("password")
				.withDescription("Axis camera password").hasArg().create("p"));

		// options.addOption(OptionBuilder.withArgName("m").withLongOpt("mjpeg")
		// .withDescription("mjpeg source file")
		// .hasArg().isRequired().create("m"));
		OptionGroup group = new OptionGroup();
		group.addOption(OptionBuilder.withLongOpt("mjpeg-file")
				.withDescription("Axis MJPEG raw file").hasArg().create("m"));
		group.addOption(OptionBuilder.withLongOpt("camera-url")
				.withDescription("Axis camera url").hasArg().isRequired()
				.create("c"));
		group.addOption(OptionBuilder.withLongOpt("jmf-url").withDescription(
				"jmf url like rtp:// or vfw:// or http://").hasArg()
				.isRequired().create("j"));
		group.setRequired(true);
		options.addOptionGroup(group);

		CommandLine cmd = null;

		try {
			CommandLineParser parser = new BasicParser();
			cmd = parser.parse(options, args);
			// } catch (ParseException pe) {
		} catch (Exception pe) {
			if (pe instanceof MissingOptionException) {
				System.out.println("missing required option: "
						+ pe.getMessage());
			} else if (pe instanceof UnrecognizedOptionException) {
				System.out.println("unknown option: " + pe.getMessage());
			}

			printUsage();
		}

		frame = new JFrame("Axis Player");

		// verify that com.eyeqinc.axis protocol prefix is registered
		boolean test = DataSource.registerProtocolPrefix();

		InputStream in = null;
		AxisPlayer mp = null;

		try {
			if (group.getSelected().endsWith("j")) {
				// use the JMF api to play the URL
				String jmfUrlS = cmd.getOptionValue("j");
				MediaLocator locator = new MediaLocator(jmfUrlS);
				mp = new AxisPlayer(locator);
			} else {
				// validate parameters
				if (!cmd.hasOption("w") || !cmd.hasOption("h")
						|| !cmd.hasOption("f")) {
					System.out
							.println("Missing one or more required options for Axis MJPEG: -w -h -f");
					printUsage();
				}

				// set parameters
				videoWidth = Integer.parseInt(cmd.getOptionValue("w"));
				videoHeight = Integer.parseInt(cmd.getOptionValue("h"));
				fps = Integer.parseInt(cmd.getOptionValue("f"));
				String username = cmd.getOptionValue("u");
				String password = cmd.getOptionValue("p");

				if (group.getSelected().endsWith("m")) {
					// find the mjpeg file
					String mjpegFile = cmd.getOptionValue("m");
					in = new FileInputStream(mjpegFile);
					mp = new AxisPlayer(videoWidth, videoHeight, fps, in);
				} else if (group.getSelected().endsWith("c")) {
					// use the camera api
					String urlS = cmd.getOptionValue("c");
					URL url = new URL(urlS);
					CameraAPI api = new CameraAPI(url, 5000, username, password);
					in = api.getMjpegCgi(videoWidth, videoHeight, -1, -1, -1,
							true, true, false, false, -1, true, -1, -1, fps,
							true, true, -1);
					mp = new AxisPlayer(videoWidth, videoHeight, fps, in);
				}
			}
		} catch (FileNotFoundException fnfe) {
			System.out.println(fnfe.getMessage());
			System.exit(2);
		} catch (MalformedURLException murle) {
			System.out.println(murle.getMessage());
			System.exit(3);
		} catch (IOException ioe) {
			System.out.println("IO Error: " + ioe.getMessage());
			System.exit(4);
		}

		frame.addWindowListener(new WindowCloser(mp));
		frame.add(new Panel().add(mp));

		// Create the frame's peer. Peer is not visible.
		frame.addNotify();

		// mp.init();
		frame.setVisible(true);

		mp.start();
	}

	protected static class WindowCloser extends WindowAdapter {
		private AxisPlayer mp = null;

		public WindowCloser(AxisPlayer mp) {
			this.mp = mp;
		}

		public void windowClosing(WindowEvent w) {
			// s.setActive (false);
			if (mp != null) {
				mp.stop();
				mp.destroy();
			}

			System.exit(0);
		}
	}
}
