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

package net.sf.jipcam.axis.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import net.sf.jipcam.axis.JpegSequenceCreator;
import net.sf.jipcam.axis.MjpegFrameParser;


/**
 * This tool converts MJPEG raw files, or streams, into JPEG images.
 * This class does not use the CameraAPI.  It only uses the MJPEG parser to
 * extract JPEG images from a raw MJPEG stream.
 *
 * @author Jason Thrasher
 */
public class Mjpeg2JpegTool {
	public Mjpeg2JpegTool(File file) throws IOException {
		this(new FileInputStream(file), file.getName());
	}

	public Mjpeg2JpegTool(InputStream in, String jpegPrefix)
		throws IOException {
		MjpegFrameParser parser = new MjpegFrameParser(in);
		JpegSequenceCreator jsc = new JpegSequenceCreator(jpegPrefix, -1);

		parser.addMjpegParserListener(jsc);
		parser.parse();
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		//check input args
		if (args.length != 1) {
			StringBuffer sb = new StringBuffer();
			sb.append(
				"Usage: java -jar Mjpeg2JpegTool.jar [MJPEG_URL | MJPEG_FILE]");
			sb.append("\nWhere:");
			sb.append("\n\tMJPEG_URL\t= full url to Apex 2100 series camera");
			sb.append("\n\tMJPEG_FILE\t= file with raw MJPEG data");
			sb.append("\n\nExample:\njava -jar MjpegCapture.jar myMjpeg.raw");
			sb.append(
				"\n\nExample:\njava -jar MjpegCapture.jar http://myCameraIpAddr:80/axis-cgi/mjpg/video.cgi");
			System.out.println(sb.toString());
			System.exit(1);
		}

		try {
			//validate argument
			String fileOrUrl = args[0];
			Mjpeg2JpegTool mjpeg2JpegTool = null;

			try {
				URL url = new URL(fileOrUrl);
				System.out.println("Attempting to read from URL");
				mjpeg2JpegTool = new Mjpeg2JpegTool(url.openStream(), "image-");
			} catch (MalformedURLException murle) {
				//try to open as file
				File file = new File(fileOrUrl);

				//check failure
				if (!file.exists()) {
					//failed to understand args
					System.out.println(
						"Could not parse argument for URL or FILE.");
					System.exit(2);
				}

				System.out.println("Attempting to read from file");
				mjpeg2JpegTool = new Mjpeg2JpegTool(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
