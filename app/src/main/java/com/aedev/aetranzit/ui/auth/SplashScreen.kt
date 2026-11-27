package com.aedev.aetranzit.ui.auth

import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aedev.aetranzit.R
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.ui.components.AeIsometricBackground
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.TicketPaperBackground

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onAuthenticated: (User) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (val current = destination) {
            SplashDestination.Loading -> Unit
            SplashDestination.Onboarding -> onNavigateToOnboarding()
            SplashDestination.Login -> onNavigateToLogin()
            is SplashDestination.Authenticated -> onAuthenticated(current.user)
        }
    }

    SplashContent()
}

@Composable
fun SplashContent(modifier: Modifier = Modifier) {
    val isInspectionMode = LocalInspectionMode.current

    AeIsometricBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AeDimens.SpaceLg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(AeDimens.SplashVideoSize),
                shape = MaterialTheme.shapes.extraLarge,
                color = TicketPaperBackground,
                shadowElevation = AeDimens.ElevatedCardElevation,
                border = BorderStroke(AeDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (isInspectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AeDimens.SpaceLg),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "AeTranzit Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    SplashAnimatedLogoPlayer(modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(modifier = Modifier.height(AeDimens.SpaceLg))

            Text(
                text = "AeTranzit",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AeDimens.SpaceXs))

            Text(
                text = "Offline-First Bus Ticketing & Fleet Operations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SplashAnimatedLogoPlayer(modifier: Modifier = Modifier) {
    val playerHolder = remember { SplashMediaPlayerHolder() }

    DisposableEffect(playerHolder) {
        onDispose {
            playerHolder.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextureView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isOpaque = false
                alpha = 0f
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surfaceTexture: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        playerHolder.attachAndStart(
                            textureView = this@apply,
                            surfaceTexture = surfaceTexture,
                            viewWidth = width,
                            viewHeight = height
                        )
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surfaceTexture: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        playerHolder.updateViewport(
                            textureView = this@apply,
                            viewWidth = width,
                            viewHeight = height
                        )
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        playerHolder.release()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                        if (alpha < 1f) {
                            alpha = 1f
                        }
                    }
                }
            }
        },
        onRelease = { view ->
            view.surfaceTextureListener = null
            playerHolder.release()
        }
    )
}

private class SplashMediaPlayerHolder {
    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null

    fun attachAndStart(
        textureView: TextureView,
        surfaceTexture: SurfaceTexture,
        viewWidth: Int,
        viewHeight: Int
    ) {
        release()
        val renderSurface = Surface(surfaceTexture)
        surface = renderSurface

        runCatching {
            val player = MediaPlayer()
            mediaPlayer = player
            val afd = textureView.context.resources.openRawResourceFd(R.raw.splash_logo)
                ?: run {
                    release()
                    return
                }
            afd.use { descriptor ->
                player.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
            player.setSurface(renderSurface)
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                applyCenterCropTransform(
                    textureView = textureView,
                    viewWidth = textureView.width.takeIf { it > 0 } ?: viewWidth,
                    viewHeight = textureView.height.takeIf { it > 0 } ?: viewHeight,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight
                )
            }
            player.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    textureView.alpha = 1f
                }
                false
            }
            player.setOnPreparedListener { preparedPlayer ->
                applyCenterCropTransform(
                    textureView = textureView,
                    viewWidth = textureView.width.takeIf { it > 0 } ?: viewWidth,
                    viewHeight = textureView.height.takeIf { it > 0 } ?: viewHeight,
                    videoWidth = preparedPlayer.videoWidth,
                    videoHeight = preparedPlayer.videoHeight
                )
                preparedPlayer.start()
            }
            player.prepareAsync()
        }.onFailure {
            release()
        }
    }

    fun updateViewport(textureView: TextureView, viewWidth: Int, viewHeight: Int) {
        val player = mediaPlayer ?: return
        runCatching {
            applyCenterCropTransform(
                textureView = textureView,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                videoWidth = player.videoWidth,
                videoHeight = player.videoHeight
            )
        }
    }

    fun release() {
        val playerToRelease = mediaPlayer
        mediaPlayer = null
        if (playerToRelease != null) {
            runCatching {
                playerToRelease.setOnPreparedListener(null)
                playerToRelease.setOnVideoSizeChangedListener(null)
                playerToRelease.setOnInfoListener(null)
                if (playerToRelease.isPlaying) {
                    playerToRelease.stop()
                }
            }
            runCatching { playerToRelease.reset() }
            runCatching { playerToRelease.release() }
        }

        val surfaceToRelease = surface
        surface = null
        if (surfaceToRelease != null) {
            runCatching { surfaceToRelease.release() }
        }
    }

    private fun applyCenterCropTransform(
        textureView: TextureView,
        viewWidth: Int,
        viewHeight: Int,
        videoWidth: Int,
        videoHeight: Int
    ) {
        if ((viewWidth <= 0) || (viewHeight <= 0) || (videoWidth <= 0) || (videoHeight <= 0)) return
        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val scaleX: Float
        val scaleY: Float
        if (videoRatio > viewRatio) {
            scaleX = videoRatio / viewRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = viewRatio / videoRatio
        }
        val matrix = Matrix().apply {
            setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        }
        textureView.setTransform(matrix)
    }
}

@Preview(name = "Splash Light", showBackground = true)
@Preview(
    name = "Splash Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun SplashContentPreview() {
    AeTranzitTheme {
        SplashContent()
    }
}
