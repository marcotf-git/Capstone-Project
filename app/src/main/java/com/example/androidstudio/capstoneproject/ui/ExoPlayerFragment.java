package com.example.androidstudio.capstoneproject.ui;


import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

/**
 * This fragment will play the video with Url in mediaUrl variable, using the ExoPlayer library.
 * The mediaUrl can be set by a method setMediaUrl(String url), that can be called from
 * the activity. The class will retain the status while rotating the device, and uses MediaSession
 * in sync with the Player, to receive commands from external clients. like headphones.
 */
public class ExoPlayerFragment extends Fragment {

    private static final String TAG = ExoPlayerFragment.class.getSimpleName();

    private static final DefaultBandwidthMeter BANDWIDTH_METER =
            new DefaultBandwidthMeter();

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow;
    private long playbackPosition;
    private ComponentListener componentListener;
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private boolean isLandscape;

    private TextView errorMessageView;
    private FrameLayout videoView;

    // This variable has a setter method and it is for initializing the fragment
    private String mediaUrl;


    public ExoPlayerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // These views will handle the error in ExoPlayer loading.
        // They will change visibility, according to the loading status, as commanded by the
        // onPlayerError method of the ExoPlayer callback listener.
        if (getActivity() != null) {
            errorMessageView = getActivity().findViewById(R.id.tv_illustration_not_available_label);
            videoView = getActivity().findViewById(R.id.player_container);
        }

        componentListener = new ComponentListener();

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            playWhenReady = savedInstanceState.getBoolean("playWhenReady");
            currentWindow = savedInstanceState.getInt("currentWindow");
            playbackPosition = savedInstanceState.getLong("playbackPosition");
            mediaUrl = savedInstanceState.getString("mediaUrl");

            Log.v(TAG, "onCreate playbackPosition:" + playbackPosition);
        }

        // Initialize the Media Session.
        initializeMediaSession();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        playerView = rootView.findViewById(R.id.video_view);

        // this view is the same in both layouts, but receives this name in the landscape
        // this helps identify the device position
        isLandscape = null != getActivity().findViewById(R.id.view_activity_part_detail_landscape);

        Log.v(TAG, "onCreateView isLandscape:" + isLandscape);

        return rootView;

    }


    private void initializePlayer() {

        // Use an adaptive track selection
        TrackSelection.Factory adaptiveTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);

        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(getContext()),
                new DefaultTrackSelector(adaptiveTrackSelectionFactory),
                new DefaultLoadControl());

        playerView.setPlayer(player);

        // Register an Player.DefaultEventListener
        player.addListener(componentListener);

        Uri uri = Uri.parse(mediaUrl);
        //Uri uri = Uri.fromFile(new File(mediaUrl));
        MediaSource mediaSource = buildMediaSource(uri);
        player.prepare(mediaSource, true, false);

        player.seekTo(currentWindow, playbackPosition);

        player.setPlayWhenReady(playWhenReady);

    }


    private MediaSource buildMediaSource(Uri uri) {

        String userAgent = Util.getUserAgent(getContext(), "BakingApp");

//        return new ExtractorMediaSource.Factory(
//                new DefaultHttpDataSourceFactory(userAgent))
//                .createMediaSource(uri);

                return new ExtractorMediaSource.Factory(
                new DefaultDataSourceFactory(getContext(), userAgent))
                .createMediaSource(uri);


    }


    /**
     * Initializes the Media Session to be enabled with media buttons, transport controls, callbacks
     * and media controller.
     */
    private void initializeMediaSession() {

        // Create a MediaSessionCompat.
        mMediaSession = new MediaSessionCompat(getContext(), TAG);

        // Enable callbacks from MediaButtons and TransportControls.
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Do not let MediaButtons restart the player when the app is not visible.
        mMediaSession.setMediaButtonReceiver(null);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player.
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);

        mMediaSession.setPlaybackState(mStateBuilder.build());

        // MySessionCallback has methods that handle callbacks from a media controller.
        mMediaSession.setCallback(new MySessionCallback());

        // Start the Media Session since the activity is active.
        mMediaSession.setActive(true);

    }


    /**
     * Media Session Callbacks, where all external clients control the player.
     */
    private class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            player.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            player.setPlayWhenReady(false);
        }

        @Override
        public void onSkipToPrevious() {
            player.seekTo(0);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.v(TAG, "onResume isLandscape:" + isLandscape);

        if(isLandscape) {
            hideSystemUi();
        }

        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
            mMediaSession.setActive(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
            mMediaSession.setActive(false);
        }
    }

    private void releasePlayer() {

        if (player != null) {

            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();

            player.removeListener(componentListener);
            player.stop();
            player.release();
            player = null;
        }
    }


    // It will save the principal variables when the device is rotated
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {

        if (player != null) {

            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();

            outState.putLong("playbackPosition", playbackPosition);
            outState.putInt("currentWindow", currentWindow);
            outState.putBoolean("playWhenReady", playWhenReady);
            outState.putString("mediaUrl", mediaUrl);
        }

        super.onSaveInstanceState(outState);
    }

    // The Player listener
    private class ComponentListener extends Player.DefaultEventListener{

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            super.onPlayerError(error);

            if (videoView != null && errorMessageView != null) {
                videoView.setVisibility(View.GONE);
                errorMessageView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);

            String stateString;
            switch (playbackState) {
                case Player.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case Player.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;
                case Player.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    break;
                case Player.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";
                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }

            Log.d(TAG, "changed state to " + stateString
                    + " playWhenReady: " + playWhenReady);

            // Sync with MediaSession
            if((playbackState == Player.STATE_READY) && playWhenReady){
                mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                        player.getCurrentPosition(), 1f);
            } else if((playbackState == Player.STATE_READY)){
                mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                        player.getCurrentPosition(), 1f);
            }

            mMediaSession.setPlaybackState(mStateBuilder.build());

        }
    }


    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

}
