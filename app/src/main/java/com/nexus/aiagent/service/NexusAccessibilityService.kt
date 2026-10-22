package com.nexus.aiagent.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Nexus Accessibility Service for real OS-level automation, UI tree inspection,
 * gesture dispatch, clicking buttons, and text injection into active third-party apps.
 */
class NexusAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: NexusAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i("NexusAccessibility", "NexusAccessibilityService connected and ready for OS automation.")

        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitors screen updates for AI context awareness
        if (event == null) return
    }

    override fun onInterrupt() {
        Log.w("NexusAccessibility", "NexusAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun performGlobalActionHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performGlobalActionBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performGlobalActionRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openAppSettings(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("NexusAccessibility", "Failed to launch package: $packageName", e)
            false
        }
    }

    fun clickNodeByText(text: String): Boolean {
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow ?: return false
        return findAndClickNode(rootNode, text)
    }

    fun injectTextIntoFocused(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)

        if (focused != null && focused.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    fun extractScreenVisibleText(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        collectVisibleText(root, sb)
        return sb.toString().trim()
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null || !node.isVisibleToUser) return
        node.text?.let {
            if (it.isNotBlank()) sb.append(it).append(" | ")
        }
        node.contentDescription?.let {
            if (it.isNotBlank() && it != node.text) sb.append("[desc: ").append(it).append("] | ")
        }
        for (i in 0 until node.childCount) {
            collectVisibleText(node.getChild(i), sb)
        }
    }

    private fun findAndClickNode(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false

        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        if ((nodeText.contains(text, ignoreCase = true) || contentDesc.contains(text, ignoreCase = true)) &&
            (node.isClickable || node.isEnabled)
        ) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (findAndClickNode(child, text)) {
                return true
            }
        }
        return false
    }
}
