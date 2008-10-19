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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet to simulate camera properties.
 * 
 * @author Jason Thrasher
 */
public class GetParamServlet extends HttpServlet {
	private static final long serialVersionUID = -1201293;
	
	private static final Log log = LogFactory.getLog(GetParamServlet.class);
	
	private static final String CONTENT_TYPE = "text/plain";

	private Camera camera;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		// mProps = new Properties();
		//
		// String prefix = getServletContext().getRealPath("/");
		// String file = getInitParameter("properties.file");
		//
		// if (file != null) {
		// File conf = new File(prefix + file); //assume relative path
		//
		// //check that conf file found
		// if (!conf.exists()) {
		// conf = new File(file); //try the absolute path
		//
		// if (!conf.exists()) {
		// throw new ServletException(
		// "unable to use specified properties file: " +
		// conf.getAbsolutePath());
		// }
		// }
		//
		// try {
		// mProps.load(new FileInputStream(conf)); //load properties
		// } catch (IOException ioe) {
		// mLog.log(Level.FATAL, "error loading camera properties", ioe);
		// throw new ServletException("could not load axis camera properties
		// file: not found in classpath",
		// ioe);
		// }
		// } else {
		// mLog.log(Level.FATAL,
		// "could not load axis camera properties file: " + file);
		// throw new ServletException(
		// "could not load axis camera properties file: " + file);
		// }
		// get the image captcha service defined via the SpringFramework
		ApplicationContext ctx = WebApplicationContextUtils
				.getRequiredWebApplicationContext(getServletContext());
		camera = (Camera) ctx.getBean("camera");

		try {
			camera.getProperties().setProperty("root.Network.IPAddress",
					InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException uhe) {
			log.warn( "could not identify host address");
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ArrayList params = new ArrayList();
		String query = request.getQueryString();

		if (query != null) {
			StringTokenizer st = new StringTokenizer(query, "&");

			while (st.hasMoreTokens()) {
				String name = st.nextToken();
				System.out.println(name);
				params.add(name);
			}
		}

		// properties to return to the requestor
		Properties propsFiltered = new Properties();

		if (params.size() == 0) {
			// send all params response
			propsFiltered = camera.getProperties();
		} else {
			ArrayList namesFiltered = new ArrayList();
			Enumeration allNames = camera.getProperties().propertyNames();

			while (allNames.hasMoreElements()) {
				String name = (String) allNames.nextElement();

				// set the filtered property
				for (int i = 0; i < params.size(); i++) {
					if (name.startsWith((String) params.get(i))) {
						namesFiltered.add(name);
					}
				}
			}

			for (int i = 0; i < namesFiltered.size(); i++) {
				String n = (String) namesFiltered.get(i);
				propsFiltered.setProperty(n, camera.getProperties()
						.getProperty(n));
			}
		}

		response.setContentType(CONTENT_TYPE);

		OutputStream out = response.getOutputStream();
		propsFiltered.store(out, null);
		out.close();

		return;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
