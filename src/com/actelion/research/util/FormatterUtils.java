/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
 * CH-4123 Allschwil, Switzerland.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.util;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Formatter Utilidy class to format numbers and date/time
 * @author freyssj
 */
public class FormatterUtils {

	/**
	 * Format used to parse/format the date.
	 * The format can be set statically by setLocaleFormat
	 * @author Joel Freyss
	 *
	 */
	public static enum DateTimeFormat {
		AMERICAN("MM/dd/yy"),
		EUROPEAN("dd/MM/yy"),
		SWISS("dd.MM.yy"),
		YYYYMMDD("yyyy-MM-dd"),
		DDMMMYYYY("dd-MMM-yyyy");

		private String localeDateFormat;

		private DateTimeFormat(String localeDateFormat) {
			this.localeDateFormat = localeDateFormat;
		}
		public String getLocaleDateFormat() {
			return localeDateFormat;
		}

		/**
		 * Returns a string representation of the format.
		 * Must starts with name()
		 */
		@Override
		public String toString() {
			String res = name()  + " (" + localeDateFormat + ")";
			assert res.startsWith(name());
			return res;
		}
		public static DateTimeFormat get(String name) {
			for (DateTimeFormat dtf : values()) {
				if(name.toLowerCase().startsWith(dtf.name().toLowerCase())) return dtf;
			}
			return null;
		}
	}

	/**
	 * The DateTimeFormat used by the formatted. It can be modified by the developer
	 */
	private static DateTimeFormat localeFormat = DateTimeFormat.SWISS;


	//	private static final DateFormat dateFormatFull = new SimpleDateFormat("dd-MMM-yyyy");
	private static DateFormat yyyymmddFormat = new SimpleDateFormat("yyyyMMdd");
	private static DateFormat dateFormat;
	private static DateFormat dateFormatYYYY;
	private static DateFormat timeFormat;

	/**
	 * List of datetime parsers:
	 */
	private static DateFormat[] dateTimeParsers;
	/**
	 * List of datetime formatters:
	 * - with seconds
	 * - with hours/minutes
	 * - with days
	 */
	private static DateFormat[] dateTimeFormatters;



	private static final DecimalFormat df0 = new DecimalFormat("0");
	private static final DecimalFormat df1 = new DecimalFormat("0.0");
	private static final DecimalFormat df2 = new DecimalFormat("0.00");
	private static final DecimalFormat dfmax2 = new DecimalFormat("0.##");
	private static final DecimalFormat df3 = new DecimalFormat("0.000");
	private static final DecimalFormat dfmax3 = new DecimalFormat("0.###");
	private static final DecimalFormat df4 = new DecimalFormat("0.0000");
	private static final DecimalFormat df8 = new DecimalFormat("0.00000000");
	private static final DecimalFormat dfE = new DecimalFormat("0.00E0");

	static {
		//Set default format
		setLocaleFormat(DateTimeFormat.SWISS);

		//Decimal Format should round like Excel, i.e half up
		df0.setRoundingMode(RoundingMode.HALF_UP);
		df1.setRoundingMode(RoundingMode.HALF_UP);
		df2.setRoundingMode(RoundingMode.HALF_UP);
		dfmax2.setRoundingMode(RoundingMode.HALF_UP);
		df3.setRoundingMode(RoundingMode.HALF_UP);
		dfmax3.setRoundingMode(RoundingMode.HALF_UP);
		df4.setRoundingMode(RoundingMode.HALF_UP);
		df8.setRoundingMode(RoundingMode.HALF_UP);
		dfE.setRoundingMode(RoundingMode.HALF_UP);
	}



	public static DateTimeFormat getLocaleFormat() {
		return FormatterUtils.localeFormat;
	}

