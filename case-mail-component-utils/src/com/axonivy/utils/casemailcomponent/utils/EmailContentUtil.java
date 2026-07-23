package com.axonivy.utils.casemailcomponent.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.stream.Collectors;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.ParseException;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.mailstore.MessageService;
import com.axonivy.utils.casemailcomponent.Constants;
import com.axonivy.utils.casemailcomponent.enums.BpmErrorCode;
import com.axonivy.utils.casemailcomponent.service.BpmErrorService;

/**
 * Utils for working with {@link Message}s content
 *
 * @author ny.huynh
 */
public class EmailContentUtil {

	/**
	 * Get all content parts of type par:subType concatenated into a single
	 * {@link String} joining them using par:glue.
	 *
	 * @param message
	 * @param subType
	 * @param glue
	 * @param includeSubMessages
	 * @return a single string containing the concatenated text content of all
	 *         matching parts
	 */
	public static String getAllTextBySubtype(Message message, String subType, String glue, boolean includeSubMessages) {
		return MessageService.getAllParts(message, includeSubMessages, MessageService.isText(subType)).stream()
				.map(part -> extractTextFromPart(part)).collect(Collectors.joining(glue));
	}

	/**
	 * Extracts text content from a single MIME part using the appropriate character
	 * encoding.
	 *
	 * @param part
	 * @return the extracted text content as a string
	 */
	private static String extractTextFromPart(Part part) {
		try (InputStream content = part.getInputStream()) {
			final Charset charset = detectCharset(part.getContentType());
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(content, charset))) {
				return reader.lines().collect(Collectors.joining(Constants.SYSTEM_LINE_SEPARATOR));
			}
		} catch (IOException | MessagingException e) {
			BpmErrorService.get().throwBpmError(BpmErrorCode.RECEIVE_MAIL_CONTENT_ERROR, e);
		}
		return StringUtils.EMPTY;
	}

	/**
	 * Detects the character encoding from the content type of a MIME part. Falls
	 * back to UTF-8 if the charset is not specified or unsupported.
	 *
	 * @param contentType the content type string from the MIME part
	 * @return the detected or default character set
	 */
	private static Charset detectCharset(String contentType) {
		try {
			final ContentType ct = new ContentType(contentType);
			final String charsetName = ct.getParameter("charset");
			if (charsetName != null && Charset.isSupported(charsetName)) {
				return Charset.forName(charsetName);
			}
		} catch (ParseException | IllegalCharsetNameException | UnsupportedCharsetException e) {
			BpmErrorService.get().throwBpmError(BpmErrorCode.RECEIVE_MAIL_CONTENT_ERROR, e);
		}
		return StandardCharsets.UTF_8;
	}

	/**
	 * Checks whether the given text contains HTML tags.
	 * 
	 * @param text the input string to check
	 * @return {@code true} if the input contains HTML tags; {@code false} otherwise
	 */
	public static boolean isHtml(String text) {
		return text != null && text.trim().matches(Constants.HTML_REGEX);
	}

}
