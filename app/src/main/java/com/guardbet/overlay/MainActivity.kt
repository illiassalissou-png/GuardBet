package com.guardbet.overlay

import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.guardbet.overlay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var budgetTracker: BudgetTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        budgetTracker = BudgetTracker(this)

        binding.editDailyLimit.setText(
            if (budgetTracker.getDailyLimit() > 0) budgetTracker.getDailyLimit().toString() else ""
        )

        binding.btnOverlayPermission.setOnClickListener {
            requestOverlayPermission()
        }

        binding.btnUsagePermission.setOnClickListener {
            requestUsageStatsPermission()
        }

        binding.btnSaveLimit.setOnClickListener {
            val value = binding.editDailyLimit.text.toString().toDoubleOrNull()
            if (value != null && value > 0) {
                budgetTracker.setDailyLimit(value)
                Toast.makeText(this, "Limite quotidienne enregistrée", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Entre un montant valide", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogSpent.setOnClickListener {
            val value = binding.editSpentAmount.text.toString().toDoubleOrNull()
            if (value != null && value > 0) {
                budgetTracker.addSpent(value)
                binding.editSpentAmount.text.clear()
                updateStatusText()
                Toast.makeText(this, "Montant ajouté à ton suivi", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStartService.setOnClickListener {
            if (!hasOverlayPermission()) {
                Toast.makeText(this, "Active d'abord la permission d'affichage", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!hasUsageStatsPermission()) {
                Toast.makeText(this, "Active d'abord la permission d'usage", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val intent = Intent(this, WatcherService::class.java)
            startForegroundService(intent)
            Toast.makeText(this, "Surveillance activée", Toast.LENGTH_SHORT).show()
        }

        binding.btnStopService.setOnClickListener {
            stopService(Intent(this, WatcherService::class.java))
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Surveillance désactivée", Toast.LENGTH_SHORT).show()
        }

        updateStatusText()
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
    }

    private fun updateStatusText() {
        binding.statusText.text = buildString {
            append("Permission overlay : ${if (hasOverlayPermission()) "✅" else "❌"}\n")
            append("Permission usage : ${if (hasUsageStatsPermission()) "✅" else "❌"}\n\n")
            append("Dépensé aujourd'hui : ${"%.2f".format(budgetTracker.getTodaySpent())}\n")
            append("Total suivi (historique) : ${"%.2f".format(budgetTracker.getLifetimeSpent())}")
        }
    }

    private fun hasOverlayPermission(): Boolean =
        Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}
