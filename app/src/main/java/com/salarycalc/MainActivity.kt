package com.salarycalc

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getColor
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var ds: DataStore
    private lateinit var settings: SalarySettings

    private lateinit var tvYearMonth: TextView
    private lateinit var tvMonthTotal: TextView
    private lateinit var tvMonthHours: TextView
    private lateinit var calendarTable: TableLayout
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvDayTotal: TextView
    private lateinit var tvDayStatus: TextView
    private lateinit var etHourlyRate: EditText
    private lateinit var etHours: EditText
    private lateinit var etDailyRate: EditText
    private lateinit var etBonus: EditText
    private lateinit var llHourly: LinearLayout
    private lateinit var llDaily: LinearLayout
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var btnModeHourly: Button
    private lateinit var btnModeDaily: Button
    private lateinit var lvRecords: ListView

    private var currentYear = 0
    private var currentMonth = 0
    private var selectedDate = ""
    private var isHoliday = false
    private var isWeekend = false
    private var useDailyMode = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.CHINA)
    private val dayDisplayFormat = SimpleDateFormat("M月d日 E", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ds = DataStore(this)

        val now = Calendar.getInstance()
        currentYear = now.get(Calendar.YEAR)
        currentMonth = now.get(Calendar.MONTH) + 1
        selectedDate = dateFormat.format(now.time)

        initViews()
        updateAll()
    }

    private fun initViews() {
        tvYearMonth    = findViewById(R.id.tv_year_month)
        tvMonthTotal   = findViewById(R.id.tv_month_total)
        tvMonthHours   = findViewById(R.id.tv_month_hours)
        calendarTable  = findViewById(R.id.calendar_table)
        tvSelectedDate = findViewById(R.id.tv_selected_date)
        tvDayTotal     = findViewById(R.id.tv_day_total)
        tvDayStatus    = findViewById(R.id.tv_day_status)
        etHourlyRate   = findViewById(R.id.et_hourly_rate)
        etHours        = findViewById(R.id.et_hours)
        etDailyRate    = findViewById(R.id.et_daily_rate)
        etBonus        = findViewById(R.id.et_bonus)
        llHourly       = findViewById(R.id.ll_hourly)
        llDaily        = findViewById(R.id.ll_daily)
        btnSave        = findViewById(R.id.btn_save)
        btnDelete      = findViewById(R.id.btn_delete)
        btnPrevMonth   = findViewById(R.id.btn_prev_month)
        btnNextMonth   = findViewById(R.id.btn_next_month)
        btnModeHourly  = findViewById(R.id.btn_mode_hourly)
        btnModeDaily   = findViewById(R.id.btn_mode_daily)
        lvRecords      = findViewById(R.id.lv_records)

        btnPrevMonth.setOnClickListener {
            if (--currentMonth == 0) { currentMonth = 12; currentYear-- }
            updateAll()
        }
        btnNextMonth.setOnClickListener {
            if (++currentMonth == 13) { currentMonth = 1; currentYear++ }
            updateAll()
        }
        btnModeHourly.setOnClickListener { useDailyMode = false; applyModeUI() }
        btnModeDaily.setOnClickListener { useDailyMode = true; applyModeUI() }
        btnSave.setOnClickListener { saveRecord() }
        btnDelete.setOnClickListener { deleteRecord() }
        lvRecords.setOnItemClickListener { _, _, pos, _ ->
            val records = ds.getRecordsForMonth(currentYear, currentMonth)
            if (pos < records.size) { selectedDate = records[pos].date; updateAll() }
        }
        findViewById<Button>(R.id.btn_settings).setOnClickListener { showSettings() }
    }

    private fun updateAll() {
        settings = ds.loadSettings()

        val cal = Calendar.getInstance().apply { set(currentYear, currentMonth - 1, 1) }
        tvYearMonth.text = monthFormat.format(cal.time)

        val stats = ds.getMonthStats(currentYear, currentMonth)
        tvMonthTotal.text = "¥${String.format("%.2f", stats.totalSalary)}"
        tvMonthHours.text = "${String.format("%.1f", stats.totalHours)} 小时"

        val selCal = Calendar.getInstance()
        val parts = selectedDate.split("-")
        selCal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        tvSelectedDate.text = dayDisplayFormat.format(selCal.time)

        // 自动判断节假日/周末
        isHoliday = ds.isHoliday(selectedDate)
        val dow = selCal.get(Calendar.DAY_OF_WEEK)
        isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY

        val record = ds.getRecord(selectedDate)
        if (record != null) {
            // 有记录，读取保存的值
            useDailyMode = record.dailyRate > 0
            tvDayTotal.text = "¥${String.format("%.2f", record.total)}"
            tvDayStatus.text = typeLabel(record.isHoliday, record.isWeekend)
            etHourlyRate.setText(String.format("%.1f", record.hourlyRate))
            etHours.setText(String.format("%.1f", record.hours))
            etDailyRate.setText(if (record.dailyRate > 0) String.format("%.1f", record.dailyRate) else "")
            etBonus.setText(if (record.bonus > 0) String.format("%.1f", record.bonus) else "")
            btnDelete.visibility = View.VISIBLE
        } else {
            // 无记录 → 自动套用预设时薪
            tvDayTotal.text = "¥0.00"
            tvDayStatus.text = typeLabel(isHoliday, isWeekend)
            val autoRate = when {
                isHoliday -> settings.holidayHourlyRate
                isWeekend  -> settings.weekendHourlyRate
                else       -> settings.normalHourlyRate
            }
            etHourlyRate.setText(String.format("%.1f", autoRate))
            etHours.setText("")
            etDailyRate.setText("")
            etBonus.setText("")
            btnDelete.visibility = View.GONE
        }

        applyModeUI()
        buildCalendar()
        lvRecords.adapter = RecordAdapter(ds.getRecordsForMonth(currentYear, currentMonth))
    }

    private fun typeLabel(holiday: Boolean, weekend: Boolean) = when {
        holiday -> "节假日"
        weekend -> "周末"
        else -> "工作日"
    }

    private fun applyModeUI() {
        if (useDailyMode) {
            llHourly.visibility = View.GONE
            llDaily.visibility = View.VISIBLE
            btnModeHourly.setBackgroundColor(Color.parseColor("#E0E0E0"))
            btnModeHourly.setTextColor(Color.parseColor("#888888"))
            btnModeDaily.setBackgroundColor(getColor(this, R.color.primary))
            btnModeDaily.setTextColor(Color.WHITE)
        } else {
            llHourly.visibility = View.VISIBLE
            llDaily.visibility = View.GONE
            btnModeHourly.setBackgroundColor(getColor(this, R.color.primary))
            btnModeHourly.setTextColor(Color.WHITE)
            btnModeDaily.setBackgroundColor(Color.parseColor("#E0E0E0"))
            btnModeDaily.setTextColor(Color.parseColor("#888888"))
        }
    }

    private fun buildCalendar() {
        calendarTable.removeAllViews()

        val cal = Calendar.getInstance().apply { set(currentYear, currentMonth - 1, 1) }
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)
        val startOffset = if (firstDow == Calendar.SUNDAY) 6 else firstDow - 2
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = dateFormat.format(Date())
        val todayCal = Calendar.getInstance()
        val isCurrentMonth = currentYear == todayCal.get(Calendar.YEAR) &&
                             currentMonth == todayCal.get(Calendar.MONTH) + 1

        val recordMap = ds.getRecordsForMonth(currentYear, currentMonth).associateBy { it.date }

        for (row in 0 until 6) {
            val tableRow = TableRow(this)
            tableRow.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.day_cell_height)
            )
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val day = cellIndex - startOffset + 1

                if (day < 1 || day > daysInMonth) {
                    val spacer = Space(this)
                    val lp = TableRow.LayoutParams(0,
                        resources.getDimensionPixelSize(R.dimen.day_cell_height), 1f)
                    spacer.layoutParams = lp
                    tableRow.addView(spacer)
                } else {
                    cal.set(currentYear, currentMonth - 1, day)
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                    val isSat = dow == Calendar.SATURDAY
                    val isSun = dow == Calendar.SUNDAY
                    val dayStr = String.format("%04d-%02d-%02d", currentYear, currentMonth, day)
                    val isSelected = dayStr == selectedDate
                    val isToday = isCurrentMonth && day == todayCal.get(Calendar.DAY_OF_MONTH)
                    val hasRecord = recordMap.containsKey(dayStr)
                    val isHol = ds.isHoliday(dayStr)

                    val label = if (hasRecord) "$day\n●" else day.toString()
                    val tv = TextView(this).apply {
                        text = label
                        gravity = Gravity.CENTER
                        textSize = 13f
                        setLineSpacing(0f, 1.1f)
                        val lp2 = TableRow.LayoutParams(0,
                            resources.getDimensionPixelSize(R.dimen.day_cell_height), 1f)
                        layoutParams = lp2

                        when {
                            isSelected -> {
                                setBackgroundColor(getColor(context, R.color.primary))
                                setTextColor(Color.WHITE)
                            }
                            isToday -> {
                                setBackgroundColor(getColor(context, R.color.accent))
                                setTextColor(Color.WHITE)
                            }
                            else -> {
                                setBackgroundColor(Color.TRANSPARENT)
                                setTextColor(when {
                                    isHol -> getColor(context, R.color.holiday_color)
                                    isSat || isSun -> getColor(context, R.color.weekend_color)
                                    else -> Color.parseColor("#333333")
                                })
                            }
                        }
                        setOnClickListener {
                            selectedDate = dayStr
                            updateAll()
                        }
                    }
                    tableRow.addView(tv)
                }
            }
            calendarTable.addView(tableRow)
        }
    }

    private fun saveRecord() {
        val bonus = etBonus.text.toString().toDoubleOrNull() ?: 0.0
        if (!useDailyMode) {
            val rate = etHourlyRate.text.toString().toDoubleOrNull()
            val hours = etHours.text.toString().toDoubleOrNull() ?: 0.0
            if (rate == null) { Toast.makeText(this, "请填写时薪", Toast.LENGTH_SHORT).show(); return }
            if (rate < 0 || hours < 0 || bonus < 0) {
                Toast.makeText(this, "数值不能为负", Toast.LENGTH_SHORT).show(); return
            }
            val total = rate * hours + bonus
            tvDayTotal.text = "¥${String.format("%.2f", total)}"
            ds.saveRecord(
                SalaryRecord.fromForm(selectedDate, rate, hours, 0.0, isHoliday, isWeekend, bonus, "")
            )
        } else {
            val daily = etDailyRate.text.toString().toDoubleOrNull() ?: 0.0
            if (daily < 0) { Toast.makeText(this, "日薪不能为负", Toast.LENGTH_SHORT).show(); return }
            val total = daily + bonus
            tvDayTotal.text = "¥${String.format("%.2f", total)}"
            ds.saveRecord(
                SalaryRecord.fromForm(selectedDate, 0.0, 0.0, daily, isHoliday, isWeekend, bonus, "")
            )
        }
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        buildCalendar()
        lvRecords.adapter = RecordAdapter(ds.getRecordsForMonth(currentYear, currentMonth))
    }

    private fun deleteRecord() {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定删除 $selectedDate 的记录吗？")
            .setPositiveButton("删除") { _, _ ->
                ds.deleteRecord(selectedDate)
                updateAll()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSettings() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_settings)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()

        val etNormal = dialog.findViewById<EditText>(R.id.et_normal_rate)!!
        val etHol    = dialog.findViewById<EditText>(R.id.et_holiday_rate)!!
        val etWeekend = dialog.findViewById<EditText>(R.id.et_weekend_rate)!!
        val etHols   = dialog.findViewById<EditText>(R.id.et_holidays)!!
        dialog.findViewById<Button>(R.id.btn_save)!!.setOnClickListener {
            val n  = etNormal.text.toString().toDoubleOrNull() ?: return@setOnClickListener
            val h  = etHol.text.toString().toDoubleOrNull() ?: return@setOnClickListener
            val w  = etWeekend.text.toString().toDoubleOrNull() ?: return@setOnClickListener
            val holSet = etHols.text.toString().split("\n")
                .map { it.trim() }
                .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                .toSet()
            ds.saveSettings(SalarySettings(n, h, w, holSet))
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            updateAll()
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.btn_cancel)!!.setOnClickListener { dialog.dismiss() }

        val s = ds.loadSettings()
        etNormal.setText(String.format("%.1f", s.normalHourlyRate))
        etHol.setText(String.format("%.1f", s.holidayHourlyRate))
        etWeekend.setText(String.format("%.1f", s.weekendHourlyRate))
        etHols.setText(s.customHolidays.joinToString("\n"))
    }

    inner class RecordAdapter(private val records: List<SalaryRecord>) : BaseAdapter() {
        override fun getCount() = records.size
        override fun getItem(pos: Int) = records[pos]
        override fun getItemId(pos: Int) = pos.toLong()
        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.item_record, parent, false)
            val r = records[pos]
            val dayNum = r.date.substring(8, 10).toInt().toString() + "日"
            v.findViewById<TextView>(R.id.tv_day).text = dayNum
            v.findViewById<TextView>(R.id.tv_hours).text =
                if (r.dailyRate > 0) "-" else "${r.hours}h"
            v.findViewById<TextView>(R.id.tv_rate).text =
                if (r.dailyRate > 0) "固定" else "${r.hourlyRate}/h"
            val bonusStr = if (r.bonus > 0) "+¥${String.format("%.0f", r.bonus)}" else ""
            v.findViewById<TextView>(R.id.tv_total).text =
                "¥${String.format("%.2f", r.total)}$bonusStr"
            val flag = v.findViewById<TextView>(R.id.tv_flag)
            val (text, color) = when {
                r.isHoliday -> "节假日" to getColor(this@MainActivity, R.color.holiday_color)
                r.isWeekend -> "周末" to getColor(this@MainActivity, R.color.weekend_color)
                else -> "工作" to getColor(this@MainActivity, R.color.workday_color)
            }
            flag.text = text
            flag.setBackgroundColor(color)
            if (r.date == dateFormat.format(Date())) {
                v.setBackgroundColor(getColor(this@MainActivity, R.color.today_bg))
            }
            return v
        }
    }

    override fun onResume() { super.onResume(); updateAll() }
}
