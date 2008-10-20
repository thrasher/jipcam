/**
 * jipCam : The Java IP Camera Project
 * Copyright (C) 2005-2008 Jason Thrasher
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

import java.awt.Dimension;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Camera API for Axis 2100 / 2120 camera , Axis Revision: 1.14, Date:
 * 2004-April-22 This is the "HTTP API ver.1" as published by Axis
 * Communications. This class is not a full implementation of the API, but many
 * useful methods are provided.
 * 
 * See full implementation of the Axis Camera API here:
 * http://www.axis.com/techsup/cam_servers/dev/cam_http_api.htm#api_blocks_general_setparam
 * 
 * @author Jason Thrasher <a
 *         href="mailto:jason@coachthrasher.com">jason@coachthrasher.com</a>
 * @version $Revision$
 */
public class CameraAPI {
	/**
	 * The request has succeeded, but an application error can still occur,
	 * which will be returned as an application error code.
	 */
	public static final int HTTP_OK = 200;

	/**
	 * The server has fulfilled the request, but there is no new information to
	 * send back.
	 */
	public static final int HTTP_NO_CONTENT = 204;

	/**
	 * The request had bad syntax or was inherently impossible to be satisfied.
	 */
	public static final int HTTP_BAD_REQUEST = 400;

	/**
	 * The request requires user authentication or the authorization has been
	 * refused.
	 */
	public static final int HTTP_UNAUTHORIZED = 401;

	/**
	 * The server has not found anything matching the request.
	 */
	public static final int HTTP_NOT_FOUND = 404;

	/**
	 * The request could not be completed due to a conflict with the current
	 * state of the resource.
	 */
	public static final int HTTP_CONFLICT = 409;

	/**
	 * The server encountered an unexpected condition that prevented it from
	 * fulfilling the request.
	 */
	public static final int HTTP_INTERNAL_ERROR = 500;

	/**
	 * The server is unable to handle the request due to temporary overload.
	 */
	public static final int HTTP_SERVICE_UNAVAILABLE = 503;
	/**
	 * Some cameras may also support /axis-cgi/admin/paramlist.cgi for parameter
	 * lists (Axis 2130).
	 */
	protected static String REQ_PROPS = "/axis-cgi/admin/getparam.cgi";
	protected static String REQ_IMAGE = "/jpg/CAM_NUMBER/image.jpg";
	protected static String REQ_MJPEG = "/mjpg/CAM_NUMBER/video.mjpg";
	protected static String REQ_MJPEG_CGI = "/axis-cgi/mjpg/video.cgi";
	protected static String REQ_RESTART = "/axis-cgi/admin/restart.cgi";
	protected static String REQ_FAC_DEFAULT = "/axis-cgi/admin/factorydefault.cgi";
	protected static String REQ_SET = "/axis-cgi/admin/setparam.cgi";
	protected static String REQ_SERVER_REPORT = "/axis-cgi/admin/serverreport.cgi";
	protected static String REQ_SYSTEM_LOG = "/axis-cgi/admin/systemlog.cgi";
	protected static String MJPEG_BOUNDARY = "--myboundary";

	/**
	 * image resolution property
	 */
	protected static String P_IMAGE_RESOLUTION = "root.Image.Resolution";
	/**
	 * frames per second property
	 */
	protected static String P_APPWIZ_POSTRATE = "root.Appwiz.PostRate";
	/**
	 * camera product full name
	 */
	protected static String P_BRAND_PRODFULLNAME = "root.BRAND.PRODFULLNAME";
	/**
	 * camera product full name, different caps used in Axis 205, 206M, and 207
	 */
	protected static String P_BRAND_PRODFULLNAME_2 = "root.Brand.ProdFullName";

	protected HttpClient mClient;
	protected URL mCamUrl;
	protected Logger mLog;
	private Properties camPropsCache = null; // cache of camera properties

