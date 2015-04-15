package play.modules.theme;

import play.Play;

public class PropertyUtils {

	public static String getProperty(String key, String ctx) {
		String ctxKey = (new StringBuilder(key)).append(".").append(ctx).toString();
		String val = Play.configuration.getProperty(ctxKey);
		if (val == null || val.length() == 0) {
			val = Play.configuration.getProperty(key);
		}

		return val;
	}
}
