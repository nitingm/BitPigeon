package com.codingskillshub.bitpigeon.common

import javax.inject.Inject

class DateTimeUtil @Inject constructor() {
    fun getCurrentDateTime(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val date = java.util.Date()
        return dateFormat.format(date)
    }

    fun getCurrentDateTimeInMilliSeconds(): Long {
        return java.util.Date().time
    }

    fun toFriendlyDate(dateTime: String): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

        return try {
            val parsedDate = dateFormat.parse(dateTime) ?: return dateTime
            val calendar = java.util.Calendar.getInstance()
            calendar.time = parsedDate

            val today = java.util.Calendar.getInstance()
            val yesterday = java.util.Calendar.getInstance()
            yesterday.add(java.util.Calendar.DAY_OF_MONTH, -1)

            // Compare only the date part (year, month, day)
            when {
                calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) -> "Today"

                calendar.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) &&
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR) -> "Yesterday"

                else -> outputFormat.format(parsedDate)
            }
        } catch (e: Exception) {
            dateTime // Return original string if parsing fails
        }
    }
}