package com.guardbet.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var budgetTracker: BudgetTracker

    override fun onCreate() {
        super.onCreate()
        budgetTracker = BudgetTracker(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (overlayView != null) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_bubble, null)
        overlayView = view

        val layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 16
        params.y = 120

        setupContent(view)

        windowManager.addView(view, params)
        makeDraggable(view, params)
    }

    private fun setupContent(view: View) {
        val toggleButton = view.findViewById<Button>(R.id.toggleButton)
        val expandedLayout = view.findViewById<LinearLayout>(R.id.expandedLayout)
        val gameListText = view.findViewById<TextView>(R.id.gameListText)
        val budgetText = view.findViewById<TextView>(R.id.budgetText)
        val closeButton = view.findViewById<Button>(R.id.closeButton)

        val sb = StringBuilder()
        GameData.games.sortedBy { it.houseEdgePercent }.forEach {
            sb.append("• ${it.name}\n")
            sb.append("   RTP: ${it.rtpPercent}%  |  Avantage maison: ${it.houseEdgePercent}%\n")
            sb.append("   ${it.note}\n\n")
        }
        gameListText.text = sb.toString()

        fun refreshBudget() {
            val spent = budgetTracker.getTodaySpent()
            val limit = budgetTracker.getDailyLimit()
            val remaining = budgetTracker.remainingToday()
            val warning = if (budgetTracker.isOverLimit())
                "\n⚠️ LIMITE QUOTIDIENNE ATTEINTE" else ""
            budgetText.text = buildString {
                append("Dépensé aujourd'hui : ${"%.2f".format(spent)}\n")
                if (limit > 0) {
                    append("Limite du jour : ${"%.2f".format(limit)}\n")
                    append("Reste disponible : ${"%.2f".format(remaining)}")
                } else {
                    append("Aucune limite définie (règle-la dans l'appli)")
                }
                append(warning)
            }
        }
        refreshBudget()

        expandedLayout.visibility = View.GONE
        toggleButton.setOnClickListener {
            expandedLayout.visibility =
                if (expandedLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (expandedLayout.visibility == View.VISIBLE) refreshBudget()
        }

        closeButton.setOnClickListener {
            stopSelf()
        }
    }

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        val dragHandle = view.findViewById<View>(R.id.dragHandle)
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        super.onDestroy()
    }
}
