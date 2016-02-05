package play.modules.theme.utils;

import play.mvc.Scope.RenderArgs;

public class ModuleUtils {

	public static Object getFromRenderArg(String paramName) {
		Object result = null;
		RenderArgs renderArg = RenderArgs.current();
		if (renderArg != null) {
			result = renderArg.get(paramName);
		}

		return result;
	}

	public static String getStringFromRenderArg(String paramName) {
		String result = null;
		Object argCtx = getFromRenderArg(paramName);
		if (argCtx != null) {
			result = argCtx.toString();
		}

		return result;
	}

	public static Boolean getBooleanFromRenderArg(String paramName) {
		Boolean result = false;
		Object argCtx = getFromRenderArg(paramName);
		if (argCtx != null) {
			result = new Boolean(argCtx.toString());
		}

		return result;
	}
}
