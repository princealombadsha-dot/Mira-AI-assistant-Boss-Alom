package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AuditLogEntry
import com.example.data.model.ChatMessage
import com.example.data.model.CommandAction
import com.example.data.model.CommandCategory
import com.example.data.model.Sender
import com.example.device.DeviceIntelligenceManager
import com.example.domain.CommandRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Maira Bot", appName)
  }

  @Test
  fun `test command router classifies wifi query`() {
    val cmd = CommandRouter.classify("আমার ফোনের Wi-Fi সেটিংস খুলে দাও")
    assertEquals(CommandCategory.OPEN_SETTINGS, cmd.category)
  }

  @Test
  fun `test command router classifies screen help query`() {
    val cmd = CommandRouter.classify("স্ক্রিনে দেখানো সমস্যাটা সমাধানের ধাপ বলো")
    assertEquals(CommandCategory.SCREEN_HELP, cmd.category)
  }

  @Test
  fun `test command router classifies rest mode query`() {
    val cmd = CommandRouter.classify("এখন বিশ্রাম মোড চালু করো")
    assertEquals(CommandCategory.REST_MODE, cmd.category)
  }

  @Test
  fun `test command router classifies app inventory query`() {
    val cmd = CommandRouter.classify("আমার ফোনে কী কী অ্যাপ আছে?")
    assertEquals(CommandCategory.APP_INVENTORY, cmd.category)
  }

  @Test
  fun `test command router classifies device health query`() {
    val cmd = CommandRouter.classify("আমার ফোনের স্টোরেজ কত খালি আছে?")
    assertEquals(CommandCategory.DEVICE_HEALTH, cmd.category)
  }

  @Test
  fun `test command router classifies accessibility scroll query`() {
    val cmd = CommandRouter.classify("এই পেজে নিচে স্ক্রল করো")
    assertEquals(CommandCategory.ACCESSIBILITY_ACTION, cmd.category)
    assertEquals("SCROLL_DOWN", cmd.actionTarget)
  }

  @Test
  fun `test device intelligence manager diagnostics and audit logs`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = DeviceIntelligenceManager(context)

    val report = manager.runDeviceHealthDiagnostics()
    assertNotNull(report)
    assertTrue(report.totalStorageGb >= 0.0)
    assertTrue(report.findings.isNotEmpty())

    manager.clearAuditLogs()
    assertEquals(0, manager.getAuditLogs().size)

    manager.recordAuditLog(
      AuditLogEntry(
        actionTitleBn = "টেস্ট অ্যাকশন",
        actionTarget = "Test Target",
        category = CommandCategory.DEVICE_HEALTH,
        statusTextBn = "সফল"
      )
    )
    val logs = manager.getAuditLogs()
    assertEquals(1, logs.size)
    assertEquals("টেস্ট অ্যাকশন", logs[0].actionTitleBn)

    manager.clearAuditLogs()
    assertEquals(0, manager.getAuditLogs().size)
  }

  @Test
  fun `test room voice interaction database insert and query`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.nexus.aiagent.data.NexusDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.voiceInteractionDao()
    val testInteraction = com.nexus.aiagent.data.VoiceInteractionEntity(
      spokenPrompt = "আমার ফোনের ইউটিউব চালু করো",
      aiResponse = "ইউটিউব অ্যাপ চালু করা হচ্ছে।",
      language = "bn-BD",
      status = "SUCCESS",
      executedAction = "LAUNCH_YOUTUBE"
    )

    val id = dao.insertInteraction(testInteraction)
    assertTrue(id > 0)

    val recent = dao.getRecentInteractions(5)
    assertEquals(1, recent.size)
    assertEquals("আমার ফোনের ইউটিউব চালু করো", recent[0].spokenPrompt)
    assertEquals("LAUNCH_YOUTUBE", recent[0].executedAction)

    dao.clearAll()
    assertEquals(0, dao.getRecentInteractions(5).size)
    db.close()
  }

  @Test
  fun `test chat message and command action structure for floating orb`() {
    val action = CommandAction(
      label = "ইউটিউব ওপেন",
      actionType = "OPEN_APP",
      payload = "com.google.android.youtube"
    )
    val msg = ChatMessage(
      text = "ইউটিউব অ্যাপ চালু করা হচ্ছে",
      sender = Sender.ASSISTANT,
      suggestedActions = listOf(action)
    )
    assertEquals(Sender.ASSISTANT, msg.sender)
    assertEquals(1, msg.suggestedActions.size)
    assertEquals("OPEN_APP", msg.suggestedActions[0].actionType)
  }
}
