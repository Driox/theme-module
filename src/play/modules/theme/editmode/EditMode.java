package play.modules.theme.editmode;

import play.Play;
import play.modules.theme.core.MessageLoader;
import play.modules.theme.utils.ModuleUtils;

public class EditMode {
	private final static boolean IS_ACTIVE = isActive();

	private static final String template = buildTemplate();

	public static boolean isEnable() {
		return IS_ACTIVE && isEditModeEnable();
	}

	public static String formatMsgForEdition(String ctx, String locale, Object key, Object... args) {
		if (isAllowedToChangeKey(key)) {
			return template.replaceAll("%key%", key.toString()).replaceAll("%msg%", MessageLoader.getMessageByContext(ctx, locale, key, args));
		} else {
			return MessageLoader.getMessageByContext(ctx, locale, key, args);
		}
	}

	private static Boolean isActive() {
		String isEnable = Play.configuration.getProperty("editmod.enable", "false");
		return new Boolean(isEnable);
	}

	private static boolean isAllowedToChangeKey(Object key) {
		return key != null
				&& !key.toString().contains("placeholder")
				&& !key.toString().contains("meta.")
				&& !key.toString().contains("alt");
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