	/**
	 * Create a new camera api connection for HTTP based communication.
	 * 
	 * @param camUrl
	 *            for the Axis camera
	 * @param timeout
	 *            milliseconds to wait for the camera to respond before failing
	 * @param username
	 *            to login with
	 * @param password
	 *            to login with
	 */
	public CameraAPI(URL camUrl, int timeoutMillis, String username,
			String password) {
		// simple
		this(camUrl);

		// set the client timeout
		if (timeoutMillis >= 0) {
			// mClient.setConnectionTimeout(timeoutMillis);
			HttpConnectionManager conManager = mClient
					.getHttpConnectionManager();
			HttpConnectionParams params = conManager.getParams();
			params.setConnectionTimeout(timeoutMillis);

		}

		// set the authentication details
		if ((username != null) && (password != null)) {
			Credentials defaultcreds = new UsernamePasswordCredentials(
					username, password);
			// mClient.getState().setCredentials(null, camUrl.getHost(),
			// defaultcreds);
			AuthScope as = new AuthScope(camUrl.getHost(), camUrl.getPort());
			mClient.getState().setCredentials(as, defaultcreds);

			// set use of preemptive authentication
			// mClient.getState().setAuthenticationPreemptive(true);
			mClient.getParams().setAuthenticationPreemptive(true);
		}

		// To enable preemptive authentication by default
		// for all newly created HttpState's,
		// a system property can be set, as shown below.
		// System.setProperty(HttpState.PREEMPTIVE_PROPERTY, "true");
	}

	/**
	 * Create a new camera api connection for HTTP based communication.
	 * 
	 * @param camUrl
	 *            for the Axis camera
	 */
	public CameraAPI(URL camUrl) {
		mLog = Logger.getLogger(this.getClass());
		mCamUrl = camUrl;
		mClient = new HttpClient(new MultiThreadedHttpConnectionManager());
	}

