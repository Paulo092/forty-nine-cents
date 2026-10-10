package com.fortyninecents.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.fortyninecents.domain.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Grava o CSV no cache do app e monta a Intent de compartilhamento (Drive, Sheets, e-mail...). */
class CsvExporter(
    private val context: Context,
    private val builder: CsvBuilder = CsvBuilder(),
) {

    suspend fun export(transactions: List<Transaction>, label: String): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        // remove exportações antigas para não acumular lixo no cache
        dir.listFiles()?.forEach { it.delete() }

        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
        val file = File(dir, "49centavos_${label}_$stamp.csv")
        file.writeText(builder.build(transactions), Charsets.UTF_8)

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(uri: Uri): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Gastos – 49 Centavos")
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Exportar planilha")
    }

    private companion object {
        const val EXPORT_DIR = "exports"
    }
}
