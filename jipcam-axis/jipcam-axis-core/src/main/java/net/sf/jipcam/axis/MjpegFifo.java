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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Logger;

/**
 * Provides notification of new frame events to the servlet, from the parser
 * thread. This can be used to hand off MJPEG frames from one thread to another
 * in a non-blocking way by treating the hand-off as a FIFO data structure. The
 * MJPEG frame source thread (which creates MJPEG frames), will add to the FIFO,
 * while the destination thread will wait until at least one frame is in the
 * FIFO.
 * 
 * @author Jason Thrasher
 */
public class MjpegFifo implements MjpegParserListener {
	private static Logger mLog;

	private FifoQueue frameFifo; // fifo of frames

	public MjpegFifo(int size) {
		frameFifo = new FifoQueue();
	}

	public MjpegFrame getMjpegFrame() throws InterruptedException {
		return (MjpegFrame) frameFifo.removeOne(); // blocks until there is a frame
		// in the fifo
	}

	public boolean isEmpty() {
		return frameFifo.isEmpty();
	}

	/**
	 * Allow users to query if we're full, and subsequent writes will block.
	 * 
	 * @return true if full, false otherwise
	 */
	public boolean isFull() {
		return frameFifo.isFull();
	}

	public void removeAll() throws InterruptedException {
		frameFifo.removeAll();
	}

	/**
	 * Anyone can add a frame to the fifo here ...this allows interleaving of
	 * multiple cameras into one FIFO
	 * 
	 * @param frame
	 */
	public void addMjpegFrame(MjpegFrame frame) {
		// try {
		frameFifo.add(frame); // blocks until there is space in the fifo

		// } catch (InterruptedException ie) {
		// mLog.log(Level.WARN, "failed to add next frame", ie);
		// }
	}

	/**
	 * listener interface method
	 * 
	 * @param frame
	 */
	public void onMjpegParserEvent(MjpegParserEvent event) {
		addMjpegFrame(event.getMjpegFrame());
	}

	public void waitUntilEmpty() throws InterruptedException {
		frameFifo.waitUntilEmpty();
	}

	private class FifoQueue {
		private List mQueue = Collections.synchronizedList(new LinkedList());

		private Object queueLock = new Object();

		public FifoQueue() {
		}

		public Object add(Object element) {
			synchronized (queueLock) {
				mQueue.add(element);
				queueLock.notify();
			}

			return element;
		}

		/**
		 * Remove the first object from the queue
		 * 
		 * @return the first object, or null if the queue is empty
		 */
		public Object remove() {
			// test that there's an object in the list to avoid IndexOutOfBounds
			return mQueue.isEmpty() ? null : mQueue.remove(0);
		}

		/**
		 * This method will block until at least one object exists in the queue.
		 * The object will be removed and returned.
		 * 
		 * @return
		 */
		public Object removeOne() {
			synchronized (queueLock) {
				while (mQueue.isEmpty()) {
					try {
						queueLock.wait(); // block until object exists
					} catch (InterruptedException ie) {
						//mLog.debug(ie);
						return null;	//TODO: improve this
					}
				}
				return mQueue.remove(0);
			}

		}

		public boolean isEmpty() {
			return mQueue.isEmpty();
		}

		public boolean isFull() {
			return false;
		}

		public void removeAll() {
			mQueue.clear();
		}

		public void waitUntilEmpty() {
			while (!mQueue.isEmpty()) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
