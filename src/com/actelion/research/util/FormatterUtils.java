/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
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

/**
 * Formatter Utilidy class to format numbers and date/time 
 * @author freyssj
 */
public class FormatterUtils {

	public static enum LocaleFormat {
		SWISS("dd.MM.yy", "HH:mm"),
		EUROPEAN("dd/MM/yy", "HH:mm"),
		AMERICAN("dd/MM/yy", "HH:mm");
		private String localeDateFormat;
		private String localeTimeFormat;
		private LocaleFormat(String localeDateFormat, String localeTimeFormat) {
			this.localeDateFormat = localeDateFormat;
			this.localeTimeFormat = localeTimeFormat;
		}
		public String getLocaleDateFormat() {
			return localeDateFormat;
		}
		public String getLocaleTimeFormat() {
			return localeTimeFormat;
		}
		@Override
		public String toString() {
			return name() + " (" + localeDateFormat+" "+localeTimeFormat+")";
		}
		public static LocaleFormat get(String name) {
			for (LocaleFormat lf : values()) {
				if(name.toLowerCase().startsWith(lf.name().toLowerCase())) return lf;
			}
			return null;
		}
	}
	
	private LocaleFormat localeFormat = LocaleFormat.SWISS;
	
	private static DateFormat yyyyFormat = new SimpleDateFormat("yyyyMMdd");

	private static DateFormat dateFormat;
	private static DateFormat dateFormatYYYY;
	private static DateFormat timeFormat;
	public static final DateFormat dateFormatFull = new SimpleDateFormat("MMM dd, yyyy");

	private static DateFormat[] dateTimeParsers;
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
		setLocaleFormat(LocaleFormat.SWISS);

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
	
	

	public LocaleFormat getLocaleFormat() {
		return localeFormat;
	}
	
	public static void setLocaleFormat(LocaleFormat localeFormat) {
		assert localeFormat!=null;
		dateFormat = new SimpleDateFormat(localeFormat.getLocaleDateFormat());
		dateFormatYYYY = new SimpleDateFormat(localeFormat.getLocaleDateFormat()+"yy");
		timeFormat = new SimpleDateFormat(localeFormat.getLocaleTimeFormat());

		dateTimeParsers = new SimpleDateFormat[] {		
			new SimpleDateFormat(localeFormat.getLocaleDateFormat() + " " + localeFormat.getLocaleTimeFormat() + ":ss"),
			new SimpleDateFormat(localeFormat.getLocaleDateFormat() + " " + localeFormat.getLocaleTimeFormat()),
			new SimpleDateFormat(localeFormat.getLocaleDateFormat()),
			new SimpleDateFormat("MM.yy"),
			new SimpleDateFormat("yy")};
		dateTimeFormatters = new SimpleDateFormat[] {		
			new SimpleDateFormat(localeFormat.getLocaleDateFormat() + "yy " + localeFormat.getLocaleTimeFormat() + ":ss"),
			new SimpleDateFormat(localeFormat.getLocaleDateFormat() + "yy " + localeFormat.getLocaleTimeFormat()),
			new SimpleDateFormat(localeFormat.getLocaleDateFormat() + "yy"),
			new SimpleDateFormat("MM.yyyy"),
			new SimpleDateFormat("yyyy")};
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
		return d == null ? "" : dateFormat.format(d);
	}

	public static final String formatDateFull(Date d) {
		return d == null ? "" : dateFormatFull.format(d);
	}

	public static final String formatDateYyyy(Date d) {
		return d == null ? "" : dateFormatYYYY.format(d);
	}

	public static final String formatTime(Date d) {
		return d == null ? "" : timeFormat.format(d);
	}

	public static final String formatDateTime(Date d) {
		if(d==null) return "";
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		if(cal.get(Calendar.SECOND)!=0) return dateTimeFormatters[0].format(d);
		if(cal.get(Calendar.HOUR_OF_DAY)!=0 || cal.get(Calendar.MINUTE)!=0) return dateTimeFormatters[1].format(d);
		if(cal.get(Calendar.DAY_OF_MONTH)!=1) return dateTimeFormatters[2].format(d);
		if(cal.get(Calendar.MONTH)!=0) return dateTimeFormatters[3].format(d);
		return dateTimeFormatters[4].format(d);
	}
	
	public static final String formatDateOrTime(Date d) {
		if(d==null) return "";
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);

		String s = dateFormat.format(d);
		if(s.equals(dateFormat.format(new Date()))) {
			return timeFormat.format(d);
		} else {
			return s;
		}
	}
	
	public static final String formatDateTimeShort(Date d) {
		if(d==null) return "";
		return dateTimeParsers[1].format(d);
	}

	public static final Date parseDate(String s) {
		if(s==null || s.length()==0) return null;
		
		s = s.replace('/', '.');
		for (DateFormat dateFormat : dateTimeParsers) {
			try {			
				return dateFormat.parse(s);
			} catch (Exception e) {
			}			
		}
		return null;
	}
	
	public static final Date parseDateTime(String s) {
		if(s==null || s.length()==0) return null;
		
		s = s.replace('/', '.');
		for (DateFormat dateFormat : dateTimeParsers) {
			try {			
				return dateFormat.parse(s);
			} catch (Exception e) {
			}			
		}
		return null;
	}

	public static final String cleanDate(String s) {
		if(s==null || s.length()==0) return s;
		try {
			return formatDate(parseDate(s));
		} catch (Exception e) {
			return null;
		}
			
	}
	
	public static final String cleanDateTime(String s) {
		if(s==null || s.length()==0) return s;
		Date d = parseDateTime(s);
		if(d==null) return "";
		return formatDateTime(d);
	}
	
	public static final String formatYYYYMMDD() {		
		return yyyyFormat.format(new Date());
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
