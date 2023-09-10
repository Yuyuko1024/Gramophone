package org.akanework.gramophone.logic.services

import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.akanework.gramophone.Constants
import org.akanework.gramophone.MainActivity
import org.akanework.gramophone.R

/**
 * [GramophonePlaybackService] is a server service.
 * It's using exoplayer2 as its player backend.
 */
@UnstableApi
class GramophonePlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var customCommands: List<CommandButton>

    var timer: CountDownTimer? = null
    var timerDuration = 0

    override fun onCreate() {

        customCommands =
            listOf(
                getShuffleCommandButton(
                    SessionCommand(Constants.PLAYBACK_SHUFFLE_ACTION_ON, Bundle.EMPTY)
                ),
                getShuffleCommandButton(
                    SessionCommand(Constants.PLAYBACK_SHUFFLE_ACTION_OFF, Bundle.EMPTY)
                ),
                getCloseButton(
                    SessionCommand(Constants.PLAYBACK_CLOSE, Bundle.EMPTY)
                )
            )

        // Create an exoplayer2 instance here for server side.
        val player = ExoPlayer.Builder(this).build()

        val callback = CustomMediaSessionCallback()
        val notificationProvider = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)
        notificationProvider.setSmallIcon(R.drawable.ic_gramophone)
        // Create a mediaSession here so we can connect to our
        // client later.
        mediaSession =
            MediaSession
                .Builder(this, player)
                .setCallback(callback)
                .setCustomLayout(ImmutableList.of(customCommands[0], customCommands[2]))
                .setBitmapLoader(CacheBitmapLoader(DataSourceBitmapLoader(/* context= */ this)))
                .setSessionActivity(
                    getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
                    ),
                ).build()

        // Set AudioAttributes here so media3 can manage audio
        // focus correctly.
        val audioAttributes: AudioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
        player.setAudioAttributes(audioAttributes, true)

        super.onCreate()
    }

    private fun getShuffleCommandButton(sessionCommand: SessionCommand): CommandButton {
        val isOn = sessionCommand.customAction == Constants.PLAYBACK_SHUFFLE_ACTION_ON
        return CommandButton.Builder()
            .setDisplayName(getString(R.string.shuffle))
            .setSessionCommand(sessionCommand)
            .setIconResId(if (isOn) R.drawable.ic_shuffle else R.drawable.ic_shuffle_on)
            .build()
    }

    private fun getCloseButton(sessionCommand: SessionCommand): CommandButton =
        CommandButton.Builder()
            .setDisplayName(getString(R.string.close))
            .setSessionCommand(sessionCommand)
            .setIconResId(R.drawable.ic_close)
            .build()

    override fun onDestroy() {
        // When destroying, we should release server side player
        // alongside with the mediaSession.
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        super.onDestroy()
    }

    // This onGetSession is a necessary method override needed by
    // MediaSessionService.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private inner class CustomMediaSessionCallback: MediaSession.Callback {
        // Configure commands available to the controller in onConnect()
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val availableSessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            for (commandButton in customCommands) {
                // Add custom command to available session commands.
                commandButton.sessionCommand?.let { availableSessionCommands.add(it) }
            }
            availableSessionCommands.add(SessionCommand(Constants.SERVICE_ADD_TIMER, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(Constants.SERVICE_QUERY_TIMER, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(Constants.SERVICE_CLEAR_TIMER, Bundle.EMPTY))
            availableSessionCommands.add(SessionCommand(Constants.SERVICE_IS_STOPPED_BY_TIMER, Bundle.EMPTY))
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands.build())
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            Log.d("PlaybackService", "onCustomCommand")
            val sessionResult: SessionResult
            when (customCommand.customAction) {
                Constants.PLAYBACK_SHUFFLE_ACTION_ON -> {
                    // Enable shuffling.
                    session.player.shuffleModeEnabled = true
                    // Change the custom layout to contain the `Disable shuffling` command.
                    session.setCustomLayout(ImmutableList.of(customCommands[1], customCommands[2]))
                    sessionResult = SessionResult(SessionResult.RESULT_SUCCESS)
                }
                Constants.PLAYBACK_SHUFFLE_ACTION_OFF -> {
                    // Disable shuffling.
                    session.player.shuffleModeEnabled = false
                    // Change the custom layout to contain the `Enable shuffling` command.
                    session.setCustomLayout(ImmutableList.of(customCommands[0], customCommands[2]))
                    sessionResult = SessionResult(SessionResult.RESULT_SUCCESS)
                }
                Constants.PLAYBACK_CLOSE -> {
                    session.player.clearMediaItems()
                    sessionResult = SessionResult(SessionResult.RESULT_SUCCESS)
                }
                Constants.SERVICE_ADD_TIMER -> {
                    Log.d("PlaybackService", "SERVICE_ADD_TIMER")
                    sessionResult = SessionResult(SessionResult.RESULT_SUCCESS)
                    setUpTimer(customCommand.customExtras.getInt("duration"))
                }
                Constants.SERVICE_CLEAR_TIMER -> {
                    Log.d("PlaybackService", "SERVICE_CLEAR_TIMER")
                    sessionResult = SessionResult(SessionResult.RESULT_SUCCESS)
                    timer?.cancel()
                    timer = null
                    timerDuration = 0
                }
                Constants.SERVICE_QUERY_TIMER -> {
                    Log.d("PlaybackService", "SERVICE_QUERY_TIMER")
                    sessionResult = SessionResult(SessionResult.RESULT_SUCCESS)
                    sessionResult.extras.putInt("duration", timerDuration)
                } else -> {
                    sessionResult = SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                }
            }
            return Futures.immediateFuture(sessionResult)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return SettableFuture.create()
        }
    }

    private fun setUpTimer(destinationTime: Int) {
        timerDuration = destinationTime
        timer =
            object : CountDownTimer(destinationTime.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    androidx.media3.common.util.Log.d("TAG", "TICK, $millisUntilFinished")
                }

                override fun onFinish() {
                    mediaSession?.player?.pause()
                    timerDuration = 0
                    timer = null

                    mediaSession?.connectedControllers?.forEach {
                        mediaSession!!.sendCustomCommand(it,
                            SessionCommand(Constants.SERVICE_IS_STOPPED_BY_TIMER, Bundle.EMPTY),
                            Bundle.EMPTY
                        )
                    }
                }
            }
        timer!!.start()
    }
}
