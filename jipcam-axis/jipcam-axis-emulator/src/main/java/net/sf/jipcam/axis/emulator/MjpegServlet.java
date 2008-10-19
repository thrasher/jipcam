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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jipcam.axis.MjpegFrame;
import net.sf.jipcam.axis.MjpegInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet to simulate the MJPEG stream coming from the camera. This allows use
 * of a "canned" stream on the disk. The stream is "played" in a loop for the
 * HTTP client. The client should behave as if it's a continuous stream.
 * 
 * The CGI request supports modifications to FPS. This allows the client to run
 * at much higher FPS than the camera actually supports. The timing of the
 * response MJPEG frames is not very accurate - but good enough up to around 100
 * FPS on a 2 GHz computer.
 * 
 * @author Jason Thrasher
 */
public class MjpegServlet extends HttpServlet {
	private static final long serialVersionUID = -1201293;

	private static final Log log = LogFactory.getLog(MjpegServlet.class);

	private static final String CONTENT_TYPE = "multipart/x-mixed-replace; boundary=--myboundary";

	// private static int DEFAULT_FPS = 10; // set in web.xml

	// private File mMjpegFile; // set in web.xml
	private Camera camera;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ApplicationContext ctx = WebApplicationContextUtils
				.getRequiredWebApplicationContext(getServletContext());
		camera = (Camera) ctx.getBean("camera");

		// set the path relative to the webapp
		String path = getServletContext().getRealPath(camera.getMjpegFile());
		camera.setMjpeg(new File(path));

		log.info("using MJPEG: " + camera.getMjpeg().getAbsolutePath());

		log.info("default FPS: " + camera.getFps());
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// log request info
		log.info("client host: " + request.getRemoteHost());
		log.info("client addr: " + request.getRemoteAddr());

		// log client information
		Enumeration headers = request.getHeaderNames();
		while (headers.hasMoreElements()) {
			String name = (String) headers.nextElement();
			String value = request.getHeader(name);
			log.info("request header: " + name + "=" + value);
		}

		// log out the request parameters
		Enumeration names = request.getParameterNames();

		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String value = request.getParameter(name);
			log.info("request parameter: " + name + "=" + value);
		}

		// try to get the frames per second value from the request
		int fps = getIntegerParam(request.getParameter("req_fps"), -1);
		if (fps == -1) {
			// fall-back to "desired fps" if needed
			fps = getIntegerParam(request.getParameter("des_fps"), -1);
		}

		// calculate the milliseconds of delay to use between frames
		int deltaT = (fps <= 0) ? (1000 / camera.getFps()) : (1000 / fps);
		log.info("Delta T between frames (ms): " + deltaT);

		// send response headers, this doesn't work the same with all servlet
		// containers
		response.setContentType(CONTENT_TYPE);
		response.setHeader("Server", "Camd");
		response.setHeader("Connection", "Close");

		OutputStream out = new BufferedOutputStream(response.getOutputStream());
		MjpegInputStream in = new MjpegInputStream(new FileInputStream(camera
				.getMjpeg()));
		MjpegFrame frame = null; // next frame
		int byteCount = 0; // count number of bytes written
		int frameCount = 0; // count frames
		boolean lostConnection = false;

		try {
			while (true) {
				try {
					frame = in.readMjpegFrame();
				} catch (IOException eof) {
					// we hit the end of the file, start over!
					in.close();
					in = new MjpegInputStream(new FileInputStream(camera
							.getMjpeg()));

					// log this client's activity
					log.info(request.getRemoteHost() + " bytes: " + byteCount
							+ " frames: " + frameCount);

					continue;
				}
				out.write(frame.getBytes(), 0, frame.getLength());
				out.flush();

				// increment counters
				byteCount += frame.getLength(); // count bytes
				frameCount++;

				// pause to simulate camera delay per framerate
				Thread.sleep(deltaT);
			}
		} catch (IOException ioe) {
			// usually due to client disconnect
			log.fatal("client connection closed from: "
					+ request.getRemoteHost());
			lostConnection = true;
		} catch (InterruptedException ie) {
			log.warn(ie.getMessage());
		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			if (!lostConnection) {
				try {
					out.close();
				} catch (Exception e) {
					log.warn(e.getMessage());
				}
			}
			try {
				in.close();
			} catch (Exception e) {
				log.fatal("we had a problem reading from the mjpeg file!", e);
			}
		}
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Utility method to uniformly parse numbers
	 */
	private int getIntegerParam(String value, int vDefault) {
		int val = vDefault;

		try {
			val = Integer.parseInt(value);
		} catch (Exception e) {
			log.warn("failed to parse integer: " + value + " using default: "
					+ vDefault);
		}

		return val;
	}
}
