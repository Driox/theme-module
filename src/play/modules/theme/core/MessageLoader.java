package play.modules.theme.core;

import play.i18n.Messages;

public class MessageLoader {

	public static String getMessageByContext(String ctx, String locale, Object key, Object... args) {
		String newKey = key.toString();
		if (endWithNull(newKey)) {
			newKey = removeSuffix(newKey);
		}

		if (!endWithMaj(newKey)) {
			newKey = new StringBuilder(newKey).append(".").append(ctx).toString();
		}

		String msg = getMessageFromResourceFile(locale, newKey, args);

		if (hasNotFoundMsg(newKey, msg)) {
			newKey = removeSuffix(newKey);
			msg = getMessageFromResourceFile(locale, newKey, args);
		}

		if (play.Play.runingInTestMode() && !newKey.isEmpty() && hasNotFoundMsg(newKey, msg)) {
			return "error.messages-tr:" + key;
		}

		return msg;
	}

	private static boolean endWithMaj(String key) {
		return key.matches(".*\\.[A-Z0-9_]+$");
	}

	private static boolean endWithNull(String key) {
		return key.endsWith(".null");
	}

	private static String removeSuffix(String key) {
		return key.substring(0, key.lastIndexOf('.'));
	}

	private static boolean hasNotFoundMsg(String key, String msg) {
		key = key.replaceAll("%%", "%");
		return msg == null || msg.length() == 0 || msg.equals(key);
	}

	private static String getMessageFromResourceFile(String locale, Object key, Object... args) {
		String value = null;
		if (key == null) {
			return "";
		}
		if (Messages.locales.containsKey(locale)) {
			value = Messages.locales.get(locale).getProperty(key.toString());
		}
		if (value == null) {
			value = Messages.defaults.getProperty(key.toString());
		}
		if (value == null) {
			value = key.toString();
		}

		return Messages.formatString(value, args);
	}

}
