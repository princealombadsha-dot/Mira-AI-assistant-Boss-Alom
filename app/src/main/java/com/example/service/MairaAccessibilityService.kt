package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.AccessibilityNodeSummary

class MairaAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MairaAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null

        fun clickTarget(textOrId: String): Boolean {
            return instance?.performClickByTextOrId(textOrId) ?: false
        }

        fun scroll(forward: Boolean): Boolean {
            return instance?.performScrollAction(forward) ?: false
        }

        fun navigateBack(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        }

        fun navigateHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        fun readScreen(): String {
            return instance?.extractVisibleScreenText() ?: "কোনো সক্রিয় স্ক্রিন টেক্সট পাওয়া যায়নি।"
        }

        fun getInteractiveNodes(): List<AccessibilityNodeSummary> {
            return instance?.collectInteractiveNodes() ?: emptyList()
        }

        fun injectText(text: String): Boolean {
            return instance?.performTextInjection(text) ?: false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing if needed for reactive tracking
    }

    override fun onInterrupt() {
        // Interrupted by system
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun performClickByTextOrId(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val targetLower = target.lowercase().trim()

        // 1. Try finding by text
        val nodesByText = root.findAccessibilityNodeInfosByText(target)
        for (node in nodesByText) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            // Check parent if child node isn't clickable
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
            }
        }

        // 2. Traversal fallback
        return traverseAndClick(root, targetLower)
    }

    private fun traverseAndClick(node: AccessibilityNodeInfo?, target: String): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if ((text.contains(target) || desc.contains(target) || viewId.contains(target)) && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (traverseAndClick(child, target)) {
                return true
            }
        }
        return false
    }

    fun performScrollAction(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return findAndPerformScroll(root, action)
    }

    private fun findAndPerformScroll(node: AccessibilityNodeInfo?, action: Int): Boolean {
        if (node == null) return false
        if (node.isScrollable && node.performAction(action)) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (findAndPerformScroll(child, action)) {
                return true
            }
        }
        return false
    }

    fun extractVisibleScreenText(): String {
        val root = rootInActiveWindow ?: return "বর্তমান স্ক্রিনে কোনো উপাদান দেখা যাচ্ছে না।"
        val builder = StringBuilder()
        collectText(root, builder)
        val text = builder.toString().trim()
        return if (text.isBlank()) "স্ক্রিনে কোনো দৃশ্যমান টেক্সট সনাক্ত হয়নি।" else text
    }

    private fun collectText(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            builder.append(text).append("\n")
        } else if (!desc.isNullOrBlank()) {
            builder.append(desc).append("\n")
        }

        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), builder)
        }
    }

    fun collectInteractiveNodes(): List<AccessibilityNodeSummary> {
        val root = rootInActiveWindow ?: return emptyList()
        val list = mutableListOf<AccessibilityNodeSummary>()
        traverseNodes(root, list)
        return list
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeSummary>) {
        if (node == null) return

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val isClickable = node.isClickable
        val isScrollable = node.isScrollable
        val isEditable = node.isEditable

        if (isClickable || isScrollable || isEditable || text.isNotBlank() || desc.isNotBlank()) {
            list.add(
                AccessibilityNodeSummary(
                    text = text,
                    contentDescription = desc,
                    viewIdResourceName = node.viewIdResourceName,
                    className = node.className?.toString() ?: "",
                    isClickable = isClickable,
                    isScrollable = isScrollable,
                    isEditable = isEditable
                )
            )
        }

        for (i in 0 until node.childCount) {
            traverseNodes(node.getChild(i), list)
        }
    }

    fun performTextInjection(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // Fallback: search for first editable node
        return findFirstEditableAndInject(root, text)
    }

    private fun findFirstEditableAndInject(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            if (findFirstEditableAndInject(node.getChild(i), text)) {
                return true
            }
        }
        return false
    }
}

