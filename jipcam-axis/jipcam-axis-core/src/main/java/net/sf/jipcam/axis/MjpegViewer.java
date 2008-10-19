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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JFrame;


public class MjpegViewer {
	public MjpegViewer() {
		try {
			JFrame frame = new JFrame("MjpegViewer");
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

	public static void main(String[] args) {
		MjpegViewer mjpegViewer = new MjpegViewer();
	}
}
