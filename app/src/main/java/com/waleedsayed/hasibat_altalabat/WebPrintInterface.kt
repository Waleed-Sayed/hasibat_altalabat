package com.waleedsayed.hasibat_altalabat

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class WebPrintInterface(private val context: Context, private val webView: WebView) {

    // 1. خيار الحفظ كملف PDF أو الطباعة عبر نظام أندرويد الرسمي
    @JavascriptInterface
    fun printPage(jobName: String?) {
        webView.post {
            try {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val safeTitle = if (jobName.isNullOrBlank()) "تقرير_حاسبة_الطلبات" else jobName
                val printAdapter = webView.createPrintDocumentAdapter(safeTitle)
                val builder = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)

                printManager.print(safeTitle, printAdapter, builder.build())
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ أثناء فتح نافذة الحفظ: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 2. خيار إنشاء ملف PDF كامل ومشاركته فوراً أو حفظه في مجلد التنزيلات
    @JavascriptInterface
    fun sharePdf(jobName: String?) {
        webView.post {
            try {
                val safeTitle = (if (jobName.isNullOrBlank()) "تقرير_الطلبات" else jobName)
                    .replace(" ", "_")
                    .replace("(", "")
                    .replace(")", "")

                // حساب الطول الكامل للتقرير وليس الجزء الظاهر فقط
                val width = webView.width.takeIf { it > 0 } ?: 1080
                val contentHeight = (webView.contentHeight * webView.scale).toInt()
                val height = contentHeight.takeIf { it > 0 } ?: 1920

                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
                val page = document.startPage(pageInfo)

                val canvas: Canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                webView.draw(canvas)

                document.finishPage(page)

                // حفظ نسخة في مجلد التنزيلات (Downloads)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val downloadFile = File(downloadsDir, "$safeTitle.pdf")
                try {
                    FileOutputStream(downloadFile).use { out ->
                        document.writeTo(out)
                    }
                } catch (_: Exception) {}

                // حفظ نسخة للمشاركة السريعة
                val cacheDir = File(context.cacheDir, "pdfs")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val shareFile = File(cacheDir, "$safeTitle.pdf")
                FileOutputStream(shareFile).use { out ->
                    document.writeTo(out)
                }
                document.close()

                Toast.makeText(context, "تم حفظ النسخة في التنزيلات وجاري فتح المشاركة...", Toast.LENGTH_SHORT).show()

                // فتح نافذة المشاركة في أندرويد
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو حفظ التقرير PDF"))

            } catch (e: Exception) {
                Toast.makeText(context, "تعذر مشاركة الملف، استخدم زر الحفظ كـ PDF", Toast.LENGTH_LONG).show()
            }
        }
    }
}