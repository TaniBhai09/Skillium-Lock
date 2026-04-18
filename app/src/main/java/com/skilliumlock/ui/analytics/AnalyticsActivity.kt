package com.skilliumlock.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.skilliumlock.R
import com.skilliumlock.data.db.entity.ProfileEntity
import com.skilliumlock.data.db.entity.UsageLogEntity
import com.skilliumlock.manager.ProfileManager
import kotlinx.coroutines.*

/**
 * Analytics screen showing daily app usage per profile as a bar chart.
 * Uses MPAndroidChart for visualization.
 */
class AnalyticsActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var profileManager: ProfileManager
    private lateinit var profileSpinner: Spinner
    private lateinit var barChart: BarChart
    private lateinit var noDataText: TextView

    private var profiles: List<ProfileEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        profileManager = ProfileManager(this)
        profileSpinner = findViewById(R.id.profileSpinner)
        barChart = findViewById(R.id.barChart)
        noDataText = findViewById(R.id.noDataText)

        setupChart()
        loadProfiles()
    }

    private fun setupChart() {
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(false) // No touch on TV
            setPinchZoom(false)
            setScaleEnabled(false)
            legend.textColor = Color.WHITE
            legend.textSize = 14f

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                textSize = 12f
                setDrawGridLines(false)
                granularity = 1f
            }

            axisLeft.apply {
                textColor = Color.WHITE
                textSize = 12f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#333355")
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
        }
    }

    private fun loadProfiles() {
        scope.launch {
            profiles = withContext(Dispatchers.IO) {
                profileManager.profileRepo.getNonAdminProfiles()
            }

            if (profiles.isEmpty()) {
                noDataText.visibility = View.VISIBLE
                noDataText.text = "No profiles created yet"
                return@launch
            }

            val adapter = ArrayAdapter(
                this@AnalyticsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                profiles.map { it.name }
            )
            profileSpinner.adapter = adapter

            profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    loadChartData(profiles[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadChartData(profile: ProfileEntity) {
        scope.launch {
            val logs = withContext(Dispatchers.IO) {
                profileManager.usageLogRepo.getLogsForLastNDays(profile.id, 7)
            }

            if (logs.isEmpty()) {
                noDataText.visibility = View.VISIBLE
                barChart.visibility = View.GONE
                return@launch
            }

            noDataText.visibility = View.GONE
            barChart.visibility = View.VISIBLE

            // Group by app name and sum durations
            val appUsage = logs.groupBy { it.appName.ifEmpty { it.packageName } }
                .mapValues { (_, entries) ->
                    entries.sumOf { it.durationSeconds } / 60f // Convert to minutes
                }
                .toList()
                .sortedByDescending { it.second }
                .take(8) // Top 8 apps

            val entries = appUsage.mapIndexed { index, (_, minutes) ->
                BarEntry(index.toFloat(), minutes)
            }

            val labels = appUsage.map { it.first }

            val chartColors = listOf(
                Color.parseColor("#1E88E5"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#7C4DFF"),
                Color.parseColor("#FF6E40"),
                Color.parseColor("#69F0AE"),
                Color.parseColor("#FFD740"),
                Color.parseColor("#FF5252"),
                Color.parseColor("#448AFF")
            )

            val dataSet = BarDataSet(entries, "Usage (minutes)").apply {
                colors = chartColors.take(entries.size)
                valueTextColor = Color.WHITE
                valueTextSize = 12f
            }

            barChart.data = BarData(dataSet)
            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            barChart.xAxis.labelCount = labels.size
            barChart.animateY(800)
            barChart.invalidate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
