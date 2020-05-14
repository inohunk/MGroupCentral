package utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

fun openFile(path: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(Uri.fromFile(File(path)), "text/plain")
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    Intent.createChooser(intent, "Chose an app:")
    try {
        context.startActivity(intent)
    } catch (ex: java.lang.Exception) {
        Toast.makeText(context, "Could't open file", Toast.LENGTH_SHORT).show()
    }
}

fun sendFile(path: String, context: Context) {
    val uri = FileProvider.getUriForFile(context, context.packageName, File(path))
    val intent = Intent(Intent.ACTION_SEND)
        .apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_EMAIL, arrayOf("mgrouptrackers@yandex.ru"))
            putExtra(
                Intent.EXTRA_SUBJECT,
                convertMillisToTime(
                    System.currentTimeMillis(),
                    PATTERN_DAY_MONTH_YEAR
                )
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            type = "application/octet-stream"

        }
    context.startActivity(Intent.createChooser(intent, "Choose an action"))
}

fun sendText(text: String, context: Context) {
    val intent = Intent(Intent.ACTION_SEND)
        .apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf("mgrouptrackers@yandex.ru"))
            putExtra(
                Intent.EXTRA_SUBJECT,
                convertMillisToTime(
                    System.currentTimeMillis(),
                    PATTERN_DAY_MONTH_YEAR
                )
            )
            putExtra(
                Intent.EXTRA_TEXT,
                text
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            type = "text/plain"

        }
    context.startActivity(Intent.createChooser(intent, "Choose an action"))
}