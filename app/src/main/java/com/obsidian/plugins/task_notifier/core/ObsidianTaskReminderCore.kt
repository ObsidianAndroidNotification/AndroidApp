package com.obsidian.plugins.task_notifier.core

import android.content.Context
import android.net.Uri
import com.obsidian.plugins.task_notifier.core.bo.ObsidianActiveReminderBO
import com.obsidian.plugins.task_notifier.os.AlertManager
import com.obsidian.plugins.task_notifier.os.NotificationManager
import com.obsidian.plugins.task_notifier.os.PersistenceManager
import com.obsidian.plugins.task_notifier.os.ServiceManager
import com.obsidian.plugins.task_notifier.plugin.ObsidianPluginManager
import com.obsidian.plugins.task_notifier.utils.Logger
import com.obsidian.plugins.task_notifier.utils.ScopeEnum

class ObsidianTaskReminderCore {
  companion object {
    @JvmStatic
    fun init(context: Context) {
      Logger.info("ObsidianTaskReminderCore.init")
      PersistenceManager.init(context)
      val folders = PersistenceManager.getWatchedFolders(context)
      ServiceManager.ensureAllPathsAreWatched(context, folders)
    }

    @JvmStatic
    fun onFileChanged(context: Context, uri: Uri, content: String): OnFileChangedResult {
      Logger.info("ObsidianTaskReminderCore.onFileChanged path: ${uri.path} content: $content")
      NotificationManager.notify(
        context,
        "file changed at",
        "path ${uri.path}",
        1,
        ScopeEnum.APPLICATION
      )

      val folders = PersistenceManager.getWatchedFolders(context)
      if (!folders.contains(uri.toString())) return OnFileChangedResult.STOP_LISTENING
      try {
        val reminders = ObsidianPluginManager.processFile(content)
        val remindersWithReqIds =
          AlertManager().syncNotificationsAndAssignReqIds(context, reminders)
        PersistenceManager.setActiveReminders(context, remindersWithReqIds)
      } catch (e: Exception) {
        Logger.info("Failed to process reminders. Error: ${e.message} ${e.cause}")
      }
      return OnFileChangedResult.ACK
    }

    @JvmStatic
    fun onWatchedPathAdded(context: Context, uri: Uri) {
      Logger.info("ObsidianTaskReminderCore.onWatchedPathAdded")
      if (!ObsidianPluginManager.isInterestedFile(uri)) return

      NotificationManager.notify(context, "folder added", "path ${uri.path}")
      PersistenceManager.addWatchedFolder(uri, context)
      val folders = PersistenceManager.getWatchedFolders(context)
      ServiceManager.ensureAllPathsAreWatched(context, folders)
    }

    @JvmStatic
    fun removeFolder(context: Context, folderPath: String) {
      Logger.info("Removing watched folder ${folderPath}")
      PersistenceManager.removeWatchedFolder(folderPath, context)
      val folders = PersistenceManager.getWatchedFolders(context)
      ServiceManager.ensureAllPathsAreWatched(context, folders)
    }

    @JvmStatic
    fun removeActiveReminder(context: Context, reminder: ObsidianActiveReminderBO) {
      PersistenceManager.removeActiveReminder(context, reminder.reqId!!)
      val activeReminders = PersistenceManager.getActiveReminders(context)
      val remindersWithReqIds =
        AlertManager().syncNotificationsAndAssignReqIds(context, activeReminders)
      PersistenceManager.setActiveReminders(context, remindersWithReqIds)
    }
  }
}

enum class OnFileChangedResult {
  ACK,
  STOP_LISTENING,
}
