package com.salarycalc

data class SalaryRecord(
    val date: String,
    val hourlyRate: Double,       // 原始时薪（不含倍率）
    val multiplier: Double,       // 实际倍率（用户填的）
    val hours: Double,
    val dailyRate: Double,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    val bonus: Double,
    val note: String
) {
    /** 实际时薪 = 原始时薪 × 倍率 */
    val effectiveRate: Double get() = hourlyRate * multiplier
    /** 小计 = 实际时薪 × 小时数 */
    val hourlySubtotal: Double get() = effectiveRate * hours
    /** 总计 = (时薪模式?小计:日薪) + 奖金 */
    val total: Double get() = if (dailyRate > 0) dailyRate + bonus else hourlySubtotal + bonus

    companion object {
        fun fromForm(
            date: String, hourlyRate: Double, multiplier: Double, hours: Double,
            dailyRate: Double, isHoliday: Boolean, isWeekend: Boolean,
            bonus: Double, note: String
        ) = SalaryRecord(date, hourlyRate, multiplier, hours, dailyRate, isHoliday, isWeekend, bonus, note)
    }
}

data class SalarySettings(
    val normalHourlyRate: Double = 20.0,
    val normalDailyRate: Double = 0.0
)

data class MonthStats(
    val totalSalary: Double = 0.0,
    val totalHours: Double = 0.0
)
