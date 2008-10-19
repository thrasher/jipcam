/**
 * jipCam : The Java IP Camera Project
 * Copyright (C) 2005-2007 Jason Thrasher
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

import javax.swing.event.EventListenerList;

/**
 * Multiplex event distribution to multiple MjpegFrame event consumers.
 * 
 * @author Jason Thrasher
 * 
 */
public class MjpegFrameMultiplexer {
	protected EventListenerList mListeners = new EventListenerList(); // event
																		// listeners
																		// for
																		// parsing
																		// events

	/**
	 * Add new parse listeners. This is how other code gets notification of new
	 * MJPEG frames.
	 * 
	 * @param listener
	 */
	public void addMjpegParserListener(MjpegParserListener listener) {
		mListeners.add(MjpegParserListener.class, listener);
	}

	/**
	 * Allow removal of listeners.
	 * 
	 * @param listener
	 */
	public void removeMjpegParserListener(MjpegParserListener listener) {
		mListeners.remove(MjpegParserListener.class, listener);
	}

	/**
	 * notify all listeners
	 */
	public void notifyListeners(MjpegParserEvent event) {
		Object[] listeners = mListeners.getListenerList();

		// Each listener occupies two elements - the first is the listener class
		// and the second is the listener instance
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == MjpegParserListener.class) {
				((MjpegParserListener) listeners[i + 1])
						.onMjpegParserEvent(event);
			}
		}
	}

}
