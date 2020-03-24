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

package net.sf.jipcam.axis.emulator;

import java.io.File;
import java.io.FileInputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.sf.jipcam.axis.MjpegFifo;
import net.sf.jipcam.axis.MjpegFrameParser;


/**
 * FIFO Looping Thread reads from a file.  When the end of the file is reached,
 * it is re-opened from the beginning and the therad continues.
 * @author Jason Thrasher
 */
public class MjpegFifoLoop implements Runnable {
	private volatile Thread mThread; //thread management
	private volatile boolean mThreadSuspended = false; //track thread suspended or not
	private Object mLock = new Object(); //lock for thread
	private Logger mLog; //logging mechanism
	private MjpegFrameParser mParser;
	private File mMjpegFile;
	private MjpegFifo mFifo;

	public MjpegFifoLoop(File mjpegFile, MjpegFifo fifo) {
		mLog = LogManager.getLogger(this.getClass());

		mMjpegFile = mjpegFile;
		mFifo = fifo;

		mThreadSuspended = false;
	}

	/**
	 * Start the thread.
	 */
	public void start() {
		mLog.log(Level.DEBUG, "starting thread");

		synchronized (mLock) {
			if (mThread == null) {
				mThread = new Thread(this, this.getClass().getName());

				//allow to exit cleanly if the VM goes down
				mThread.setDaemon(true);
			}

			mThread.start();
			mLog.log(Level.DEBUG, "started thread");
			mLock.notify();
		}
	}

	public void stop() {
		mLog.log(Level.DEBUG, "stopping thread");

		synchronized (mLock) {
			mThread = null;
			mParser.stop();

			try {
				mFifo.removeAll();
			} catch (InterruptedException ie) {
				//ignore
			}

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
		mLog.log(Level.DEBUG, "isAlive called");

		boolean alive;

		synchronized (mLock) {
			alive = (mThread == null) ? false : mThread.isAlive();

			//mLock.notify();
		}

		return alive;
	}

	public void join() {
		mLog.log(Level.DEBUG, "join called");

		try {
			mThread.join(); //wait until complete before returning
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
			//main thread loop
			while (mThread == thread) {
				mParser = new MjpegFrameParser(new FileInputStream(mMjpegFile));
				mParser.addMjpegParserListener(mFifo);
				mParser.parse(); //blocking

				//thread work
				try {
					synchronized (mLock) {
						while (mThreadSuspended && (mThread == thread)) {
							mLock.wait();
						}
					}
				} catch (InterruptedException interE) {
					interE.printStackTrace();
				}

				//break;
			}

			mFifo.removeAll(); //clean up
		} catch (Exception e) {
			e.printStackTrace();
		}

		mLog.log(Level.DEBUG, "thread stopped");
	}
}
