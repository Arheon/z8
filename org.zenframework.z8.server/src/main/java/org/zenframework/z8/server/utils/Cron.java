package org.zenframework.z8.server.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import org.zenframework.z8.server.types.bool;
import org.zenframework.z8.server.types.date;
import org.zenframework.z8.server.types.string;

public class Cron {

	private static final String COLUMN = "((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?(\\,((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?)*";
	private static final Pattern PATTERN = Pattern
			.compile("^" + COLUMN + "\\s+" + COLUMN + "\\s+" + COLUMN + "\\s+" + COLUMN + "\\s+" + COLUMN + "$");

	private static final int MIN = 0;
	private static final int HOUR = 1;
	private static final int DAYOFM = 2;
	private static final int MONTH = 3;
	private static final int DAYOFW = 4;

	private Cron() {}

	public static date nextDate(date date, String cronExp) {
		Rule[] rules = parse(cronExp);
		date dt = addMinute(date);
		for (boolean monthOk = rules[MONTH].check(dt.month(), false), dayOk = rules[DAYOFM].check(dt.day(), true)
				|| rules[DAYOFW].check(dayOfWeekMondayFirst(dt), true)
				|| rules[DAYOFM].any() && rules[DAYOFW].any(), hourOk = rules[HOUR].check(dt.hours(),
						false), minOk = rules[MIN].check(dt.minutes(), false); !monthOk || !dayOk
								|| !hourOk
								|| !minOk; monthOk = rules[MONTH].check(dt.month(), false), dayOk = rules[DAYOFM]
										.check(dt.day(), true) || rules[DAYOFW].check(dayOfWeekMondayFirst(dt), true)
										|| rules[DAYOFM].any() && rules[DAYOFW].any(), hourOk = rules[HOUR].check(
												dt.hours(), false), minOk = rules[MIN].check(dt.minutes(), false)) {
			if (!monthOk)
				dt = addMonth(dt);
			else if (!dayOk)
				dt = addDay(dt);
			else if (!hourOk)
				dt = addHour(dt);
			else
				dt = addMinute(dt);
			if (dt.operatorSub(date).days() > 366)
				throw new IllegalArgumentException("Can't find date for '" + cronExp + "' after " + date);
		}
		return dt;
	}

	public static boolean checkExp(String cronExp) {
		return PATTERN.matcher(cronExp).matches();
	}

	public static date z8_nextDate(date date, string cronExp) {
		return nextDate(date, cronExp.get());
	}

	public static bool z8_checkExp(string cronExp) {
		return new bool(checkExp(cronExp.get()));
	}

	private static Rule[] parse(String cronExp) {
		if (!checkExp(cronExp))
			throw new IllegalArgumentException("Illegal CRON expression '" + cronExp + "'");
		String[] parts = cronExp.trim().split("\\s+");
		Rule[] rules = new Rule[parts.length];
		for (int i = 0; i < rules.length; i++)
			rules[i] = new Rule(parts[i]);
		return rules;
	}

	private static date addMinute(date dt) {
		dt = dt.addMinute(1);
		dt.set(dt.year(), dt.month(), dt.day(), dt.hours(), dt.minutes(), 0, 0);
		return dt;
	}

	private static date addHour(date dt) {
		dt = dt.addHour(1);
		dt.set(dt.year(), dt.month(), dt.day(), dt.hours(), 0, 0, 0);
		return dt;
	}

	private static date addDay(date dt) {
		dt = dt.addDay(1);
		dt.set(dt.year(), dt.month(), dt.day(), 0, 0, 0, 0);
		return dt;
	}

	private static date addMonth(date dt) {
		dt = dt.addMonth(1);
		dt.set(dt.year(), dt.month(), 1, 0, 0, 0, 0);
		return dt;
	}

	private static int dayOfWeekMondayFirst(date dt) {
		return (dt.dayOfWeek() + 5) % 7 + 1;
	}

	private static class Rule {

