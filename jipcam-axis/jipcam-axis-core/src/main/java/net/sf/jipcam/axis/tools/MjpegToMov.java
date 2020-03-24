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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.sf.jipcam.axis.media.protocol.http.DataSource;

// import javax.media.protocol.DataSource;

/**
 * This program takes a MJPEG file and converts it to a QuickTime movie. Sample
 * command line: -w 352 -h 240 -f 10 -m d:/dev/axis/data/lost_girl.mjpeg -o
 * file:///d:/out.mov
 * 
 * @author Jason Thrasher
 */
public class MjpegToMov implements ControllerListener, DataSinkListener {
	private static Options options;

	private Object waitSync = new Object();

	private boolean stateTransitionOK = true;

	private Object waitFileSync = new Object();

	private boolean fileDone = false;

	private boolean fileSuccess = true;

	private static Logger mLog = LogManager.getLogger(MjpegToMov.class);

	public boolean doIt(int width, int height, int frameRate,
			String inputMjpeg, MediaLocator outML) throws FileNotFoundException {
		DataSource ids = new DataSource(width, height, frameRate,
				new FileInputStream(inputMjpeg));

		try {
			ids.connect();
		} catch (IOException ioe) {
			mLog.error("unable to read from datasource", ioe);
			return false;
		}

		Processor p;

		try {
			mLog.info("- create processor for the image datasource ...");
			p = Manager.createProcessor(ids);
		} catch (Exception e) {
			mLog.error(
					"Yikes!  Cannot create a processor from the data source: "
							+ ids, e);

			return false;
		}

		p.addControllerListener(this);

		// Put the Processor into configured state so we can set
		// some processing options on the processor.
		p.configure();

		if (!waitForState(p, p.Configured)) {
			mLog.error("Failed to configure the processor.");

			return false;
		}

		// Set the output content descriptor to QuickTime.
		p.setContentDescriptor(new ContentDescriptor(
				FileTypeDescriptor.QUICKTIME));

		// Query for the processor for supported formats.
		// Then set it on the processor.
		TrackControl[] tcs = p.getTrackControls();
		Format[] f = tcs[0].getSupportedFormats();

		if ((f == null) || (f.length <= 0)) {
			mLog.error("The mux does not support the input format: "
					+ tcs[0].getFormat());

			return false;
		}

		tcs[0].setFormat(f[0]);

		mLog.error("Setting the track format to: " + f[0]);

		// We are done with programming the processor. Let's just
		// realize it.
		p.realize();

		if (!waitForState(p, p.Realized)) {
			mLog.error("Failed to realize the processor.");

			return false;
		}

		// Now, we'll need to create a DataSink.
		DataSink dsink;

		if ((dsink = createDataSink(p, outML)) == null) {
			mLog
					.error("Failed to create a DataSink for the given output MediaLocator: "
							+ outML);

			return false;
		}

		dsink.addDataSinkListener(this);
		fileDone = false;

		mLog.info("start processing...");

		// OK, we can now start the actual transcoding.
		try {
			p.start();
			dsink.start();
		} catch (IOException e) {
			mLog.error("IO error during processing");

			return false;
		}

		// Wait for EndOfStream event.
		waitForFileDone();

		// Cleanup.
		try {
			dsink.close();
		} catch (Exception e) {
		}

		p.removeControllerListener(this);

		mLog.info("...done processing.");

		return true;
	}

	/**
	 * Create the DataSink.
	 */
	DataSink createDataSink(Processor p, MediaLocator outML) {
		javax.media.protocol.DataSource ds;

		if ((ds = p.getDataOutput()) == null) {
			mLog
					.error("Something is really wrong: the processor does not have an output DataSource");

			return null;
		}

		DataSink dsink;

		try {
			mLog.info("- create DataSink for: " + outML);
			dsink = Manager.createDataSink(ds, outML);
			dsink.open();
		} catch (Exception e) {
			mLog.error("Cannot create the DataSink: " + e);

			return null;
		}

		return dsink;
	}

	/**
	 * Block until the processor has transitioned to the given state. Return
	 * false if the transition failed.
	 */
	boolean waitForState(Processor p, int state) {
		synchronized (waitSync) {
			try {
				while ((p.getState() < state) && stateTransitionOK)
					waitSync.wait();
			} catch (Exception e) {
			}
		}

		return stateTransitionOK;
	}

