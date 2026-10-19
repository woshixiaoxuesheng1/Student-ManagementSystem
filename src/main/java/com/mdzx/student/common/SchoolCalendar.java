package com.mdzx.student.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 教学日口径：周一至周五为教学日（与前端一致） */
public final class SchoolCalendar {

    /** 教学日工具类不需要实例。 */
    private SchoolCalendar() {
    }

    /** 判断日期是否为教学日（周一至周五）。 */
    /** 判断日期是否为教学日（周一至周五）。 */
    public static boolean isSchoolDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /** 从给定日期向前找到最近的教学日 */
    public static LocalDate latestSchoolDay(LocalDate from) {
        LocalDate date = from;
        for (int i = 0; i < 7 && !isSchoolDay(date); i++) {
            date = date.minusDays(1);
        }
        return date;
    }

    /** 最近 count 个教学日，按时间升序返回 */
    public static List<LocalDate> lastSchoolDays(LocalDate endInclusive, int count) {
        List<LocalDate> days = new ArrayList<>(count);
        LocalDate date = endInclusive;
        while (days.size() < count) {
            if (isSchoolDay(date)) {
                days.add(date);
            }
            date = date.minusDays(1);
        }
        Collections.reverse(days);
        return days;
    }
}