		// { min, max, mult }[]
		final int[][] rules;

		Rule(String rule) {
			if (rule.equals("*")) {
				rules = null;
			} else {
				String[] parts = rule.split("\\,");
				rules = new int[parts.length][3];
				for (int i = 0; i < rules.length; i++) {
					String vals[] = parts[i].split("[-/]");
					if (vals[0].equals("*")) {
						rules[i][0] = 0;
						rules[i][1] = 100;
						rules[i][2] = vals.length > 1 ? Integer.parseInt(vals[1]) : 1;
					} else {
						rules[i][0] = Integer.parseInt(vals[0]);
						rules[i][1] = vals.length > 1 ? Integer.parseInt(vals[1]) : rules[i][0];
						rules[i][2] = vals.length > 2 ? Integer.parseInt(vals[2]) : 1;
					}
				}
			}
		}

		boolean any() {
			return rules == null;
		}

		boolean check(int value, boolean strict) {
			if (any())
				return !strict;
			for (int[] rule : rules)
				if (rule[0] <= value && value <= rule[1] && (value - rule[0]) % rule[2] == 0)
					return true;
			return false;
		}

	}

	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd(u) HH:mm");

	private static void check(String base, String cronExp, String... expectedArr) {
		date baseDate = new date(base);
		for (String expected : expectedArr) {
			date actualDate = nextDate(baseDate, cronExp);
			date expectedDate = new date(expected);
			System.out.println(baseDate.format(FORMAT) + " - " + cronExp + " - expected: " + expectedDate.format(FORMAT)
					+ ", actual: " + actualDate.format(FORMAT) + " - "
					+ (expectedDate.equals(actualDate) ? "OK" : "ERROR"));
			baseDate = actualDate;
		}
	}
	
	private static void checkIllegalExp(String base, String cronExp) {
		date baseDate = new date(base);
		try {
			nextDate(baseDate, cronExp);
			System.out.println(baseDate.format(FORMAT) + " - " + cronExp + " - check illegal ERROR: IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			System.out.println(baseDate.format(FORMAT) + " - " + cronExp + " - check illegal OK");
		}
	}

	// Tests
	public static void main(String[] args) {
		check("2000-01-01 00:00", "* * * * *", "2000-01-01 00:01", "2000-01-01 00:02");
		check("2000-01-01 00:05", "5 0 * * *", "2000-01-02 00:05", "2000-01-03 00:05");
		check("2000-01-01 00:00", "15 14 1 * *", "2000-01-01 14:15", "2000-02-01 14:15");
		check("2000-01-01 00:00", "0 22 * * 1-5", "2000-01-03 22:00", "2000-01-04 22:00");
		check("2000-01-01 00:00", "23 */2 * * *", "2000-01-01 00:23", "2000-01-01 02:23");
		check("2000-01-01 00:00", "5 4 * * 7", "2000-01-02 04:05", "2000-01-09 04:05");
		check("2000-01-01 00:00", "0 0 1 1 *", "2001-01-01 00:00", "2002-01-01 00:00");
		check("2000-01-01 00:00", "15 10,13 * * 1,4", "2000-01-03 10:15", "2000-01-03 13:15", "2000-01-06 10:15", "2000-01-06 13:15");
		check("2000-01-01 00:00", "0-59 * * * *", "2000-01-01 00:01", "2000-01-01 00:02");
		check("2000-01-01 00:00", "0-59/2 * * * *", "2000-01-01 00:02", "2000-01-01 00:04");
		check("2000-01-01 00:00", "1-59/2 * * * *", "2000-01-01 00:01", "2000-01-01 00:03");
		check("2000-01-01 00:00", "*/5 * * * *", "2000-01-01 00:05", "2000-01-01 00:10");
		check("2000-01-01 00:00", "* * 29 2 *");
		checkIllegalExp("2000-01-01 00:00", "* * 31 2 *");
		checkIllegalExp("2000-01-01 00:00", "3/2 * * * *");
	}

}
