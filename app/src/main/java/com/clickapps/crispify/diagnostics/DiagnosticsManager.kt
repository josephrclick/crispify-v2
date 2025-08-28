package com.clickapps.crispify.diagnostics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages local diagnostics collection for Crispify
 * 
 * Privacy-preserving implementation that:
 * - Only collects non-identifiable metrics
 * - Never stores user text content
 * - Works entirely offline (no network operations)
 * - Respects user opt-in/opt-out preferences
 */
class DiagnosticsManager(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val DIAGNOSTICS_ENABLED_KEY = booleanPreferencesKey("diagnostics_enabled")
        private const val MAX_STORED_METRICS = 100 // Limit storage to prevent memory issues
    }
    
    // Thread-safe storage for metrics
    private val metricsQueue = ConcurrentLinkedQueue<DiagnosticMetric>()
    
    /**
     * Check if diagnostics are currently enabled
     */
    suspend fun isDiagnosticsEnabled(): Boolean {
        return dataStore.data
            .map { preferences ->
                preferences[DIAGNOSTICS_ENABLED_KEY] ?: false // Default to disabled
            }
            .first()
    }
    
    /**
     * Enable or disable diagnostics collection
     */
    suspend fun setDiagnosticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DIAGNOSTICS_ENABLED_KEY] = enabled
        }
        
        if (!enabled) {
            // Clear all metrics when disabled for privacy
            clearMetrics()
        }
    }
    
    /**
     * Record a metric value (only if diagnostics enabled)
     */
    suspend fun recordMetric(type: MetricType, value: Number) {
        if (!isDiagnosticsEnabled()) return
        
        val metric = DiagnosticMetric(
            type = type,
            value = value,
            timestamp = System.currentTimeMillis()
        )
        
        metricsQueue.offer(metric)
        
        // Ensure we don't exceed storage limit
        while (metricsQueue.size > MAX_STORED_METRICS) {
            metricsQueue.poll()
        }
    }
    
    /**
     * Record an error event (only if diagnostics enabled)
     */
    suspend fun recordError(errorCode: ErrorCode) {
        recordMetric(MetricType.ERROR_CODE, errorCode.code)
    }
    
    /**
     * Record a complete text processing session
     * Note: Does NOT store any actual text content, only metrics
     */
    suspend fun recordProcessingSession(
        inputLength: Int,
        outputLength: Int,
        timeToFirstToken: Long,
        tokensPerSecond: Double,
        memoryUsedMB: Long
    ) {
        if (!isDiagnosticsEnabled()) return
        
        // Record individual metrics
        recordMetric(MetricType.INPUT_LENGTH, inputLength)
        recordMetric(MetricType.OUTPUT_LENGTH, outputLength)
        recordMetric(MetricType.TIME_TO_FIRST_TOKEN, timeToFirstToken)
        recordMetric(MetricType.TOKENS_PER_SECOND, tokensPerSecond)
        recordMetric(MetricType.MEMORY_PEAK_MB, memoryUsedMB)
    }
    
    /**
     * Clear all stored metrics
     */
    fun clearMetrics() {
        metricsQueue.clear()
    }
    
    /**
     * Get all stored metrics for testing/debugging
     */
    fun getStoredMetrics(): List<DiagnosticMetric> {
        return metricsQueue.toList()
    }
    
    /**
     * Export metrics in human-readable format
     * Includes both raw values and friendly interpretations
     */
    fun exportMetrics(): String {
        val metrics = getStoredMetrics()
        if (metrics.isEmpty()) {
            return "No diagnostics data available."
        }
        
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        sb.appendLine("=== Crispify Diagnostics Export ===")
        sb.appendLine("Generated: ${dateFormat.format(Date())}")
        sb.appendLine("Total metrics: ${metrics.size}")
        sb.appendLine()
        
        // Group metrics by type for better readability
        val groupedMetrics = metrics.groupBy { it.type }
        
        groupedMetrics.forEach { (type, typeMetrics) ->
            sb.appendLine("--- ${type.displayName} ---")
            
            typeMetrics.forEach { metric ->
                val interpretation = interpretMetric(metric)
                sb.appendLine("  ${dateFormat.format(Date(metric.timestamp))}: $interpretation")
            }
            
            // Add summary statistics
            if (type.isNumeric) {
                val values = typeMetrics.map { metric -> metric.value.toDouble() }
                if (values.isNotEmpty()) {
                    val avg = values.average()
                    val min = values.minOrNull() ?: 0.0
                    val max = values.maxOrNull() ?: 0.0
                    
                    sb.appendLine("  Summary: Avg=${type.format(avg)}, Min=${type.format(min)}, Max=${type.format(max)}")
                }
            }
            sb.appendLine()
        }
        
        sb.appendLine("=== End of Export ===")
        
        return sb.toString()
    }
    
    /**
     * Interpret a metric value with human-friendly description
     */
    private fun interpretMetric(metric: DiagnosticMetric): String {
        return when (metric.type) {
            MetricType.TIME_TO_FIRST_TOKEN -> {
                val seconds = metric.value.toLong() / 1000.0
                val rating = when {
                    seconds < 2.0 -> "Fast"
                    seconds < 4.0 -> "Okay"
                    else -> "Slow"
                }
                "Time to First Token: ${seconds}s ($rating)"
            }
            
            MetricType.TOKENS_PER_SECOND -> {
                val tps = metric.value.toDouble()
                val rating = when {
                    tps > 50 -> "Good"
                    tps > 20 -> "Acceptable"
                    else -> "Slow"
                }
                "Tokens/Second: $tps ($rating)"
            }
            
            MetricType.MEMORY_PEAK_MB -> {
                val mb = metric.value.toLong()
                val rating = when {
                    mb < 100 -> "Low"
                    mb < 200 -> "Normal"
                    else -> "High"
                }
                "Memory Peak: ${mb}MB ($rating)"
            }
            
            MetricType.ERROR_CODE -> {
                val errorCode = ErrorCode.fromCode(metric.value.toInt())
                "Error: ${errorCode.description}"
            }
            
            MetricType.INPUT_LENGTH -> {
                "Input Length: ${metric.value} characters"
            }
            
            MetricType.OUTPUT_LENGTH -> {
                "Output Length: ${metric.value} characters"
            }
        }
    }
}

