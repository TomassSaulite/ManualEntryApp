package com.example.manualentryapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var tvResult: TextView
    private lateinit var rbFull: RadioButton

    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("lang", null)
        if (lang != null) {
            val locale = Locale(lang)
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
        
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("lang")) {
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
        tvResult = findViewById(R.id.tvResult)
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
                getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString("lang", selectedLang).apply()
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
            tvResult.text = getString(R.string.error_set_dates)
            return
        }

        if (removal.after(insertion)) {
            tvResult.text = getString(R.string.error_date_order)
            return
        }

        val result = StringBuilder()
        val isFullEntry = rbFull.isChecked

        // Forward from removal (Base road)
        val r0 = removal.clone() as Calendar
        val r1 = (r0.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 24) }
        val r2 = (r1.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 12) }
        val r3 = (r2.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 12) }
        val r4 = (r3.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 12) }

        // Backward from insertion (Road back)
        val i4 = insertion.clone() as Calendar
        val i3 = (i4.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -24) }
        val i2 = (i3.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }
        val i1 = (i2.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }
        val i0 = (i1.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -12) }

        if (r4.after(i0)) {
            tvResult.text = getString(R.string.error_period_short)
            return
        }

        val bed = getString(R.string.symbol_bed)
        val avail = getString(R.string.symbol_availability)
        val vacBed = getString(R.string.symbol_vacation_bed)

        if (isFullEntry) {
//            result.append(getString(R.string.label_getting_to_base)).append("\n")
            result.append("M${dateTimeFormat.format(r0.time)} \n \uD83D\uDECF${dateTimeFormat.format(r1.time)} \n\n")
            result.append("M${dateTimeFormat.format(r1.time)} \n ⧄${dateTimeFormat.format(r2.time)} \n\n")
            result.append("M${dateTimeFormat.format(r2.time)} \n \uD83D\uDECF${dateTimeFormat.format(r3.time)}\n\n")
            result.append("M${dateTimeFormat.format(r3.time)} \n ⧄${dateTimeFormat.format(r4.time)} \n\n")
            result.append("\n")
        }

//        result.append(getString(R.string.label_vacation)).append("\n")
        val vacationStart = if (isFullEntry) r4 else r0
        result.append("M${dateTimeFormat.format(vacationStart.time)}  \n \uD83D\uDECF${dateTimeFormat.format(i0.time)} \n\n")

//        result.append("\n").append(getString(R.string.label_getting_back)).append("\n")
        result.append("M${dateTimeFormat.format(i0.time)} \n ⧄ ${dateTimeFormat.format(i1.time)} \n\n")
        result.append("M${dateTimeFormat.format(i1.time)} \n \uD83D\uDECF${dateTimeFormat.format(i2.time)} \n\n")
        result.append("M${dateTimeFormat.format(i2.time)} \n ⧄ ${dateTimeFormat.format(i3.time)} \n\n")
        result.append("M${dateTimeFormat.format(i3.time)} \n \uD83D\uDECF${dateTimeFormat.format(i4.time)} \n\n")

        tvResult.text = result.toString()
    }
}
