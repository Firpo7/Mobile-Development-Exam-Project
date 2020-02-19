package com.example.md_project01

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class ChartViewWrapper(private val barChart: BarChart, private val barColor: Int) {

    init {
        barChart.legend.isEnabled = false
        barChart.setTouchEnabled(false)
        barChart.axisLeft.setDrawLabels(false)
        barChart.animateY(500)

        val xAxis = barChart.xAxis
        xAxis.textSize = 9f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)

        barChart.axisRight.axisMinimum = 0f
        barChart.axisLeft.axisMinimum = 0f

        barChart.data = makeBarData()
    }

    fun setChartValues(daysToShow: Int, stats: List<Stats> = listOf()) {
        var tmp = System.currentTimeMillis() - DAYS_1 * (daysToShow-1)
        val valuesYList = mutableListOf<BarEntry>()
        val barEntryLabels = mutableListOf<String>()

        var j = 0
        for ( i in 0 until daysToShow) {
            val k = sdf_chart.format( Date(tmp) )
            barEntryLabels.add(k)
            if ( j < stats.size && sdf_chart.format( stats[j].date ) == k ) {
                valuesYList.add( BarEntry(i.toFloat(), stats[j].distance.toFloat()) )
                ++j
            } else {
                valuesYList.add( BarEntry(i.toFloat(), 0f ) )
            }
            tmp += DAYS_1
        }

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(barEntryLabels)
        barChart.data =  makeBarData(valuesYList)
        refreshUI()
    }

    private fun makeBarData(values: MutableList<BarEntry> = mutableListOf()): BarData? {
        val barYSet = BarDataSet(values, "m")
        barYSet.color = barColor
        return BarData(barYSet)
    }

    private fun refreshUI() {
        barChart.notifyDataSetChanged()
        barChart.invalidate()
    }

    fun setBarChartDescription(boolean: Boolean) {
        barChart.description.isEnabled = boolean
    }

    fun setDescriptionText(defaultText: String) {
        val d = Description()
        d.text = defaultText
        barChart.description = d
    }

    companion object {
        val sdf_chart = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}