	/**
	 * Restart server Note: This request requires administrator access
	 * (administrator authorization).
	 * 
	 * @throws java.io.IOException
	 */
	public void restart() throws IOException {
		mLog
				.log(Level.INFO, "restarting camera at "
						+ mCamUrl.toExternalForm());

		GetMethod get = null;

		try {
			URL url = new URL(mCamUrl, REQ_RESTART);

			// System.out.println(url.toExternalForm());
			get = new GetMethod(url.toExternalForm());
			get.setFollowRedirects(true);

			int iGetResultCode = mClient.executeMethod(get);

			if (iGetResultCode != this.HTTP_OK) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ get.getStatusText());
			}
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}
	}

	/**
	 * Reset camera to factory default settings. All confiration parameters are
	 * rest to factory defaults. Note: This request requires administrator
	 * access (administrator authorization).
	 * 
	 * @throws java.io.IOException
	 */
	public void factoryDefault() throws IOException {
		GetMethod get = null;

		try {
			URL url = new URL(mCamUrl, REQ_FAC_DEFAULT);

			// System.out.println(url.toExternalForm());
			get = new GetMethod(url.toExternalForm());
			get.setFollowRedirects(true);

			int iGetResultCode = mClient.executeMethod(get);

			if (iGetResultCode != this.HTTP_OK) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ get.getStatusText());
			}
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}
	}

	/**
	 * This CGI request generates and returns a server report. This report is
	 * useful as an input when requesting support. The report includes product
	 * information, parameter settings and system logs. Note: This request
	 * requires administrator access (administrator authorization).
	 * 
	 * @return server report text
	 * @throws java.io.IOException
	 */
	public String serverReport() throws IOException {
		GetMethod get = null;
		int iGetResultCode = -1;
		String report = null;

		try {
			URL url = new URL(mCamUrl, REQ_SERVER_REPORT);

			// System.out.println(url.toExternalForm());
			get = new GetMethod(url.toExternalForm());
			get.setFollowRedirects(true);

			iGetResultCode = mClient.executeMethod(get);

			if (iGetResultCode == this.HTTP_OK) {
				Reader reader = new InputStreamReader(new BufferedInputStream(
						get.getResponseBodyAsStream()));
				StringBuffer sb = new StringBuffer();
				char[] buff = new char[1024];
				int len = 0;

				while ((len = reader.read(buff)) != -1) {
					sb.append(buff, 0, len);
				}

				report = sb.toString();
			} else {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ get.getStatusText());
			}
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}

		return report;
	}

	/**
	 * Get system log information Note: This request requires administrator
	 * access (administrator authorization). Note: The response is
	 * product/release-dependent.
	 * 
	 * @throws java.io.IOException
	 * @return
	 */
	public String systemLog() throws IOException {
		GetMethod get = null;
		int iGetResultCode = -1;
		String log = null;

		try {
			URL url = new URL(mCamUrl, this.REQ_SYSTEM_LOG);

			// System.out.println(url.toExternalForm());
			get = new GetMethod(url.toExternalForm());
			get.setFollowRedirects(true);

			iGetResultCode = mClient.executeMethod(get);

			if (iGetResultCode == this.HTTP_OK) {
				Reader reader = new InputStreamReader(new BufferedInputStream(
						get.getResponseBodyAsStream()));
				StringBuffer sb = new StringBuffer();
				char[] buff = new char[1024];
				int len = 0;

				while ((len = reader.read(buff)) != -1) {
					sb.append(buff, 0, len);
				}

				log = sb.toString();
			} else {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ get.getStatusText());
			}
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}

		return log;
	}

	/**
	 * Set camera server configuration parameters. Setting a property with the
	 * incorrect capitalization will fail. Capitalization must match the
	 * property set returned from "getProperties()".
	 * 
	 * @param value
	 * @param name
	 * @throws java.io.IOException
	 */
	public void setProperty(String name, String value) throws IOException {
		camPropsCache = null; // invalidate the properties cache

		PostMethod post = null;

		try {
			URL url = new URL(mCamUrl, REQ_SET);

			post = new PostMethod(url.toExternalForm());
			post.setFollowRedirects(true);

			post.addParameter(name, value);

			int iGetResultCode = mClient.executeMethod(post);

			if (iGetResultCode != this.HTTP_OK) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ post.getStatusText());
			}
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		} finally {
			if (post != null) {
				post.releaseConnection();
			}
		}
	}

	/**
	 * Get all camera properties. Note that the capitalization of property names
	 * seems to be different for some cameras.
	 * 
	 * @throws java.io.IOException
	 * @return camera's configuration properties
	 */
	public Properties getProperties() throws IOException {
		if (camPropsCache != null) {
			// assume the cache if valid if not null
			mLog.debug("useing cached camera properties");

			return camPropsCache;
		}

		mLog.debug("refreshing camera properties cache");

		GetMethod get = null;

		try {
			URL url = new URL(mCamUrl, REQ_PROPS);

			// System.out.println(url.toExternalForm());
			get = new GetMethod(url.toExternalForm());
			get.setFollowRedirects(true);

			// check response code
			int iGetResultCode = mClient.executeMethod(get);

			if (iGetResultCode >= HTTP_BAD_REQUEST) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ get.getStatusText());
			}

			// read the properties
			camPropsCache = new Properties();
			camPropsCache.load(get.getResponseBodyAsStream());

		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}

		return camPropsCache;
	}

	/**
	 * Query a specific property from the camera. This is a wrapper method for
	 * <code>getProperties().getProperty(name)</code>.
	 * 
	 * To avoid running into camera property capitalization issues, this method
	 * will find the given propername without being case sensitive.
	 * 
	 * @param name
	 *            of the property
	 * @return The property value, or null if not found.
	 * @throws java.io.IOException
	 */
	public String getProperty(String name) throws IOException {
		Properties props = getProperties();
		Set keySet = props.keySet();
		for (Iterator iter = keySet.iterator(); iter.hasNext();) {
			String keyName = (String) iter.next();
			if (name.equalsIgnoreCase(keyName)) {
				return props.getProperty(keyName);
			}
		}
		return null;
	}

	/**
	 * Get the camera's currently set resolution.
	 * 
	 * @throws java.io.IOException
	 * @return The width-height current image dimensions (x=w, y=h).
	 */
	public Dimension getImageDimension() throws IOException {
		String resS = getProperty(P_IMAGE_RESOLUTION);
		StringTokenizer st = new StringTokenizer(resS, "x");

		int w = Integer.parseInt(st.nextToken());
		int h = Integer.parseInt(st.nextToken());

		return new Dimension(w, h);
	}

	/**
	 * Is this the Frames Per Second, or the image posting rate for ftp
	 * upload???
	 * 
	 * @return the appwiz post rate
	 * @throws IOException
	 */
	public int getAppwizPostRate() throws IOException {
		return Integer.parseInt(getProperty(P_APPWIZ_POSTRATE));
	}

	/**
	 * Get the product name for this camera, if it exists.
	 * 
	 * @return the product name, or null if not found
	 * @throws IOException
	 */
	public String getProductFullName() throws IOException {
		return getProperty(P_BRAND_PRODFULLNAME);
	}

	/**
	 * Get a single image from the camera. The image returned is parsed from the
	 * JPEG data in the camera's response.
	 * 
	 * @param cameraNumber
	 *            the camera to get the image from (default is "1")
	 * @throws java.io.IOException
	 * @return the camera's current view
	 */
	public Image getImage(int cameraNumber) throws IOException {
		GetMethod get = null;
		Image image = null;

		// insert the camera number
		String request = REQ_IMAGE.replace("CAM_NUMBER", Integer
				.toString(cameraNumber));

		try {
			URL url = new URL(mCamUrl, request);

			// System.out.println(url.toExternalForm());
			get = new GetMethod(url.toExternalForm());
			get.setFollowRedirects(true);

			// check response code
			int iGetResultCode = mClient.executeMethod(get);

			if (iGetResultCode >= HTTP_BAD_REQUEST) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ get.getStatusText());
			}

			// System.out.println("iGetResultCode = " + iGetResultCode);
			// Read from an input stream
			InputStream is = new BufferedInputStream(get
					.getResponseBodyAsStream());
			image = ImageIO.read(is);
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}

		// Use a label to display the image in swing UI
		// javax.swing.JFrame frame = new javax.swing.JFrame();
		// javax.swing.JLabel label = new javax.swing.JLabel(new
		// javax.swing.ImageIcon(image));
		// frame.getContentPane().add(label, java.awt.BorderLayout.CENTER);
		// frame.pack();
		// frame.setVisible(true);
		return image;
	}

	/**
	 * Get a single image from the default camera. (camera number "1")
	 * 
	 * @return
	 * @throws IOException
	 */
	public Image getImage() throws IOException {
		return getImage(1);
	}

	/**
	 * Returns a multipart JPEG image stream with the default resolution and
	 * compression as defined in the system configuration.
	 * 
	 * @param cameraNumber
	 *            to request, the first camera is always "1"
	 * @return
	 * @throws IOException
	 */
	public InputStream getMjpeg(int cameraNumber) throws IOException {
		GetMethod get = null;
		InputStream in = null;

		// insert the camera number
		String request = REQ_MJPEG.replace("CAM_NUMBER", Integer
				.toString(cameraNumber));

		try {
			URL url = new URL(mCamUrl, request);

			// System.out.println(url.toExternalForm());
			get = new GetMethod(url.toExternalForm());
			get.setFollowRedirects(true);

			// check response code
			int iGetResultCode = mClient.executeMethod(get);

			if (iGetResultCode >= HTTP_BAD_REQUEST) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ get.getStatusText());
			}

			// For this multipart response, parse out the boundary info
			Header contentType = get.getResponseHeader("Content-Type");
			HeaderElement[] elements = contentType.getElements();

			for (int i = 0; i < elements.length; i++) {
				NameValuePair boundaryNVP = elements[i]
						.getParameterByName("boundary");

				if (boundaryNVP != null) {
					MJPEG_BOUNDARY = boundaryNVP.getValue();
				}
			}

			// System.out.println("iGetResultCode = " + iGetResultCode);
			// Read from an input stream
			in = new BufferedInputStream(get.getResponseBodyAsStream());
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		}

		return in;
	}

	/**
	 * Get the MJPEG from the default camera, or the first camera in a
	 * multi-camera system.
	 * 
	 * @return
	 * @throws IOException
	 */
	public InputStream getMjpeg() throws IOException {
		return getMjpeg(1);
	}

	public static String getMjpegBoundary() {
		return MJPEG_BOUNDARY;
	}

	/**
	 * 
	 * 
	 * @param width
	 *            of the images to return
	 * @param height
	 *            of the images to return
	 * @param compression
	 *            of the jpeg images, from 0-100
	 * @deprecated This request makes too many assumptions about the camera
	 * @see http://www.axis.com/techsup/cam_servers/dev/cam_http_api.htm#api_blocks_jpeg_mjpg_mjpg_request
	 */
	public InputStream getMjpegCgi(int width, int height, int compression,
			int nbrofframes, int req_fps, boolean showlength,
			boolean deltatime, boolean date, boolean clock, boolean text)
			throws IOException {
		return getMjpegCgi(width, height, -1, compression, 100, clock, date,
				false, text, -1, showlength, -1, nbrofframes, req_fps, true,
				deltatime, 5);
	}

	/**
	 * Request MJPEG video using a CGI request. Not all cameras support all
	 * features of this request.
	 * 
	 * @throws java.io.IOException
	 * @return The raw MJPEG data from the HTTP response body.
	 * @param width
	 *            of the image, in pixels, must be valid for the camera
	 * @param height
	 *            of the image, in pixels, must be valid for the camera
	 * @param camera
	 *            used for multi-camera servers, N = 1,2,.... set to 0 for
	 *            one-camera servers.
	 * @param compression
	 *            valid from 0-100
	 * @param colorlevel
	 *            sets level of color or grayscale. 0 = grayscale, 100 = full
	 *            color. Note: This value is internally mapped and is therefore
	 *            product-dependent.
	 * @param clock
	 *            set true to show, false to hide
	 * @param date
	 *            set true to show, false to hide
	 * @param quad
	 *            set true for quad image, false for normal
	 * @param text
	 *            set true to show, false to hide
	 * @param rotation
	 *            set image orientation, N = 0, 90, 180, 270
	 * @param showlength
	 *            Content-Length is added to the HTTP-header and in the boundary
	 *            section, between the images, set true to show, false to hide.
	 * @param duration
	 *            specifies how many seconds the video will be generated and
	 *            pushed to the client, N >= 0
	 * @param nbrofframes
	 *            specifies how many frames server will generate and push, N > 0
	 * @param fps
	 *            Frames Per Seconds of the camera, within the camera's spec, N >
	 *            0
	 * @param fpsRequired
	 *            modifies fps: set true to REQUIRE the given FPS, or false to
	 *            indicated DESIRED fps
	 * @param deltatime
	 *            set true to show, false to hide
	 * @param timeout
	 *            seconds to timeout of the session. If a connection is blocked
	 *            for this length of time, the session will be closed by the
	 *            server.
	 * @see http://www.axis.com/techsup/cam_servers/dev/cam_http_api.htm#api_blocks_jpeg_mjpg_mjpg_request
	 */
	public InputStream getMjpegCgi(int width, int height, int camera,
			int compression, int colorlevel, boolean clock, boolean date,
			boolean quad, boolean text, int rotation, boolean showlength,
			int duration, int nbrofframes, int fps, boolean fpsRequired,
			boolean deltatime, int timeout) throws IOException {
		PostMethod post = null;
		InputStream in = null;

		try {
			URL url = new URL(mCamUrl, REQ_MJPEG_CGI);

			post = new PostMethod(url.toExternalForm());
			post.setFollowRedirects(false);

			if ((width > 0) && (height > 0)) {
				post.addParameter("resolution", width + "x" + height);
			}

			if (camera > 0) {
				post.addParameter("camera", Integer.toString(camera));
			}

			if ((0 <= compression) && (compression <= 100)) {
				post.addParameter("compression", Integer.toString(compression));
			}

			if ((0 <= colorlevel) && (colorlevel <= 100)) {
				post.addParameter("colorlevel", Integer.toString(colorlevel));
			}

			if (clock) {
				post.addParameter("clock", "1");
			}

			if (date) {
				post.addParameter("date", "1");
			}

			if (quad) {
				post.addParameter("quad", "1");
			}

			if (text) {
				post.addParameter("text", "1");
			}

			if ((rotation == 0) || (rotation == 90) || (rotation == 180)
					|| (rotation == 270)) {
				post.addParameter("rotation", Integer.toString(rotation));
			}

			if (showlength) {
				post.addParameter("showlength", "1");
			} else {
				post.addParameter("showlength", "0");
			}

			if (duration >= 0) {
				post.addParameter("duration", Integer.toString(duration));
			}

			if (nbrofframes > 0) {
				post.addParameter("nbrofframes", Integer.toString(nbrofframes));
			}

			if (fps > 0) {
				if (fpsRequired) {
					// high priority stream
					post.addParameter("req_fps", Integer.toString(fps)); // required
					// fps
				} else {
					// less important stream
					post.addParameter("des_fps", Integer.toString(fps)); // desired
					// fps
				}
			}

			if (deltatime) {
				post.addParameter("deltatime", "1");
			}

			if (timeout > 0) {
				post.addParameter("timeout", Integer.toString(timeout));
			}

			// check response code
			int iGetResultCode = mClient.executeMethod(post);

			if (mLog.isDebugEnabled()) {
				StringBuffer uri = new StringBuffer(post.getURI().toString());
				NameValuePair[] nvp = post.getParameters();

				if (nvp.length > 0) {
					uri.append("?");
				}

				for (int i = 0; i < nvp.length; i++) {
					uri.append(nvp[i].getName());
					uri.append("=");
					uri.append(nvp[i].getValue());

					if ((i + 1) != nvp.length) {
						uri.append("&");
					}

					mLog.debug("camera query: " + nvp[i].getName() + "="
							+ nvp[i].getValue());
				}

				mLog.debug(uri);
			}

			if (iGetResultCode >= HTTP_BAD_REQUEST) {
				throw new IOException("HTTP " + iGetResultCode + " "
						+ post.getStatusText());
			}

			// For this multipart response, parse out the boundary info
			Header contentType = post.getResponseHeader("Content-Type");
			HeaderElement[] elements = contentType.getElements();

			// debug all http headers
			if (mLog.isDebugEnabled()) {
				Header[] heads = post.getResponseHeaders();

				for (int i = 0; i < heads.length; i++) {
					mLog.debug("response header: " + heads[i].toString());
				}
			}

			for (int i = 0; i < elements.length; i++) {
				NameValuePair boundaryNVP = elements[i]
						.getParameterByName("boundary");

				if (boundaryNVP != null) {
					MJPEG_BOUNDARY = boundaryNVP.getValue();
				}
			}

			// Read from an input stream
			in = new BufferedInputStream(post.getResponseBodyAsStream());
		} catch (MalformedURLException murle) {
			throw new IOException(murle.getMessage());
		}

		return in;
	}

	/**
	 * Unit test.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			URL cam = new URL("http://10.0.0.90");

			CameraAPI api = new CameraAPI(cam, -1, null, null);

			api.getProperties().store(System.out, null);
			System.out.println();
			System.out.println(api.getImageDimension());
			System.out.println(api.getAppwizPostRate());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
