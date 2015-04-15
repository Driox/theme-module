package play.modules.theme;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ETAG;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.IF_NONE_MATCH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.i18n.Messages;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.utils.Utils;
import play.vfs.VirtualFile;

public class TemplateThemePlugin extends PlayPlugin {

	private static final ThreadLocal<Boolean> beenThere = new ThreadLocal<Boolean>();
	private final static String SESSION_OVERRIDE_CONTEXT_NAME = "OVERRIDE_CONTEXT_NAME";

	@Override
	public String getMessage(String locale, Object key, Object... args) {
		String msg = getMessageFromResourceFile(locale, key, args);

		if ((msg == null || msg.length() == 0 || msg.equals(key)) && (endWithMaj(key.toString()) || endWithNull(key.toString()))) {
			String newKey = key.toString();
			newKey = newKey.substring(0, newKey.lastIndexOf('.'));
			msg = getMessageFromResourceFile(locale, newKey, args);
		}
		return msg;
	}

	private static boolean endWithMaj(String key) {
		return key.matches(".*\\.[A-Z0-9_]+$");
	}

	private static boolean endWithNull(String key) {
		return key.endsWith(".null");
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

	@Override
	public void onApplicationStart() {
		Logger.debug("TemplateThemePlugin started");
	}

	@Override
	public Template loadTemplate(VirtualFile file) {
		if (beenThere.get() == Boolean.TRUE) {
			return super.loadTemplate(file);
		}

		beenThere.set(Boolean.TRUE);
		try {
			String ctx = getCtx(Request.current());
			String filePath = file.getRealFile().getPath();
			String ctxFilePath = filePath.replaceAll("views", "views/theme/" + ctx);

			File ctxFile = new File(ctxFilePath);
			if (ctxFile.exists()) {
				return TemplateLoader.load(VirtualFile.open(ctxFile));
			}
		} finally {
			beenThere.set(null);
		}
		return super.loadTemplate(file);
	}

	@Override
	public boolean serveStatic(VirtualFile file, Request request, Response response) {
		String ctx = getCtx(request);
		if (ctx == null || ctx.length() == 0)
			return super.serveStatic(file, request, response);

		String filePath = file.getRealFile().getPath();
		String filePathWithCtx = getFileWithCtx(filePath, request, true);
		File ctxFile = new File(filePathWithCtx);

		try {
			byte[] bytes = VirtualFile.open(ctxFile).content();
			String contentType = MimeTypes.getContentType(file.getName());
			addEtag(request, response, file.getRealFile());
			response.encoding = "UTF-8";
			response.contentType = contentType;
			response.setHeader("Content-Type", contentType);
			response.status = 200;
			response.out.write(bytes);

			return true;
		} catch (Exception e) {
			Logger.error("Erreur lors de serveStatic pour le path " + filePathWithCtx, e);
			return super.serveStatic(file, request, response);
		}
	}

	/**
	 * Methode from Play Framework that is private and don't call when serveStatic overload
	 * We do little change to adjust to the context, mainly HttpRequest -> Request and same for response
	 */
	private static Response addEtag(Request nettyRequest, Response httpResponse, File file) {
		if (Play.mode == Play.Mode.DEV) {
			httpResponse.setHeader(CACHE_CONTROL, "no-cache");
		} else {
			// Check if Cache-Control header is not set
			if (httpResponse.getHeader(CACHE_CONTROL) == null) {
				String maxAge = Play.configuration.getProperty("http.cacheControl", "3600");
				if (maxAge.equals("0")) {
					httpResponse.setHeader(CACHE_CONTROL, "no-cache");
				} else {
					httpResponse.setHeader(CACHE_CONTROL, "max-age=" + maxAge);
				}
			}
		}
		boolean useEtag = Play.configuration.getProperty("http.useETag", "true").equals("true");
		long last = file.lastModified();
		final String etag = "\"" + last + "-" + file.hashCode() + "\"";
		if (!isModified(etag, last, nettyRequest)) {
			if (nettyRequest.method.equals(HttpMethod.GET)) {
				httpResponse.status = HttpResponseStatus.NOT_MODIFIED.getCode();
			}
			if (useEtag) {
				httpResponse.setHeader(ETAG, etag);
			}

		} else {
			httpResponse.setHeader(LAST_MODIFIED, Utils.getHttpDateFormatter().format(new Date(last)));
			if (useEtag) {
				httpResponse.setHeader(ETAG, etag);
			}
		}
		return httpResponse;
	}

	/**
	 * Methode from Play Framework that is private and don't call when serveStatic overload
	 * We do little change to adjust to the context, mainly HttpRequest -> Request and same for response
	 */
	public static boolean isModified(String etag, long last, Request nettyRequest) {

		if (nettyRequest.headers.containsKey(IF_NONE_MATCH)) {
			final String browserEtag = nettyRequest.headers.get(IF_NONE_MATCH).value();
			if (browserEtag.equals(etag)) {
				return false;
			}
			return true;
		}

		if (nettyRequest.headers.containsKey(IF_MODIFIED_SINCE)) {
			final String ifModifiedSince = nettyRequest.headers.get(IF_MODIFIED_SINCE).value();

			if (!StringUtils.isEmpty(ifModifiedSince)) {
				try {
					Date browserDate = Utils.getHttpDateFormatter().parse(ifModifiedSince);
					if (browserDate.getTime() >= last) {
						return false;
					}
				} catch (ParseException ex) {
					Logger.warn("Can't parse HTTP date", ex);
				}
				return true;
			}
		}
		return true;
	}

	private static String getCtx(Request request) {
		if (request == null)
			return "";

		Scope.Session session = Scope.Session.current();
		if (session != null && session.contains(SESSION_OVERRIDE_CONTEXT_NAME)) {
			return session.get(SESSION_OVERRIDE_CONTEXT_NAME).toLowerCase();
		}

		String domain = request.domain.toLowerCase();

		if (domain.endsWith("particeep.com")) {
			return getPrefixCtx(domain);
		} else {
			return getFullUrlCtx(domain);
		}
	}
	
	private static String getFullUrlCtx(String domain) {
		String listString = Play.configuration.getProperty("particeep.ctx.list");
		String[] list = listString.split(",");

		for (String ctx : list) {
			String domains = Play.configuration.getProperty("particeep.ctx." + ctx);
			if (domains != null && domains.contains(domain)) {
				return ctx;
			}
		}

		return "";
	}

	private static String getPrefixCtx(String domain) {
		if (domain.indexOf('.') != -1) {
			domain = domain.substring(0, domain.indexOf('.'));
		}

		if (domain.startsWith("test-")) {
			domain = domain.substring(5);
		}

		Object config = Play.configuration.get("particeep.ctx.particeep");
		if (config != null) {
			String[] particeepDomain = config.toString().split(",");
			if (domain.equals("localhost"))
				domain = "test";
			else if (ArrayUtils.contains(particeepDomain, domain))
				domain = "";
		}

		return domain;
	}

	private static String getFileWithCtx(String filePath, Request request, Boolean isStatic) {
		String ctx = getCtx(request);
		String ctxFilePath = null;
		if (isStatic)
			ctxFilePath = filePath.replaceAll("public", "public/theme/" + ctx);
		else
			ctxFilePath = filePath.replaceAll("views", "views/theme/" + ctx);

		File ctxFile = new File(ctxFilePath);
		if (ctxFile.exists()) {
			return ctxFilePath;
		} else {
			return filePath;
		}
	}

}