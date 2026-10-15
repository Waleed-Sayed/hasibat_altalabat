package com.waleedsayed.hasibat_altalabat

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        // ربط أداة تصدير الـ PDF بصفحة الويب تحت اسم AndroidPrint
        webView.addJavascriptInterface(WebPrintInterface(this, webView), "AndroidPrint")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // حقن كود ذكي يقوم بربط الطباعة وحفظ واسترجاع آخر شهر ويوم تلقائياً
                injectPrintAndStateSaver()
            }
        }

        // تفعيل نوافذ التنبيه والتأكيد (مثل تأكيد الحذف أو المسح) داخل الـ WebView
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("حسناً") { _, _ -> result?.confirm() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("نعم") { _, _ -> result?.confirm() }
                    .setNegativeButton("إلغاء") { _, _ -> result?.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }
        }

        // تحميل ملف الواجهة
        webView.loadUrl("file:///android_asset/index.html")

        // التحكم بزر الرجوع
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun injectPrintAndStateSaver() {
        val jsCode = """
            (function() {
                // 1. تحويل أمر الطباعة العادي إلى أداة تصدير الـ PDF في الأندرويد
                window.print = function() {
                    if (typeof AndroidPrint !== 'undefined' && AndroidPrint.printPage) {
                        AndroidPrint.printPage('تقرير_حاسبة_الطلبات');
                    }
                };

                // 2. دالة لحفظ الشهر واليوم الحاليين في الذاكرة المحلية
                function saveCurrentDateSelection() {
                    var selects = document.querySelectorAll('select');
                    if (selects.length >= 2) {
                        localStorage.setItem('saved_app_month', selects[0].value);
                        localStorage.setItem('saved_app_day', selects[1].value);
                    }
                }

                // 3. دالة لاسترجاع آخر شهر ويوم عند فتح التطبيق
                function restoreSavedDateSelection() {
                    var selects = document.querySelectorAll('select');
                    if (selects.length >= 2) {
                        var savedMonth = localStorage.getItem('saved_app_month');
                        var savedDay = localStorage.getItem('saved_app_day');

                        if (savedMonth !== null && savedMonth !== '') {
                            selects[0].value = savedMonth;
                            selects[0].dispatchEvent(new Event('change', { bubbles: true }));
                        }

                        // ننتظر جزء من الثانية حتى تتحدث قائمة الأيام بناءً على الشهر ثم نختار اليوم المحفوظ
                        setTimeout(function() {
                            if (savedDay !== null && savedDay !== '') {
                                selects[1].value = savedDay;
                                selects[1].dispatchEvent(new Event('change', { bubbles: true }));
                            }
                        }, 150);
                    }
                }

                // تنفيذ الاسترجاع بعد تحميل الصفحة وإغلاق أو ظهور النوافذ الترحيبية
                setTimeout(restoreSavedDateSelection, 300);

                // مراقبة أي تغيير في القوائم المنسدلة (الشهر أو اليوم) لحفظه فوراً
                document.addEventListener('change', function(e) {
                    if (e.target && e.target.tagName === 'SELECT') {
                        saveCurrentDateSelection();
                    }
                }, true);

                // مراقبة الضغط على الأزرار (اليوم التالي، اليوم السابق، حفظ اليوم) لحفظ التاريخ الجديد فوراً
                document.addEventListener('click', function(e) {
                    setTimeout(saveCurrentDateSelection, 200);
                }, true);
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode, null)
    }
}