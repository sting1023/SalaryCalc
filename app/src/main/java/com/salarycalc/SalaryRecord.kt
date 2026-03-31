package com.salarycalc

data class SalaryRecord(
    val date: String,        // "yyyy-MM-dd"
    val hourlyRate: Double,  // >0 时薪模式
    val hours: Double,       // 工作小时数
    val dailyRate: Double,   // >0 日薪模式
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    val bonus: Double,       // 额外/奖金收入
    val note: String
) {
    val total: Double
        get() = if (dailyRate > 0) dailyRate + bonus else hourlyRate * hours + bonus

    companion object {
        fun fromForm(
            date: String, hourlyRate: Double, hours: Double,
            dailyRate: Double, isHoliday: Boolean, isWeekend: Boolean,
            bonus: Double, note: String
        ) = SalaryRecord(date, hourlyRate, hours, dailyRate, isHoliday, isWeekend, bonus, note)
    }
}

data class SalarySettings(
    val normalHourlyRate: Double = 20.0,
    val holidayHourlyRate: Double = 50.0,
    val weekendHourlyRate: Double = 30.0,
    val customHolidays: Set<String> = emptySet()
)

data class MonthStats(
    val totalSalary: Double = 0.0,
    val totalHours: Double = 0.0
)
