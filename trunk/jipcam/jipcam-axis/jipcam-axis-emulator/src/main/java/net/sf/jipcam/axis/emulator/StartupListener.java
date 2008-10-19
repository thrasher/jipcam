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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * StartupListener class used to initialize the system.
 */
public class StartupListener implements ServletContextListener {
	private static final Log log = LogFactory.getLog(StartupListener.class);

	public void contextInitialized(ServletContextEvent event) {
		log.debug("initializing context...");

		ServletContext context = event.getServletContext();

		ApplicationContext ctx = WebApplicationContextUtils
				.getRequiredWebApplicationContext(context);

		setupContext(context);
	}

	/**
	 * This method uses the LookupManager to lookup available roles from the
	 * data layer.
	 * 
	 * @param context
	 *            The servlet context
	 */
	public static void setupContext(ServletContext context) {
		ApplicationContext ctx = WebApplicationContextUtils
				.getRequiredWebApplicationContext(context);

		log.debug("Axis Camera Emulator started OK");
	}

	/**
	 * This is a no-op method.
	 * 
	 * @param servletContextEvent
	 *            The servlet context event
	 */
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
	}
}
