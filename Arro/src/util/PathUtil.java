package util;

public class PathUtil {
	public static String truncExtension(String s) {
		String ret = s;
		if(s.endsWith(".xml")) {
			ret = s.substring(0, s.length() - 4 /* sizeof ".amsg" */);
			s = ret;
		}
		if(s.endsWith(".anod")) {
			ret = s.substring(0, s.length() - 5 /* sizeof ".amsg" */);
			s = ret;
		} else if(s.endsWith(".adev")) {
			ret = s.substring(0, s.length() - 5 /* sizeof ".amsg" */);
			s = ret;
		} else 	if(s.endsWith(".amsg")) {
			ret = s.substring(0, s.length() - 5 /* sizeof ".amsg" */);
			s = ret;
		}
		return ret;
	}
}
