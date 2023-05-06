package com.anmvg.celi;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.anmvg.celi.databinding.FragmentPlayerBinding;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;


public class PlayerFragment extends Fragment {

    private FragmentPlayerBinding binding;
    private TextView currentTime, totalTime;
    private SeekBar playerStatus;
    private ImageButton playPauseButton;
    private LibVLC libVlc;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private final Handler handler = new Handler();
    private String musicName;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);

        currentTime = binding.currentTime;
        totalTime = binding.totalTime;
        playerStatus = binding.playerStatus;
        playPauseButton = binding.playPauseButton;


        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        args.add("--file-caching=2000");
        args.add("--rtsp-tcp");
        libVlc = new LibVLC(this.requireContext(), args);
        mediaPlayer = new MediaPlayer(libVlc);

        playerStatus.setMax(100);

        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        assert getArguments() != null;
        musicName = getArguments().getString("musicName");

        binding.backButton.setOnClickListener(view1 -> {
            ((MainActivity) requireActivity()).stopMusic();
            NavHostFragment.findNavController(PlayerFragment.this)
                    .navigate(R.id.action_PlayerFragment_to_ListenerFragment);
        });

        playPauseButton.setOnClickListener(view1 -> {
            if (isPlaying) {
                handler.removeCallbacks(updater);
                mediaPlayer.pause();
                playPauseButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            } else {
                mediaPlayer.play();
                playPauseButton.setImageResource(R.drawable.ic_baseline_pause_24);
            }
        });

        prepareMediaPlayer();

        mediaPlayer.setEventListener(event -> {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    isPlaying = true;
                    updatePlayerStatus();
                    break;
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                    isPlaying = false;
                    break;
                case MediaPlayer.Event.EndReached:
                    playerStatus.setProgress(0);
                    playPauseButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                    currentTime.setText(R.string.time_zero);
                    mediaPlayer.setTime(0);
                    prepareMediaPlayer();
                    break;
                case MediaPlayer.Event.Buffering:
                    playerStatus.setSecondaryProgress((int) event.getBuffering());
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        ((MainActivity)requireActivity()).stopMusic();
    }

    private void prepareMediaPlayer() {
        try {
            ((MainActivity)requireActivity()).playMusic(musicName);
            Media media = new Media(libVlc, Uri.parse(BuildConfig.VLC_RTSP_ADDRESS));
            media.setHWDecoderEnabled(true, false);
            media.addOption(":network-caching=150");
            media.addOption(":clock-jitter=0");
            mediaPlayer.setMedia(media);
            media.parse(Media.Parse.FetchNetwork);
            mediaPlayer.play();

            playPauseButton.setImageResource(R.drawable.ic_baseline_pause_24);
            totalTime.setText(millisecondsToTimer(media.getDuration()));
        } catch (Exception e) {
            Toast.makeText(this.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private final Runnable updater = new Runnable() {
        @Override
        public void run() {
            updatePlayerStatus();
            long currentDuration = mediaPlayer.getTime();
            currentTime.setText(millisecondsToTimer(currentDuration));
        }
    };

    private void updatePlayerStatus() {
        if (isPlaying) {
            handler.postDelayed(updater, 1000);
        }
    }

    public String millisecondsToTimer(long milliseconds) {
        String rtnTimer;

        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000* 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000* 60)) / 1000;

        rtnTimer = (hours > 0) ? (hours + ":") : "";
        rtnTimer += (minutes + ":");
        rtnTimer += (seconds < 10) ? ("0" + seconds) : seconds;

        return rtnTimer;
    }

}