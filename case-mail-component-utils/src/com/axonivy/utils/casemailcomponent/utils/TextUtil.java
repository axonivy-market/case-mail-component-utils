package com.axonivy.utils.casemailcomponent.utils;

public class TextUtil {

	/**
	 * Truncates the input text and adds "..." if it exceeds the max length.
	 * 
	 * @param text      The original string.
	 * @param maxLength The max allowed length before truncating.
	 * @return Truncated string with ellipsis if needed.
	 */
	public static String ellipsis(String text, int maxLength) {
		if (text == null || maxLength <= 0) {
			return "";
		}
		if (text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength) + "...";
	}
}
