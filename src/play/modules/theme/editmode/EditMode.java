package play.modules.theme.editmode;

import play.Play;
import play.modules.theme.core.MessageLoader;
import play.modules.theme.utils.ModuleUtils;

public class EditMode {
	private final static boolean IS_ACTIVE = isActive();

	private static final String template = buildTemplate();
	private static final String[] escaped_keys = Play.configuration.getProperty("editmod.escape", "placeholder,meta,alt").split(",");

	public static boolean isEnable() {
		return IS_ACTIVE && isEditModeEnable();
	}

	public static String formatMsgForEdition(String ctx, String locale, Object key, Object... args) {
		if (isAllowedToChangeKey(key)) {
			return template.replaceAll("%key%", key.toString()).replaceAll("%msg%", MessageLoader.getBaseMessage(ctx, locale, key));
		} else {
			return MessageLoader.getMessageByContext(ctx, locale, key, args);
		}
	}

	private static Boolean isActive() {
		String isEnable = Play.configuration.getProperty("editmod.enable", "false");
		return new Boolean(isEnable);
	}

	private static boolean isAllowedToChangeKey(Object key) {
		if (key == null || key.toString().length() == 0) {
			return false;
		}

		String keyString = key.toString();
		for (String s : escaped_keys) {
			if (keyString.contains(s)) {
				return false;
			}
		}

		return true;
	}

	private static boolean isEditModeEnable() {
		return ModuleUtils.getBooleanFromRenderArg("editModeEnable");
	}

	// TODO : find a way to externalise this template.
	// Using TemplateLoader.load don't work because of vaudoo template by Play!
	private static String buildTemplate() {
		return Play.configuration.getProperty("editmod.template");
	}

}
