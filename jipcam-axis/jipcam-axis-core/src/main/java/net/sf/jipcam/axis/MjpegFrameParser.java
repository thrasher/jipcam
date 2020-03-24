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

import java.io.InputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MJPEG data frame parser. This class parses the input stream for MJPEG data.
 * 
 * No assumption is made about the synchronization of the input stream with any
 * frame boundary. The input stream can begin mid-frame and this parser will
 * still find the first complete MJPEG data frame.
 * 
 * Axis camera JPEG images are 4:2:2
 * 
 * @see http://www2.axis.com/files/developer/camera/JPEG_format_1_1.pdf
 *      JPEG_format_1_1
 * @author Jason Thrasher
 */
public class MjpegFrameParser implements Runnable {
	private static Logger mLog = LogManager.getLogger(MjpegFrameParser.class); // logging
																			// mechanism

	protected MjpegFrameMultiplexer frameMuxer = new MjpegFrameMultiplexer();

	// protected EventListenerList mListeners = new EventListenerList(); //event
	// listeners for parsing events
	private volatile Thread mThread; // thread management

	private volatile boolean mThreadSuspended = false; // track thread
														// suspended or not

	private Object mLock = new Object(); // lock for thread

	private InputStream mIn;

	/**
	 * Create a new frame parser with the given input stream, and given buffer
	 * size.
	 * 
	 * Note: MjpegFormat.FRAME_MAX_LENGTH value controls the amount of memory
	 * used for buffering the input. If the buffer is not large enough, the
	 * parser may not find the frame boundaries.
	 * 
	 * @param mjpegStream
	 *            from any datasource.
	 */
	public MjpegFrameParser(InputStream mjpegStream) {
		mIn = mjpegStream;
		mThreadSuspended = false;
	}

	/**
	 * Add new parse listeners. This is how other code gets notification of new
	 * MJPEG frames.
	 * 
	 * @param listener
	 */
	public void addMjpegParserListener(MjpegParserListener listener) {
		// mListeners.add(MjpegParserListener.class, listener);
		frameMuxer.addMjpegParserListener(listener);
	}

	/**
	 * Allow removal of listeners.
	 * 
	 * @param listener
	 */
	public void removeMjpegParserListener(MjpegParserListener listener) {
		// mListeners.remove(MjpegParserListener.class, listener);
		frameMuxer.removeMjpegParserListener(listener);
	}

	/**
	 * notify all listeners
	 */
//	protected void notifyListeners(MjpegParserEvent event) {
//		Object[] listeners = mListeners.getListenerList();
//
//		// Each listener occupies two elements - the first is the listener class
//		// and the second is the listener instance
//		for (int i = 0; i < listeners.length; i += 2) {
//			if (listeners[i] == MjpegParserListener.class) {
//				((MjpegParserListener) listeners[i + 1])
//						.onMjpegParserEvent(event);
//			}
//		}
//	}

	/**
	 * Start the parser thread in a blocking fashion. Parser will notify all
	 * listeners of MJPEG parse events. This call will block until the end of
	 * the stream is reached. To start asynchronously, use the start() method.
	 */
	public void parse() {
		mLog.log(Level.DEBUG, "parse called");
		start();

		try {
			mThread.join(); // wait until parsing is complete before returning
		} catch (InterruptedException interE) {
			interE.printStackTrace();
		}
	}

	/**
	 * Start the parsing thread.
	 */
	public void start() {
		mLog.log(Level.DEBUG, "starting parser thread");

		synchronized (mLock) {
			if (mThread == null) {
				mThread = new Thread(this, this.getClass().getName());

				// allow to exit cleanly if the VM goes down
				mThread.setDaemon(true);
			}

			mThread.start();
			mLog.log(Level.DEBUG, "started thread");
			mLock.notify();
		}
	}

	public void stop() {
		mLog.log(Level.DEBUG, "stopping parser thread");

		synchronized (mLock) {
			mThread = null;
			mLock.notify();
		}
	}

	public void suspend() {
		synchronized (mLock) {
			mThreadSuspended = true;
		}
	}

	public void resume() {
		synchronized (mLock) {
			mThreadSuspended = false;
			mLock.notify();
		}
	}

	public boolean isAlive() {
		boolean alive;

		synchronized (mLock) {
			alive = (mThread == null) ? false : mThread.isAlive();
			mLock.notify();
		}

		return alive;
	}

	public void join() {
		try {
			mThread.join(); // wait until parsing is complete before returning
		} catch (InterruptedException interE) {
			interE.printStackTrace();
		}
	}

	/**
	 * Runnable method for the new thread.
	 */
	public void run() {
		Thread thread = null;

		synchronized (mLock) {
			thread = Thread.currentThread();
		}

		try {
			MjpegInputStream in = new MjpegInputStream(mIn);
			MjpegFrame frame;

			// main thread loop
			while (mThread == thread) {
				try {
					// start creating the MjpegFrame
					frame = in.readMjpegFrame();

					if (mLog.isDebugEnabled()) {
						mLog
								.debug("mjpeg frame count = "
										+ frame.getSequence());
					}

					// notify all listeners
					// notifyListeners(new MjpegParserEvent(this, frame));
					frameMuxer
							.notifyListeners(new MjpegParserEvent(this, frame));

					// handle suspend/resume/stop
					synchronized (mLock) {
						while (mThreadSuspended && (mThread == thread)) {
							mLock.wait();
						}
					}
				} catch (InterruptedException interE) {
					interE.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		mLog.log(Level.DEBUG, "thread stopped");
	}
}
