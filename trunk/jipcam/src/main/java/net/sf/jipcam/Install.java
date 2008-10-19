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

package net.sf.jipcam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.media.PackageManager;

/**
 * Utility class to register all jipCam plugins with the JMF registry.
 * 
 * @author Jason Thrasher
 */
public class Install {
	public Install() {
		registerPlugins();
	}

    /**
     * Register all plugins that ship with jipCam.
     *
     */
	private void registerPlugins() {
        net.sf.jipcam.axis.media.protocol.http.DataSource.registerProtocolPrefix();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Install();
	}
}
