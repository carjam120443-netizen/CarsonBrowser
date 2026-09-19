package com.carsonbrowser

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.inputmethod.EditorInfo

class MainActivity : Activity() {
    private lateinit var webContainer: FrameLayout
    private lateinit var addressBar: EditText
    private lateinit var tabButton: Button

    private val tabs = mutableListOf<WebView>()
    private val tabTitles = mutableListOf<String>()
    private var currentTab = 0

    private val homeUrl = "https://duckduckgo.com/"
    private val searchUrl = "https://duckduckgo.com/?q="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resolveColor(android.R.attr.colorBackground))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            elevation = dp(3).toFloat()
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
        }

        val navigationRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val backButton = toolbarButton("‹", "Back")
        val forwardButton = toolbarButton("›", "Forward")
        val homeButton = toolbarButton("⌂", "Home")

        addressBar = EditText(this).apply {
            hint = "Search with DuckDuckGo or enter address"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            textSize = 15f
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(14), 0, dp(14), 0)
            background = roundedBackground(
                resolveColor(com.google.android.material.R.attr.colorSurfaceVariant),
                22
            )
        }

        val addressParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
            marginStart = dp(4)
            marginEnd = dp(4)
        }

        navigationRow.addView(backButton)
        navigationRow.addView(forwardButton)
        navigationRow.addView(homeButton)
        navigationRow.addView(addressBar, addressParams)

        val reloadButton = toolbarButton("↻", "Reload")
        tabButton = toolbarButton("1", "Tabs")
        navigationRow.addView(reloadButton)
        navigationRow.addView(tabButton)
        toolbar.addView(navigationRow)

        val quickRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }

        val browserLabel = TextView(this).apply {
            text = "  🛡  CarsonBrowser"
            textSize = 12f
            alpha = 0.75f
            gravity = Gravity.CENTER_VERTICAL
        }

        quickRow.addView(browserLabel, LinearLayout.LayoutParams(0, dp(28), 1f))

        val newTabButton = toolbarButton("+", "New tab")
        quickRow.addView(newTabButton)
        toolbar.addView(quickRow)

        webContainer = FrameLayout(this)
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        root.addView(webContainer, LinearLayout.LayoutParams(-1, 0, 1f))

        root.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setContentView(root)

        backButton.setOnClickListener {
            currentWebView()?.let { webView ->
                if (webView.canGoBack()) webView.goBack()
            }
        }

        forwardButton.setOnClickListener {
            currentWebView()?.let { webView ->
                if (webView.canGoForward()) webView.goForward()
            }
        }

        homeButton.setOnClickListener {
            currentWebView()?.loadUrl(homeUrl)
        }

        reloadButton.setOnClickListener {
            currentWebView()?.reload()
        }

        tabButton.setOnClickListener {
            showTabSwitcher()
        }

        newTabButton.setOnClickListener {
            createTab(homeUrl)
        }

        addressBar.setOnEditorActionListener { _, _, _ ->
            navigateFromAddressBar()
            true
        }

        createTab(homeUrl)
    }

    private fun createTab(url: String) {
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.setSupportMultipleWindows(false)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                    if (view == currentWebView()) {
                        addressBar.setText(pageUrl ?: "")
                        addressBar.setSelection(addressBar.text.length)
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    val tabIndex = tabs.indexOf(view)
                    if (tabIndex >= 0) {
                        tabTitles[tabIndex] = title?.takeIf { it.isNotBlank() } ?: "New tab"
                    }
                }
            }
        }

        tabs.add(webView)
        tabTitles.add("New tab")
        webContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        switchToTab(tabs.lastIndex)
        webView.loadUrl(url)
    }

    private fun switchToTab(index: Int) {
        if (index !in tabs.indices) return

        currentTab = index
        tabs.forEachIndexed { tabIndex, webView ->
            webView.visibility = if (tabIndex == currentTab) View.VISIBLE else View.GONE
        }

        val webView = tabs[currentTab]
        addressBar.setText(webView.url ?: "")
        addressBar.setSelection(addressBar.text.length)
        tabButton.text = tabs.size.toString()
    }

    private fun showTabSwitcher() {
        val items = tabTitles.mapIndexed { index, title ->
            (index + 1).toString() + ". " + title.take(40)
        }.toMutableList()

        items.add("＋ New tab")

        android.app.AlertDialog.Builder(this)
            .setTitle("Tabs (" + tabs.size + ")")
            .setItems(items.toTypedArray()) { _, which ->
                if (which == tabs.size) {
                    createTab(homeUrl)
                } else {
                    switchToTab(which)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun navigateFromAddressBar() {
        val input = addressBar.text.toString().trim()
        if (input.isEmpty()) return

        val destination = when {
            input.startsWith("https://", true) || input.startsWith("http://", true) -> input
            input.contains(" ") -> searchUrl + android.net.Uri.encode(input)
            input.contains(".") -> "https://" + input
            else -> searchUrl + android.net.Uri.encode(input)
        }

        currentWebView()?.loadUrl(destination)
    }

    private fun currentWebView(): WebView? = tabs.getOrNull(currentTab)

    override fun onBackPressed() {
        val webView = currentWebView()
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else if (tabs.size > 1) {
            closeCurrentTab()
        } else {
            super.onBackPressed()
        }
    }

    private fun closeCurrentTab() {
        if (tabs.size <= 1) return

        val closingIndex = currentTab
        val webView = tabs.removeAt(closingIndex)
        tabTitles.removeAt(closingIndex)
        webContainer.removeView(webView)
        webView.destroy()

        currentTab = minOf(closingIndex, tabs.lastIndex)
        switchToTab(currentTab)
    }

    private fun toolbarButton(label: String, description: String): Button {
        return Button(this).apply {
            text = label
            contentDescription = description
            textSize = 19f
            minWidth = dp(44)
            minimumWidth = dp(44)
            minHeight = dp(44)
            minimumHeight = dp(44)
            setPadding(0, 0, 0, 0)
            stateListAnimator = null
        }
    }

    private fun roundedBackground(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun resolveColor(attribute: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attribute, typedValue, true)
        return if (typedValue.resourceId != 0) {
            getColor(typedValue.resourceId)
        } else {
            typedValue.data
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