	public static void setLocaleFormat(DateTimeFormat localeFormat) {
		assert localeFormat!=null;
		FormatterUtils.localeFormat = localeFormat;
		dateFormat = new SimpleDateFormat(localeFormat.getLocaleDateFormat());
		timeFormat = new SimpleDateFormat("HH:mm");

		String format = localeFormat.getLocaleDateFormat();
		String alternateFormat = localeFormat.getLocaleDateFormat().indexOf('/')>=0? localeFormat.getLocaleDateFormat().replace("/", "."): localeFormat.getLocaleDateFormat().replace(".", "/");
		if(localeFormat==DateTimeFormat.YYYYMMDD) {
			dateFormatYYYY = new SimpleDateFormat("yyyy-MM-dd");
			dateTimeParsers = new SimpleDateFormat[] {
					new SimpleDateFormat("yy-MM-dd HH:mm:ss"),
					new SimpleDateFormat("yy-MM-dd HH:mm"),
					new SimpleDateFormat("yy-MM-dd")};
			dateTimeFormatters = new SimpleDateFormat[] {
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
					new SimpleDateFormat("yyyy-MM-dd HH:mm"),
					new SimpleDateFormat("yyyy-MM-dd")};
		} else if(localeFormat==DateTimeFormat.DDMMMYYYY) {
			dateFormatYYYY = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
			dateTimeParsers = new SimpleDateFormat[] {
					new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US),
					new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US),
					new SimpleDateFormat("dd-MMM-yyyy", Locale.US)};
			dateTimeFormatters = new SimpleDateFormat[] {
					new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US),
					new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US),
					new SimpleDateFormat("dd-MMM-yyyy", Locale.US)};
		} else {
			assert localeFormat==DateTimeFormat.AMERICAN || localeFormat==DateTimeFormat.SWISS || localeFormat==DateTimeFormat.EUROPEAN;
			dateFormatYYYY = new SimpleDateFormat(format+"yy");
			dateTimeParsers = new SimpleDateFormat[] {
					new SimpleDateFormat(format + " " + "HH:mm" + ":ss"),
					new SimpleDateFormat(format + " " + "HH:mm"),
					new SimpleDateFormat(format),
					new SimpleDateFormat(alternateFormat + " " + "HH:mm" + ":ss"),
					new SimpleDateFormat(alternateFormat + " " + "HH:mm"),
					new SimpleDateFormat(alternateFormat)};
			dateTimeFormatters = new SimpleDateFormat[] {
					new SimpleDateFormat(format + "yy " + "HH:mm" + ":ss"),
					new SimpleDateFormat(format + "yy " + "HH:mm"),
					new SimpleDateFormat(format + "yy")};
		}

		for (DateFormat df : dateTimeParsers) {
			df.setLenient(false);
		}
	}

	public static final String format0(Double d) {
		if(d==null) return "";
		return df0.format(d);
	}

	public static final String format1(Double d) {
		if(d==null) return "";
		return df1.format(d);
	}

	public static final String format2(Double d) {
		if(d==null) return "";
		return df2.format(d);
	}

	public static final String formatMax2(Double d) {
		if(d==null) return "";
		return dfmax2.format(d);
	}

	public static final String format3(Double d) {
		if(d==null) return "";
		return df3.format(d);
	}

	public static final String formatMax3(Double d) {
		if(d==null) return "";
		return dfmax3.format(d);
	}

	public static final String format4(Double d) {
		if(d==null) return "";
		return df4.format(d);
	}

	public static final String format8(Double d) {
		if(d==null) return "";
		return df8.format(d);
	}

	public static final String formatE(Double d) {
		if(d==null) return "";
		return dfE.format(d);
	}

	public static final String format(Object value) {
		if(value==null) {
			return "";
		} else if(value instanceof Double) {
			return format3((Double) value);
		} else if(value instanceof Date) {
			return formatDateTime((Date) value);
		} else {
			return "" + value;
		}
	}




	public static final String formatDate(Date d) {
		synchronized(FormatterUtils.dateFormat) {
			return d == null ? "" : FormatterUtils.dateFormat.format(d);
		}
	}

	public static final String formatDateFull(Date d) {
		synchronized(FormatterUtils.dateFormatYYYY) {
			return d == null ? "" : FormatterUtils.dateFormatYYYY.format(d);
		}
	}

	public static final String formatDateYyyy(Date d) {
		synchronized(FormatterUtils.dateFormatYYYY) {
			return d == null ? "" : FormatterUtils.dateFormatYYYY.format(d);
		}
	}

	public static final String formatTime(Date d) {
		synchronized(FormatterUtils.timeFormat) {
			return d == null ? "" : FormatterUtils.timeFormat.format(d);
		}
	}

	public static final String formatDateTime(Date d) {
		if(d==null) return "";
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		if(cal.get(Calendar.SECOND)!=0) return dateTimeFormatters[0].format(d);
		if(cal.get(Calendar.HOUR_OF_DAY)!=0 || cal.get(Calendar.MINUTE)!=0) return dateTimeFormatters[1].format(d);
		return dateTimeFormatters[2].format(d);
	}

	public static final String formatDateOrTime(Date d) {
		if(d==null) return "";
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);

		synchronized(FormatterUtils.dateFormat) {
			String s = FormatterUtils.dateFormat.format(d);
			if(s.equals(FormatterUtils.dateFormat.format(new Date()))) {
				return FormatterUtils.timeFormat.format(d);
			} else {
				return s;
			}
		}
	}

	public static final String formatDateTimeShort(Date d) {
		if(d==null) return "";
		return dateTimeParsers[1].format(d);
	}

	public static final Date parseDateTime(String s) {
		if(s==null || s.length()==0) return null;

		for (DateFormat dateFormat : dateTimeParsers) {
			try {
				Date res = dateFormat.parse(s);
				return res;
			} catch (Exception e) {
			}
		}
		return null;
	}


	/**
	 * Cleans the date in the proper localeFormat
	 * If the date is not valid returns null
	 * @param s
	 * @return
	 */
	public static final String cleanDate(String s) {
		if(s==null || s.length()==0) return "";

		for (int i = 0; i < s.length(); i++) {
			if(Character.isDigit(s.charAt(i))) continue; //OK
			if(localeFormat==DateTimeFormat.YYYYMMDD) {
				if(s.charAt(i)=='-') continue; //OK
			} else if(localeFormat==DateTimeFormat.DDMMMYYYY) {
				if(Character.isLetter(s.charAt(i)) || s.charAt(i)=='-') continue; //OK
			} else if(localeFormat!=DateTimeFormat.YYYYMMDD) {
				if(s.charAt(i)=='.' || s.charAt(i)=='/') continue; //OK
			}
			return null;
		}

		try {
			Date date = parseDateTime(s);
			if(date==null) return null;
			return formatDateYyyy(date);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static final String cleanDateTime(String s) {
		if(s==null || s.length()==0) return s;
		Date d = parseDateTime(s);
		if(d==null) return null;
		return formatDateTime(d);
	}

	public static final String formatYYYYMMDD(Date date) {
		synchronized(FormatterUtils.yyyymmddFormat) {
			return FormatterUtils.yyyymmddFormat.format(date);
		}
	}

	public static final String formatYYYYMMDD() {
		synchronized(FormatterUtils.yyyymmddFormat) {
			return FormatterUtils.yyyymmddFormat.format(new Date());
		}
	}
	
	/**
	 * Returns the date, parsed according to the format set up in FormatterUtils
	 * @return Date object or null
	 */
	public static Date getDateFromString(String date) {
		Date parsed = FormatterUtils.parseDateTime(date);
		if(parsed==null && date.length()>0) return null; //throw new Exception("The date is not well formatted");
		return parsed;
	}

	public static void main(String[] args) {
		System.out.println(parseDateTime("10.10.2013 12:23:20"));
		System.out.println(parseDateTime("10.10.2013 12:23"));
		System.out.println(parseDateTime("10.10.2013"));
		System.out.println(parseDateTime("10.2013"));
		System.out.println(parseDateTime("2013"));
		System.out.println();

		System.out.println(formatDateTime(parseDateTime("10.10.2013 12:23:20")));
		System.out.println(formatDateTime(parseDateTime("10.10.2013 12:23")));
		System.out.println(formatDateTime(parseDateTime("10.10.2013")));
		System.out.println();

		System.out.println(cleanDateTime("10.10.2013 12:23:20"));
		System.out.println(cleanDateTime("31.12.2013 23:60"));
		System.out.println(cleanDateTime("10.12.2013"));
		System.out.println(cleanDateTime("12.2013"));
		System.out.println(cleanDateTime("2013"));
		System.out.println(cleanDateTime("13"));
		System.out.println();
		System.out.println(cleanDateTime("10/10/2013"));
		System.out.println(cleanDateTime("toto"));

	}
}
