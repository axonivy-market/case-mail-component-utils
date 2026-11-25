package com.axonivy.utils.casemailcomponent.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import ch.ivyteam.ivy.scripting.objects.DateTime;

public class DateUtil {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd.MM.yyyy, HH:mm:ss");
	private static final DateTimeFormatter MEDIUM_DATE_TIME_FORMATTER = DateTimeFormatter
			.ofLocalizedDateTime(FormatStyle.MEDIUM);

	public static String format(DateTime dateTime) {
		if (dateTime == null) {
			return "";
		}
		Date javaDate = dateTime.toJavaDate();
		return FORMATTER.format(javaDate);
	}

	/**
	 * Converts a {@link Date} to {@link Instant}
	 *
	 * @param date - date to convert
	 * @return converted instant
	 */
	public static Instant convertDateToInstant(Date date) {
		return date.toInstant();
	}

	/**
	 * Formats a {@link Instant} for the given {@link Locale} using the MEDIUM
	 * format style. It includes the time part.
	 *
	 * @param date   - date to convert
	 * @param locale - target Locale
	 * @return formatted date as String
	 */
	public static String formatInstantToMediumStyleWithTime(Instant date, Locale locale) {
		if (Objects.isNull(date)) {
			return "";
		}
		return MEDIUM_DATE_TIME_FORMATTER.withZone(ZoneId.systemDefault()).withLocale(locale).format(date);
	}
}
