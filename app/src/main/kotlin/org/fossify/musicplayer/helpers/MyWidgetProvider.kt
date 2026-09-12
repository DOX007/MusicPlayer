package org.fossify.musicplayer.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.widget.RemoteViews
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getColoredBitmap
import org.fossify.commons.extensions.getLaunchIntent
import org.fossify.musicplayer.R
import org.fossify.musicplayer.activities.SplashActivity
import org.fossify.musicplayer.extensions.config
import org.fossify.musicplayer.extensions.maybePreparePlayer
import org.fossify.musicplayer.extensions.togglePlayback
import org.fossify.musicplayer.playback.PlaybackService

class MyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val PREVIOUS = "previous"
        const val PLAYPAUSE = "playpause"
        const val NEXT = "next"
        const val SEEK_BACK = "seek_back"
        const val SEEK_FORWARD = "seek_forward"
        const val TRACK_STATE_CHANGED = "track_state_changed"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        performUpdate(context)
    }

    override fun onEnabled(context: Context) = triggerUpdate(context)

    override fun onReceive(context: Context, intent: Intent) {
        when (val action = intent.action) {
            TRACK_STATE_CHANGED -> performUpdate(context)
            PREVIOUS, PLAYPAUSE, NEXT, SEEK_BACK, SEEK_FORWARD ->
                handlePlayerControls(context, action)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        newOptions: Bundle
    ) = triggerUpdate(context)

    private fun performUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach { widgetId ->
            val views = getRemoteViews(appWidgetManager, context, widgetId)
            updateColors(context, views)
            setupButtons(context, views)
            updateSongInfo(views, PlaybackService.currentMediaItem?.mediaMetadata)
            updatePlayPauseButton(context, views, PlaybackService.isPlaying)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun handlePlayerControls(context: Context, action: String) {
        maybePreparePlayer(context) { player, _ ->
            if (player.currentMediaItem == null) {
                val intent = context.getLaunchIntent()
                    ?: Intent(context, SplashActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                when (action) {
                    NEXT -> player.seekToNext()
                    PREVIOUS -> player.seekToPrevious()
                    PLAYPAUSE -> player.togglePlayback()
                    SEEK_BACK ->
                        player.seekTo(
                            (player.currentPosition - 60000L).coerceAtLeast(0L)
                        )
                    SEEK_FORWARD ->
                        player.seekTo(
                            (player.currentPosition + 60000L)
                                .coerceAtMost(player.duration.coerceAtLeast(0L))
                        )
                }
            }
        }
    }

    private fun maybePreparePlayer(
        context: Context,
        callback: (player: MediaController, prepared: Boolean) -> Unit
    ) {
        SimpleMediaController.getInstance(context).withController {
            maybePreparePlayer(context) { success ->
                callback(this, success)
            }
        }
    }

    private fun triggerUpdate(context: Context) {
        performUpdate(context)
        maybePreparePlayer(context) { _, success ->
            if (success) {
                performUpdate(context)
            }
        }
    }

    private fun setupIntent(
        context: Context,
        views: RemoteViews,
        action: String,
        id: Int
    ) {
        val intent = Intent(context, MyWidgetProvider::class.java).apply {
            this.action = action
        }
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(id, pendingIntent)
    }

    private fun setupAppOpenIntent(context: Context, views: RemoteViews, id: Int) {
        val intent =
            context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(id, pendingIntent)
    }

    private fun updateSongInfo(views: RemoteViews, currSong: MediaMetadata?) {
        if (currSong != null) {
            views.setTextViewText(R.id.song_info_title, currSong.title)
        } else {
            views.setTextViewText(R.id.song_info_title, "")
        }
    }

    private fun updatePlayPauseButton(
        context: Context,
        views: RemoteViews,
        isPlaying: Boolean
    ) {
        val drawableId =
            if (isPlaying)
                org.fossify.commons.R.drawable.ic_pause_vector
            else
                org.fossify.commons.R.drawable.ic_play_vector

        val icon =
            context.resources.getColoredBitmap(
                drawableId,
                context.config.widgetTextColor
            )

        views.setImageViewBitmap(R.id.play_pause_btn, icon)
    }

    private fun updateColors(context: Context, views: RemoteViews) {
        val widgetTextColor = context.config.widgetTextColor

        views.apply {
            applyColorFilter(R.id.widget_background, context.config.widgetBgColor)
            setTextColor(R.id.song_info_title, widgetTextColor)

            setImageViewBitmap(
                R.id.previous_btn,
                context.resources.getColoredBitmap(
                    org.fossify.commons.R.drawable.ic_previous_vector,
                    widgetTextColor
                )
            )
            setImageViewBitmap(
                R.id.next_btn,
                context.resources.getColoredBitmap(
                    org.fossify.commons.R.drawable.ic_next_vector,
                    widgetTextColor
                )
            )
            setImageViewBitmap(
                R.id.seek_back_btn,
                context.resources.getColoredBitmap(
                    R.drawable.ic_rewind_double,
                    widgetTextColor
                )
            )
            setImageViewBitmap(
                R.id.seek_forward_btn,
                context.resources.getColoredBitmap(
                    R.drawable.ic_forward_double,
                    widgetTextColor
                )
            )
        }
    }

    private fun setupButtons(context: Context, views: RemoteViews) {
        setupIntent(context, views, PREVIOUS, R.id.previous_btn)
        setupIntent(context, views, PLAYPAUSE, R.id.play_pause_btn)
        setupIntent(context, views, NEXT, R.id.next_btn)
        setupIntent(context, views, SEEK_BACK, R.id.seek_back_btn)
        setupIntent(context, views, SEEK_FORWARD, R.id.seek_forward_btn)

        setupAppOpenIntent(context, views, R.id.song_info_title)
    }

    private fun getRemoteViews(
        appWidgetManager: AppWidgetManager,
        context: Context,
        widgetId: Int
    ): RemoteViews {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        if (
            widgetId == context.config.widgetIdToMeasure &&
            context.config.initialWidgetHeight == 0
        ) {
            context.config.initialWidgetHeight = minHeight
        }

        val layoutId =
            if (minHeight < context.config.initialWidgetHeight / 2)
                R.layout.small_widget
            else
                R.layout.widget

        return RemoteViews(context.packageName, layoutId)
    }

    private fun getComponentName(context: Context) =
        ComponentName(context, MyWidgetProvider::class.java)
}
