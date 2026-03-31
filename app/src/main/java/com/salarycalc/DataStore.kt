package com.salarycalc

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class DataStore(private val ctx: Context) {

    private val prefs: SharedPreferences by lazy {
        ctx.getSharedPreferences("salary_data", Context.MODE_PRIVATE)
    }

    private fun recordsKey(year: Int, month: Int) = "records_$year-$month"

    fun loadSettings(): SalarySettings {
        val json = prefs.getString("settings", null) ?: return SalarySettings()
        val obj = JSONObject(json)
        val holidays = parseDateSet(obj.optJSONArray("customHolidays"))
        val legalHolidays = parseDateSet(obj.optJSONArray("legalHolidayDates"))
        return SalarySettings(
            normalHourlyRate = obj.optDouble("normalHourlyRate", 20.0),
            weekendMultiplier = obj.optDouble("weekendMultiplier", 2.0),
            holidayMultiplier = obj.optDouble("holidayMultiplier", 2.0),
            legalHolidayMultiplier = obj.optDouble("legalHolidayMultiplier", 3.0),
            customHolidays = holidays,
            legalHolidayDates = legalHolidays
        )
    }

    fun saveSettings(settings: SalarySettings) {
        val obj = JSONObject().apply {
            put("normalHourlyRate", settings.normalHourlyRate)
            put("weekendMultiplier", settings.weekendMultiplier)
            put("holidayMultiplier", settings.holidayMultiplier)
            put("legalHolidayMultiplier", settings.legalHolidayMultiplier)
            put("customHolidays", JSONArray(settings.customHolidays.toList()))
            put("legalHolidayDates", JSONArray(settings.legalHolidayDates.toList()))
        }
        prefs.edit().putString("settings", obj.toString()).apply()
    }

    private fun parseDateSet(arr: JSONArray?): Set<String> {
        if (arr == null) return emptySet()
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    fun getRecord(date: String): SalaryRecord? {
        val parts = date.split("-")
        val key = recordsKey(parts[0].toInt(), parts[1].toInt())
        val json = prefs.getString(key, null) ?: return null
        val obj = JSONObject(json)
        if (!obj.has(date)) return null
        val r = obj.getJSONObject(date)
        return SalaryRecord(
            date = date,
            hourlyRate = r.optDouble("hourlyRate", 0.0),
            hours = r.optDouble("hours", 0.0),
            dailyRate = r.optDouble("dailyRate", 0.0),
            isLegalHoliday = r.optBoolean("isLegalHoliday", false),
            isHoliday = r.optBoolean("isHoliday", false),
            isWeekend = r.optBoolean("isWeekend", false),
            bonus = r.optDouble("bonus", 0.0),
            note = r.optString("note", "")
        )
    }

    fun saveRecord(record: SalaryRecord) {
        val parts = record.date.split("-")
        val key = recordsKey(parts[0].toInt(), parts[1].toInt())
        val json = prefs.getString(key, null) ?: "{}"
        val obj = JSONObject(json)
        obj.put(record.date, JSONObject().apply {
            put("hourlyRate", record.hourlyRate)
            put("hours", record.hours)
            put("dailyRate", record.dailyRate)
            put("isLegalHoliday", record.isLegalHoliday)
            put("isHoliday", record.isHoliday)
            put("isWeekend", record.isWeekend)
            put("bonus", record.bonus)
            put("note", record.note)
        })
        prefs.edit().putString(key, obj.toString()).apply()
    }

    fun deleteRecord(date: String) {
        val parts = date.split("-")
        val key = recordsKey(parts[0].toInt(), parts[1].toInt())
        val json = prefs.getString(key, null) ?: return
        val obj = JSONObject(json)
        obj.remove(date)
        prefs.edit().putString(key, obj.toString()).apply()
    }

    fun getRecordsForMonth(year: Int, month: Int): List<SalaryRecord> {
        val key = recordsKey(year, month)
        val json = prefs.getString(key, null) ?: return emptyList()
        val obj = JSONObject(json)
        val list = mutableListOf<SalaryRecord>()
        obj.keys().forEach { date ->
            val r = obj.getJSONObject(date)
            list.add(SalaryRecord(
                date = date,
                hourlyRate = r.optDouble("hourlyRate", 0.0),
                hours = r.optDouble("hours", 0.0),
                dailyRate = r.optDouble("dailyRate", 0.0),
                isLegalHoliday = r.optBoolean("isLegalHoliday", false),
                isHoliday = r.optBoolean("isHoliday", false),
                isWeekend = r.optBoolean("isWeekend", false),
                bonus = r.optDouble("bonus", 0.0),
                note = r.optString("note", "")
            ))
        }
        return list.sortedBy { it.date }
    }

    fun getMonthStats(year: Int, month: Int): MonthStats {
        val records = getRecordsForMonth(year, month)
        return MonthStats(
            totalSalary = records.sumOf { it.total },
            totalHours = records.sumOf { if (it.dailyRate > 0) 0.0 else it.hours }
        )
    }

    /** 普通节假日（2倍），格式 yyyy-MM-dd */
    fun isHoliday(date: String): Boolean {
        val settings = loadSettings()
        return settings.customHolidays.contains(date)
    }

    /** 法定节假日（3倍），格式 yyyy-MM-dd */
    fun isLegalHoliday(date: String): Boolean {
        val settings = loadSettings()
        return settings.legalHolidayDates.contains(date)
    }
}
