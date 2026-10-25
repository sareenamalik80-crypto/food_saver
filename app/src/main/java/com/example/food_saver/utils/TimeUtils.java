package com.example.food_saver.utils;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtils {

    private static final Pattern WINDOW = Pattern.compile(
            "(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?\\s*(?:-|–|to)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?");

    private TimeUtils() {}

    public static int[] parseWindow(String text) {
        if (text == null) return null;
        Matcher m = WINDOW.matcher(text.toLowerCase(Locale.US));
        if (!m.find()) return null;
        String startSuffix = m.group(3);
        String endSuffix = m.group(6);
        if (startSuffix == null) startSuffix = endSuffix;
        int start = toMinutes(m.group(1), m.group(2), startSuffix);
        int end = toMinutes(m.group(4), m.group(5), endSuffix);
        if (start < 0 || end < 0) return null;
        return new int[]{start, end};
    }

    private static int toMinutes(String hourText, String minuteText, String suffix) {
        int hour = Integer.parseInt(hourText);
        int minute = minuteText != null ? Integer.parseInt(minuteText) : 0;
        if (minute > 59) return -1;
        if (suffix != null) {
            if (hour < 1 || hour > 12) return -1;
            boolean pm = suffix.startsWith("p");
            if (hour == 12) hour = 0;
            if (pm) hour += 12;
        } else if (hour > 23) {
            return -1;
        }
        return hour * 60 + minute;
    }

    public static boolean isInWindow(int minuteOfDay, int[] window) {
        int start = window[0];
        int end = window[1];
        if (start <= end) return minuteOfDay >= start && minuteOfDay <= end;
        return minuteOfDay >= start || minuteOfDay <= end;
    }

    public static String format(int minuteOfDay) {
        int hour = minuteOfDay / 60;
        int minute = minuteOfDay % 60;
        String suffix = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12 == 0 ? 12 : hour % 12;
        return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, suffix);
    }

    public static String formatWindow(int start, int end) {
        return format(start) + " - " + format(end);
    }

    public static int nowMinuteOfDay() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    public static long todayAt(int minuteOfDay) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        c.set(Calendar.MINUTE, minuteOfDay % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static boolean hasArrivalTimePassed(long arrivalAt) {
        return arrivalAt <= 0 || System.currentTimeMillis() >= arrivalAt;
    }
}
