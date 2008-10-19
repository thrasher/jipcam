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

import junit.framework.TestCase;

public class CameraAPITest extends TestCase {
	private CameraAPI api;

	public CameraAPITest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		// api = new CameraAPI();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * dummy test for now
	 */
	public void testGetMjpegBoundary() throws Exception {
		String boundary = CameraAPI.getMjpegBoundary();
		assertEquals(CameraAPI.MJPEG_BOUNDARY, boundary);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(CameraAPITest.class);
	}
}
