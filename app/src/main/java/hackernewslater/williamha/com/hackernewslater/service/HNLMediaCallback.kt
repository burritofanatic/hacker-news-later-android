package hackernewslater.williamha.com.hackernewslater.service

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

/**
 * Created by williamha on 9/8/18.
 */
class HNLMediaCallback(val context: Context, val mediaSession: MediaSessionCompat,
                       var mediaPlayer: MediaPlayer? = null): MediaSessionCompat.Callback() {

    private var mediaUri: Uri? = null
    private var newMedia: Boolean = false
    private var mediaExtras: Bundle? = null

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        println("Playing ${uri.toString()}")

        if (mediaUri == uri) {
            newMedia = false
            mediaExtras = null
        } else {
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()
    }

    override fun onPlay() {
        super.onPlay()

        if (ensureAudioFocus()) {
            mediaSession.isActive = true
            initializeMediaPlayer()
            prepareMedia {
                startPlaying()
            }

        }

        initializeMediaPlayer()
        prepareMedia {
            startPlaying()
        }
    }

    override fun onStop() {
        super.onStop()

        stopPlaying()
    }

    override fun onPause() {
        super.onPause()

        pausePlaying()
    }

    private fun setNewMedia(uri: Uri?) {
        newMedia = true
        mediaUri = uri
    }

    private fun ensureAudioFocus(): Boolean {
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun removeAudioFocus() {
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setOnCompletionListener({
                setState(PlaybackStateCompat.STATE_PAUSED)
            })
        }
    }

    private fun prepareMedia(callback:() -> Unit) {
        if (newMedia) {
            newMedia = false

            mediaPlayer?.let { mediaPlayer ->
                mediaUri?.let {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(context, mediaUri)
                    mediaSession.setMetadata(MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
                            .build())

                    mediaPlayer.setOnPreparedListener {
                        callback()
                    }

                    mediaPlayer.prepareAsync()
                }
            }
        }
    }

    private fun startPlaying() {
        mediaPlayer?.let { mediaPlayer ->
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun pausePlaying() {
        removeAudioFocus()
        mediaPlayer?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun stopPlaying() {
        removeAudioFocus()
        mediaSession.isActive = false
        mediaPlayer?.let {  mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }
    }

    private fun setState(state: Int) {
        var position: Long = -1

        mediaPlayer?.let {
            position = it.currentPosition.toLong()
        }

        val playbackState = PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_STOP or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_PAUSE)
                .setState(state, position, 1.0f)
                .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun displayNotification() {
        if (mediaSession.controller.metadata == null) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            createNotification
        }
    }
}