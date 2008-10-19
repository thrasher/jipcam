package net.sf.jipcam.axis.emulator;

import java.io.File;
import java.util.Properties;

import net.sf.jipcam.axis.Axis2120CaptureDevice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This bean maintains the state of the "Virtual Camera"
 * 
 * @author Jason Thrasher
 */
public class Camera {
	private static final Log log = LogFactory.getLog(Camera.class);

	private File mjpeg;

	private String mjpegFile;

	private Properties properties;

	private int fps;

	public Camera() {
		try {
			properties = Axis2120CaptureDevice.getPropertyDefaults();
		} catch (Exception e) {
			log.fatal("failed to load props", e);
		}
	}

	/**
	 * @return the mjpeg
	 */
	public File getMjpeg() {
		return this.mjpeg;
	}

	/**
	 * @param mjpeg
	 *            the mjpeg to set
	 */
	public void setMjpeg(File mjpeg) {
		this.mjpeg = mjpeg;
	}

	/**
	 * @return the mjpegFile
	 */
	public String getMjpegFile() {
		return this.mjpegFile;
	}

	/**
	 * @param mjpegFile
	 *            the mjpegFile to set
	 */
	public void setMjpegFile(String mjpegFile) {
		this.mjpegFile = mjpegFile;
	}

	/**
	 * @return the properties
	 */
	public Properties getProperties() {
		return this.properties;
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * @return the fps
	 */
	public int getFps() {
		return this.fps;
	}

	/**
	 * @param fps
	 *            the fps to set
	 */
	public void setFps(int fps) {
		this.fps = fps;
	}

}