	/**
	 * Controller Listener.
	 */
	public void controllerUpdate(ControllerEvent evt) {
		if (evt instanceof ConfigureCompleteEvent
				|| evt instanceof RealizeCompleteEvent
				|| evt instanceof PrefetchCompleteEvent) {
			synchronized (waitSync) {
				stateTransitionOK = true;
				waitSync.notifyAll();
			}
		} else if (evt instanceof ResourceUnavailableEvent) {
			synchronized (waitSync) {
				stateTransitionOK = false;
				waitSync.notifyAll();
			}
		} else if (evt instanceof EndOfMediaEvent) {
			evt.getSourceController().stop();
			evt.getSourceController().close();
		}
	}

	/**
	 * Block until file writing is done.
	 */
	boolean waitForFileDone() {
		synchronized (waitFileSync) {
			try {
				while (!fileDone)
					waitFileSync.wait();
			} catch (Exception e) {
			}
		}

		return fileSuccess;
	}

	/**
	 * Event handler for the file writer.
	 */
	public void dataSinkUpdate(DataSinkEvent evt) {
		if (evt instanceof EndOfStreamEvent) {
			synchronized (waitFileSync) {
				fileDone = true;
				waitFileSync.notifyAll();
			}
		} else if (evt instanceof DataSinkErrorEvent) {
			synchronized (waitFileSync) {
				fileDone = true;
				fileSuccess = false;
				waitFileSync.notifyAll();
			}
		}
	}

	public static void main(String[] args) {
		// create Options object
		options = new Options();

		// add options
		options.addOption(OptionBuilder.withArgName("w").withLongOpt("width")
				.withDescription("image width").hasArg(true).isRequired(true)
				.create("w"));
		options.addOption(OptionBuilder.withArgName("h").withLongOpt("height")
				.withDescription("image height").hasArg().isRequired().create(
						"h"));
		options.addOption(OptionBuilder.withArgName("f").withLongOpt("fps")
				.withDescription("frames per second").hasArg().isRequired()
				.create("f"));
		options.addOption(OptionBuilder.withArgName("m").withLongOpt("mjpeg")
				.withDescription("mjpeg source file").hasArg().isRequired()
				.create("m"));
		options.addOption(OptionBuilder.withArgName("o").withLongOpt("mov")
				.withDescription("output mov file").hasArg().isRequired()
				.create("o"));

		CommandLine cmd = null;

		try {
			CommandLineParser parser = new BasicParser();
			cmd = parser.parse(options, args);
		} catch (ParseException pe) {
			if (pe instanceof MissingOptionException) {
				mLog.info("missing required option: " + pe.getMessage());
			} else if (pe instanceof UnrecognizedOptionException) {
				mLog.info("unknown option: " + pe.getMessage());
			}

			prUsage();
		}

		// Parse the arguments.
		int width = Integer.parseInt(cmd.getOptionValue("w"));
		int height = Integer.parseInt(cmd.getOptionValue("h"));
		int frameRate = Integer.parseInt(cmd.getOptionValue("f"));
		String outputURL = cmd.getOptionValue("o");
		String inputMjpeg = cmd.getOptionValue("m");

		// Check for output file extension.
		if (!outputURL.toLowerCase().endsWith(".mov")) {
			mLog
					.error("The output file extension should end with a .mov extension");
		}

		if ((width < 0) || (height < 0)) {
			mLog.error("Please specify the correct image size.");
			prUsage();
		}

		// Check the frame rate.
		if (frameRate < 1) {
			frameRate = 1;
		}

		// Generate the output media locators.
		MediaLocator oml;

		if ((oml = createMediaLocator(outputURL)) == null) {
			mLog.error("Cannot build media locator from: " + outputURL);
			System.exit(0);
		}

		net.sf.jipcam.axis.tools.MjpegToMov imageToMovie = new net.sf.jipcam.axis.tools.MjpegToMov();

		try {
			imageToMovie.doIt(width, height, frameRate, inputMjpeg, oml);
		} catch (FileNotFoundException fnfe) {
			mLog.error("Input MJPEG file not found: " + inputMjpeg);
		}

		System.exit(0);
	}

	static void prUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("MjpegToMov", options);
		System.exit(1);
	}

	/**
	 * Create a media locator from the given string.
	 */
	static MediaLocator createMediaLocator(String url) {
		MediaLocator ml;

		if ((url.indexOf(":") > 0) && ((ml = new MediaLocator(url)) != null)) {
			return ml;
		}

		if (url.startsWith(File.separator)) {
			if ((ml = new MediaLocator("file:" + url)) != null) {
				return ml;
			}
		} else {
			String file = "file:" + System.getProperty("user.dir")
					+ File.separator + url;

			if ((ml = new MediaLocator(file)) != null) {
				return ml;
			}
		}

		return null;
	}
}
