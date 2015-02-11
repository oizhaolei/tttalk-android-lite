package com.ruptech.tttalk_android.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateCommonUtils {


	public static String DF_yyyyMM = "yyyy-MM";
	public static String DF_yyyyMMddHHmmss = "yyyy-MM-dd HH:mm:ss";
	public static String DF_yyyyMMddHHmmssSSS = "yyyy-MM-dd HH:mm:ss.SSS";
	public static String DF_yyyyMMddHHmmss2 = "yyyyMMddHHmmss";
	public static String DF_yyyyMMddHHmmssSSS2 = "yyyyMMddHHmmssSSS";
	public final static String DF_yyyyMMdd = "yyyy-MM-dd";
	public static String DF_MMddHHmm = "MM-dd HH:mm";
	public static String DF_HHmm = "HH:mm";

	public static Locale defaultLocale = Locale.getDefault();

	public static final long DAY_SPAN_SIZE = 24 * 60 * 60 * 1000;

	public static final long CHAT_TIME_SPAN_SIZE = 60 * 60 * 1000;



	public static String convUtcDateString(Date date, String pattern) {
		try {
			long t = date.getTime() + TimeZone.getDefault().getRawOffset();
			SimpleDateFormat df = new SimpleDateFormat(pattern,
					defaultLocale);
			return df.format(t);
		} catch (Exception e) {
			return "";
		}
	}

	public static String dateFormat(Date dd, String pattern) {
		if (dd == null)
			return "";
		try {
			SimpleDateFormat df = new SimpleDateFormat(pattern, defaultLocale);
			return df.format(dd);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 计算两个日期之间相差的天数
	 * 
	 * @param smdate
	 *            较小的时间
	 * @param bdate
	 *            较大的时间
	 * @return 相差天数
	 * @throws ParseException
	 */
	private static int daysBetween(Date smdate, Date bdate)
			throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat(DF_yyyyMMdd, defaultLocale);
		
		String smdateStr = DateCommonUtils.dateFormat(smdate, DF_yyyyMMdd);
		smdate = df.parse(smdateStr);
		String bdateStr = DateCommonUtils.dateFormat(bdate, DF_yyyyMMdd);
		bdate = df.parse(bdateStr);
		Calendar cal = Calendar.getInstance();
		cal.setTime(smdate);
		long time1 = cal.getTimeInMillis();
		cal.setTime(bdate);
		long time2 = cal.getTimeInMillis();
		long between_days = (time2 - time1) / (1000 * 3600 * 24);

		return Integer.parseInt(String.valueOf(between_days));
	}



	// 跨时区时间转换成分钟
	public static int getTimezoneMinuteOffset() {
		int minute = TimeZone.getDefault().getRawOffset() / (1000 * 60);
		return minute;
	}

	public static String getUtcDate(Date date, String pattern) {
		try {
			long t = date.getTime() - TimeZone.getDefault().getRawOffset();
			SimpleDateFormat df = new SimpleDateFormat(pattern, defaultLocale);
			return df.format(t);
		} catch (Exception e) {
			return "";
		}
	}

}
