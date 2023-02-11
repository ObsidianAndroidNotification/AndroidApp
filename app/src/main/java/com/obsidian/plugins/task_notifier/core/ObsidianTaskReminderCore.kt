package com.obsidian.plugins.task_notifier.core

import android.content.Context
import android.net.Uri
import com.obsidian.plugins.task_notifier.os.AlertManager
import com.obsidian.plugins.task_notifier.os.NotificationManager
import com.obsidian.plugins.task_notifier.os.PersistenceManager
import com.obsidian.plugins.task_notifier.os.ServiceManager
import com.obsidian.plugins.task_notifier.plugin.ObsidianPluginManager
import com.obsidian.plugins.task_notifier.utils.Logger

class ObsidianTaskReminderCore {
    companion object {
        @JvmStatic
        fun init(context: Context) {
            Logger.info("ObsidianTaskReminderCore.init")
            val folders = PersistenceManager.getWatchedFolders(context);
            ServiceManager.ensureAllPathsAreWatched(context, folders);
        }

        @JvmStatic
        fun onFileChanged(context: Context, uri: Uri, content: String): OnFileChangedResult {
            Logger.info("ObsidianTaskReminderCore.onFileChanged path: ${uri.path} content: $content")
            NotificationManager.notify(context, "file changed at", "path ${uri.path}")
            val folders = PersistenceManager.getWatchedFolders(context);
            /*if (folders.contains(uri.path)) {
                return OnFileChangedResult.ACK;
            } else {
                return OnFileChangedResult.STOP_LISTENING;
            }*/
            val reminders = ObsidianPluginManager.processFile(content)
            AlertManager().syncNotifications(context, reminders);
            return OnFileChangedResult.ACK;
        }

        @JvmStatic
        fun onWatchedPathAdded(context: Context, uri: Uri) {
            Logger.info("ObsidianTaskReminderCore.onWatchedPathAdded")
            if (!ObsidianPluginManager.isInterestedFile(uri)) return;

            NotificationManager.notify(context, "folder added", "path ${uri.path}")
            PersistenceManager.addWatchedFolder(uri, context)
            val folders = PersistenceManager.getWatchedFolders(context);
            ServiceManager.ensureAllPathsAreWatched(context, folders)
        }
    }
}

enum class OnFileChangedResult {
    ACK,
    STOP_LISTENING,
}
