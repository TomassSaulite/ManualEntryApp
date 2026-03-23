package com.example.manualentryapp

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var removalCalendar: Calendar? = null
    private var insertionCalendar: Calendar? = null

    private lateinit var tvRemoval: TextView
    private lateinit var tvInsertion: TextView
    private lateinit var rbFull: RadioButton
    private lateinit var rbBackOnly: RadioButton
    private lateinit var rbToBase: RadioButton

    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var currentBlocks = mutableListOf<TachoBlock>()

    data class TachoBlock(val activityResId: Int, val endTime: Calendar)

    companion object {
        private var languageSelectedThisSession = false
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("prefs", MODE_PRIVATE).getString("lang", null)
        if (lang != null) {
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!languageSelectedThisSession) {
            languageSelectedThisSession = true
            showLanguageDialog()
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvRemoval = findViewById(R.id.tvRemoval)
        tvInsertion = findViewById(R.id.tvInsertion)
        rbFull = findViewById(R.id.rbFull)
        rbBackOnly = findViewById(R.id.rbBackOnly)
        rbToBase = findViewById(R.id.rbToBase)

        findViewById<Button>(R.id.btnRemoval).setOnClickListener {
            pickDateTime(removalCalendar) { calendar ->
                removalCalendar = calendar
                tvRemoval.text = getString(R.string.removal_label, dateTimeFormat.format(calendar.time))
            }
        }

        findViewById<Button>(R.id.btnInsertion).setOnClickListener {
            pickDateTime(insertionCalendar) { calendar ->
                insertionCalendar = calendar
                tvInsertion.text = getString(R.string.insertion_label, dateTimeFormat.format(calendar.time))
            }
        }

        findViewById<Button>(R.id.btnInsertionNow).setOnClickListener {
            val now = Calendar.getInstance()
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            insertionCalendar = now
            tvInsertion.text = getString(R.string.insertion_label, dateTimeFormat.format(now.time))
        }

        findViewById<Button>(R.id.btnCalculate).setOnClickListener {
            calculateEntries()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.lang_en),
            getString(R.string.lang_ru),
            getString(R.string.lang_lv),
            getString(R.string.lang_hi),
            getString(R.string.lang_uz)
        )
        val langCodes = arrayOf("en", "ru", "lv", "hi", "uz")
        
        AlertDialog.Builder(this)
            .setTitle(R.string.choose_language)
            .setItems(languages) { _, which ->
                val selectedLang = langCodes[which]
                getSharedPreferences("prefs", MODE_PRIVATE).edit {
                    putString("lang", selectedLang)
                }
                recreate()
            }
            .setCancelable(false)
            .show()
    }

    private fun pickDateTime(currentValue: Calendar?, onDateTimePicked: (Calendar) -> Unit) {
        val calendar = currentValue ?: Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val picked = Calendar.getInstance()
                picked.set(year, month, dayOfMonth, hourOfDay, minute)
                picked.set(Calendar.SECOND, 0)
                picked.set(Calendar.MILLISECOND, 0)
                onDateTimePicked(picked)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun calculateEntries() {
        val removal = removalCalendar
        val insertion = insertionCalendar

        if (removal == null || insertion == null) {
            showErrorDialog(getString(R.string.error_set_dates))
            return
        }

        if (removal.after(insertion)) {
            showErrorDialog(getString(R.string.error_date_order))
            return
        }

        currentBlocks.clear()

        // Forward from removal
        val r0 = removal.clone() as Calendar
        val r1 = (r0.clone() as Calendar).apply { 
            add(Calendar.HOUR_OF_DAY, 24)
            if (get(Calendar.MINUTE) > 0) {
                set(Calendar.MINUTE, 0)
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
        val r2 = (r1.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 12) }
        val r3 = (r2.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 12) }
        val r4 = (r3.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 12) }

        // Backward from insertion
        val i4 = insertion.clone() as Calendar
        val i3 = (i4.clone() as Calendar).apply { 
            add(Calendar.HOUR_OF_DAY, -24)
            set(Calendar.MINUTE, 0)
        }
        val i2 = (i3.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }
        val i1 = (i2.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }
        val i0 = (i1.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }

        if (rbFull.isChecked) {
            if (r4.after(i0)) {
                showErrorDialog(getString(R.string.error_period_short))
                return
            }
        } else if (rbBackOnly.isChecked) {
            if (removal.after(i0)) {
                showErrorDialog(getString(R.string.error_period_short))
                return
            }
        } else if (rbToBase.isChecked) {
             if (r4.after(insertion)) {
                showErrorDialog(getString(R.string.error_period_short))
                return
            }
        }

        if (rbFull.isChecked || rbToBase.isChecked) {
            currentBlocks.add(TachoBlock(R.string.activity_rest, r1))
            currentBlocks.add(TachoBlock(R.string.activity_avail, r2))
            currentBlocks.add(TachoBlock(R.string.activity_rest, r3))
            currentBlocks.add(TachoBlock(R.string.activity_avail, r4))
        }

        if (rbFull.isChecked || rbBackOnly.isChecked) {
            currentBlocks.add(TachoBlock(R.string.activity_rest, i0))
            currentBlocks.add(TachoBlock(R.string.activity_avail, i1))
            currentBlocks.add(TachoBlock(R.string.activity_rest, i2))
            currentBlocks.add(TachoBlock(R.string.activity_avail, i3))
            currentBlocks.add(TachoBlock(R.string.activity_rest, i4))
        } else if (rbToBase.isChecked) {
            currentBlocks.add(TachoBlock(R.string.activity_rest, insertion))
        }

        val resultText = buildString {
            var startTime: Calendar = removal
            currentBlocks.forEach { block ->
                val symbol = when(block.activityResId) {
                    R.string.activity_rest -> getString(R.string.symbol_bed)
                    R.string.activity_avail -> getString(R.string.symbol_availability)
                    else -> "⚒"
                }
                
                val padding = if (symbol.length > 1) " " else "  "
                
                append("M  ${dateTimeFormat.format(startTime.time)}\n")
                append("$symbol$padding${dateTimeFormat.format(block.endTime.time)}\n\n")
                startTime = block.endTime
            }
        }

        showResultDialog(resultText.trim())
    }

    private fun showResultDialog(result: String) {
        val dialog = Dialog(this, R.style.Theme_ManualEntryApp)
        dialog.setContentView(R.layout.dialog_result)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        
        val tvResultPopup = dialog.findViewById<TextView>(R.id.tvResultPopup)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)
        val btnInfo = dialog.findViewById<Button>(R.id.btnInfo)
        
        tvResultPopup.typeface = Typeface.MONOSPACE
        tvResultPopup.text = result
        btnClose.setOnClickListener { dialog.dismiss() }
        btnInfo.setOnClickListener {
            showVdoInstructions()
        }
        
        dialog.show()
    }

    private fun showVdoInstructions() {
        val instr = StringBuilder()
        instr.append(getString(R.string.vdo_instr_intro)).append("\n\n")
        
        currentBlocks.forEachIndexed { index, block ->
            val activityName = getString(block.activityResId)
            val dateStr = dateFormat.format(block.endTime.time)
            val timeStr = timeFormat.format(block.endTime.time)
            
            instr.append(getString(R.string.vdo_instr_step, index + 1, activityName, dateStr, timeStr))
            instr.append("\n\n")
        }
        
        instr.append(getString(R.string.vdo_instr_final))

        AlertDialog.Builder(this)
            .setTitle(R.string.vdo_instructions_title)
            .setMessage(instr.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
