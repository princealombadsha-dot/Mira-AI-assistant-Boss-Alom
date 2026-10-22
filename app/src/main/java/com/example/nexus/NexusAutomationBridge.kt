package com.example.nexus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.example.service.MairaAccessibilityService

object NexusAutomationBridge {

    fun copyToClipboard(context: Context, text: String, label: String = "BD-NEXUS Prompt"): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getClipboardText(context: Context): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val item = clipboard.primaryClip?.getItemAt(0)
            item?.text?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun injectTextIntoActiveApp(text: String): Boolean {
        return MairaAccessibilityService.injectText(text)
    }

    fun clickTarget(target: String): Boolean {
        return MairaAccessibilityService.clickTarget(target)
    }

    fun scroll(forward: Boolean): Boolean {
        return MairaAccessibilityService.scroll(forward)
    }

    fun navigateBack(): Boolean {
        return MairaAccessibilityService.navigateBack()
    }
}
