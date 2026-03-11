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

    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

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
        val languages = arrayOf(getString(R.string.lang_en), getString(R.string.lang_ru), getString(R.string.lang_lv))
        val langCodes = arrayOf("en", "ru", "lv")
        
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

        val result = StringBuilder()
        val isFullEntry = rbFull.isChecked

        val bed = getString(R.string.symbol_bed)
        val avail = getString(R.string.symbol_availability)

        // Forward from removal (Base road)
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

        // Backward from insertion (Road back)
        val i4 = insertion.clone() as Calendar
        val i3 = (i4.clone() as Calendar).apply { 
            add(Calendar.HOUR_OF_DAY, -24)
            set(Calendar.MINUTE, 0)
        }
        val i2 = (i3.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }
        val i1 = (i2.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }
        val i0 = (i1.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }

        if (r4.after(i0)) {
            showErrorDialog(getString(R.string.error_period_short))
            return
        }

        if (isFullEntry) {
            result.append("M  ${dateTimeFormat.format(r0.time)} \n$bed ${dateTimeFormat.format(r1.time)}\n\n")
            result.append("M  ${dateTimeFormat.format(r1.time)} \n$avail  ${dateTimeFormat.format(r2.time)}\n\n")
            result.append("M  ${dateTimeFormat.format(r2.time)} \n$bed ${dateTimeFormat.format(r3.time)}\n\n")
            result.append("M  ${dateTimeFormat.format(r3.time)} \n$avail  ${dateTimeFormat.format(r4.time)}\n\n")
        }

        val vacationStart = if (isFullEntry) r4 else r0
        result.append("M  ${dateTimeFormat.format(vacationStart.time)} \n$bed ${dateTimeFormat.format(i0.time)}\n\n")

        result.append("M  ${dateTimeFormat.format(i0.time)} \n$avail  ${dateTimeFormat.format(i1.time)}\n\n")
        result.append("M  ${dateTimeFormat.format(i1.time)} \n$bed ${dateTimeFormat.format(i2.time)}\n\n")
        result.append("M  ${dateTimeFormat.format(i2.time)} \n$avail  ${dateTimeFormat.format(i3.time)}\n\n")
        result.append("M  ${dateTimeFormat.format(i3.time)} \n$bed ${dateTimeFormat.format(i4.time)}\n\n")

        showResultDialog(result.toString())
    }

    private fun showResultDialog(result: String) {
        val dialog = Dialog(this, com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
        dialog.setContentView(R.layout.dialog_result)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        
        val tvResultPopup = dialog.findViewById<TextView>(R.id.tvResultPopup)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)
        
        tvResultPopup.typeface = Typeface.MONOSPACE
        tvResultPopup.text = result
        btnClose.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
