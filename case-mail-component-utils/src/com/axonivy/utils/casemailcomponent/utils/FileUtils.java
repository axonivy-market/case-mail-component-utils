package com.axonivy.utils.casemailcomponent.utils;

import java.net.URLConnection;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class that provides convenience methods to for the Ivy
 * {@link ch.ivyteam.ivy.scripting.objects.File} type.
 */
public class FileUtils {

	// Fallback content type, to be used when the conten't type can't be guessed
	// from the file name
	private static final String FALLBACK_CONTENT_TYPE = "application/octet-stream";

	/**
	 * Determines the mime type of the filename with
	 * {@link URLConnection#guessContentTypeFromName(String)}.
	 *
	 * <p>
	 * Falls back to {@link #FALLBACK_CONTENT_TYPE} if no mime type could be
	 * determined.
	 * </p>
	 *
	 * @param filename
	 * @return
	 */
	public static String getContentType(String filename) {
		String contentType = URLConnection.guessContentTypeFromName(filename);

		if (StringUtils.isBlank(contentType)) {
			contentType = FALLBACK_CONTENT_TYPE;
		}

		return contentType;
	}
}
