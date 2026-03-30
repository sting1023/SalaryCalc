package com.salarycalc

import java.util.Calendar

data class SalaryRecord(
    val date: String,        // "yyyy-MM-dd"
    val hourlyRate: Double,  // >0 时薪模式
    val hours: Double,       // 工作小时数
    val dailyRate: Double,  // >0 日薪模式
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    val note: String
) {
    val total: Double
        get() = if (dailyRate > 0) dailyRate else hourlyRate * hours

    companion object {
        fun fromForm(
            date: String, hourlyRate: Double, hours: Double,
            dailyRate: Double, isHoliday: Boolean, isWeekend: Boolean, note: String
        ) = SalaryRecord(date, hourlyRate, hours, dailyRate, isHoliday, isWeekend, note)
    }
}

data class SalarySettings(
    val normalHourlyRate: Double = 20.0,
    val overtimeDailyRate: Double = 0.0,
    val holidayHourlyRate: Double = 50.0,
    val customHolidays: Set<String> = emptySet()
)

data class MonthStats(
    val totalSalary: Double = 0.0,
    val totalHours: Double = 0.0
)
