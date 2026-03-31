package com.salarycalc

data class SalaryRecord(
    val date: String,
    val hourlyRate: Double,  // 用户填写的实际时薪（已乘完倍率）
    val hours: Double,
    val dailyRate: Double,
    val isLegalHoliday: Boolean,   // 法定节假日（春节/国庆等，3倍）
    val isHoliday: Boolean,         // 普通节假日（自定义，2倍）
    val isWeekend: Boolean,
    val bonus: Double,
    val note: String
) {
    val total: Double
        get() = if (dailyRate > 0) dailyRate + bonus else hourlyRate * hours + bonus

    companion object {
        fun fromForm(
            date: String, hourlyRate: Double, hours: Double,
            dailyRate: Double, isLegalHoliday: Boolean, isHoliday: Boolean,
            isWeekend: Boolean, bonus: Double, note: String
        ) = SalaryRecord(date, hourlyRate, hours, dailyRate, isLegalHoliday, isHoliday, isWeekend, bonus, note)
    }
}

data class SalarySettings(
    val normalHourlyRate: Double = 20.0,
    val weekendMultiplier: Double = 2.0,          // 周末倍率，默认2倍
    val holidayMultiplier: Double = 2.0,         // 普通节假日倍率，默认2倍
    val legalHolidayMultiplier: Double = 3.0,    // 法定节假日倍率，默认3倍
    val customHolidays: Set<String> = emptySet(),      // 普通节假日（2倍）
    val legalHolidayDates: Set<String> = emptySet()       // 法定节假日（3倍），格式 yyyy-MM-dd
)

data class MonthStats(
    val totalSalary: Double = 0.0,
    val totalHours: Double = 0.0
)