/**
 * Types of metrics that can be collected
 */
enum class MetricType(
    val displayName: String,
    val isNumeric: Boolean = true
) {
    TIME_TO_FIRST_TOKEN("Time to First Token", true),
    TOKENS_PER_SECOND("Processing Speed", true),
    MEMORY_PEAK_MB("Memory Usage", true),
    ERROR_CODE("Errors", false),
    INPUT_LENGTH("Input Size", true),
    OUTPUT_LENGTH("Output Size", true);
    
    fun format(value: Double): String {
        return when (this) {
            TIME_TO_FIRST_TOKEN -> "${value / 1000.0}s"
            TOKENS_PER_SECOND -> "%.1f tok/s".format(value)
            MEMORY_PEAK_MB -> "${value.toLong()}MB"
            INPUT_LENGTH, OUTPUT_LENGTH -> "${value.toInt()} chars"
            ERROR_CODE -> value.toInt().toString()
        }
    }
}

/**
 * Error codes for diagnostics
 */
enum class ErrorCode(val code: Int, val description: String) {
    UNKNOWN(0, "Unknown error"),
    MODEL_INITIALIZATION_FAILED(1001, "Model initialization failed"),
    OUT_OF_MEMORY(1002, "Out of memory"),
    TEXT_TOO_LONG(1003, "Input text too long"),
    PROCESSING_FAILED(1004, "Text processing failed");
    
    companion object {
        fun fromCode(code: Int): ErrorCode {
            return values().find { it.code == code } ?: UNKNOWN
        }
    }
}

/**
 * Data class for storing diagnostic metrics
 */
data class DiagnosticMetric(
    val type: MetricType,
    val value: Number,
    val timestamp: Long
)