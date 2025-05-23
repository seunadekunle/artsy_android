package com.example.asssignment_4.ui.components

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.coroutines.delay

@Composable
fun TimeAgoText(
    dateTime: String,
    delayMillis: Long = 60_000 // Update every minute by default
) {
    val context = LocalContext.current
    
    // Convert string to Instant
    val instant = remember(dateTime) {
        try {
            Instant.parse(dateTime)
        } catch (e: DateTimeParseException) {
            null
        }
    }
    
    if (instant == null) {
        Text(
            text = "Unknown time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
        return
    }
    
    // Update the time ago text periodically
    val timeAgo by produceState(initialValue = getRelativeTimeSpan(instant, context), instant) {
        while (true) {
            value = getRelativeTimeSpan(instant, context)
            delay(delayMillis)
        }
    }

    Text(
        text = timeAgo,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Normal
    )
}

fun getRelativeTimeSpan(dateTime: Instant, context: Context): String {
    val now = System.currentTimeMillis()
    val time = dateTime.toEpochMilli()
    
    if (time > now) {
        return "just now"
    }
    
    val diff = now - time
    
    return when {
        diff < 1_000 -> "just now"
        diff < 60_000 -> {
            val seconds = diff / 1_000
            "$seconds ${if (seconds == 1L) "second" else "seconds"} ago"
        }
        diff < 3_600_000 -> {
            val minutes = diff / 60_000
            "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
        }
        diff < 86_400_000 -> {
            val hours = diff / 3_600_000
            "$hours ${if (hours == 1L) "hour" else "hours"} ago"
        }
        diff < 604_800_000 -> {
            val days = diff / 86_400_000
            "$days ${if (days == 1L) "day" else "days"} ago"
        }
        else -> DateUtils.getRelativeTimeSpanString(
            time,
            now,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString().replace("In ", "").plus(" ago")
    }
}
