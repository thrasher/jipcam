package net.sf.jipcam;

/**
 * Detect various camera formats based on their URL.  This class is a placeholder, and is not complete.
 * 
 * @author jason
 * @see http://www.crazypixels.com/support.htm
 * 
 */
public class CameraDetector {

	// Grand IP video server (MJPEG video stream)
	public final String GRAND_IP_VIDEO_SERVER = "http://<IP address>:port/GetData .cgi";

	// Sony SNC-RZ30N (MJPEG video stream)
	private final String SonySNCRZ30NCaptureDevice = "http://<IP address>/image";

	// Planet video server (MJPEG video stream)
	private final String PLANET_VIDEO_SERVER = "ttp://<IP address>:port/GetData .cgi";

	// Trust NW-7100 (MJPEG video stream)
	private final String TRUST_NW7100 = "http://<IP address>/video.cgi";

	// Panasonic BL-C30A (MJPEG video stream)
	private final String PANASONIC = "http://<IP address>:port/nphMotionJpeg?Resolution=320x240&Quality=Motion";

	// Genius IpCam Secure 300 (MJPEG video stream)
	private final String GENIUS_IPCAM = "http://<IP address>/video.cgi";

	// GadSpot NC1000-L10 & -W10 (MJPEG video stream)
	public final String GADSPOT = "http://<IP address>/GetData.cgi";

	// Orite IC-301 (MJPEG video stream)
	public final String ORITE = "http://<IP address>/GetData.cgi";

	// AXIS (MJPEG video stream)
	public final String AXIS_2 = "http://210.236.173.198/axis-cgi/mjpg/video.cgi";

	// AXIS
	public final String AXIS_1 = "http://atlantis.lkn.e-technik.tu-muenchen.de/axis-cgi/mjpg/video.cgi";

	// LevelOne WCS-2003 (MPEG4 video stream)
	public final String LEVELONE = "http://<IP address>:80/img/video.asf";

}
