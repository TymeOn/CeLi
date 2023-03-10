package com.anmvg.celi;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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


public class PlayerFragment extends Fragment {

    private FragmentPlayerBinding binding;
    private TextView currentTime, totalTime;
    private SeekBar playerStatus;
    private ImageButton playPauseButton;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();

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
        mediaPlayer = new MediaPlayer();

        playerStatus.setMax(100);

        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.backButton.setOnClickListener(view1 -> NavHostFragment.findNavController(PlayerFragment.this)
                .navigate(R.id.action_PlayerFragment_to_ListenerFragment));

        playPauseButton.setOnClickListener(view1 -> {
            if (mediaPlayer.isPlaying()) {
                handler.removeCallbacks(updater);
                mediaPlayer.pause();
                playPauseButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            } else {
                mediaPlayer.start();
                playPauseButton.setImageResource(R.drawable.ic_baseline_pause_24);
                updatePlayerStatus();
            }
        });

        prepareMediaPlayer();

        playerStatus.setOnTouchListener((view1, motionEvent) -> {
            int playPosition = (mediaPlayer.getDuration() / 100) * playerStatus.getProgress();
            mediaPlayer.seekTo(playPosition);
            currentTime.setText(millisecondsToTimer(mediaPlayer.getCurrentPosition()));
            return false;
        });

        mediaPlayer.setOnBufferingUpdateListener((mediaPlayer, i) -> playerStatus.setSecondaryProgress(i));

        mediaPlayer.setOnCompletionListener(mediaPlayer -> {
            playerStatus.setProgress(0);
            playPauseButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            currentTime.setText(R.string.time_zero);
            mediaPlayer.reset();
            prepareMediaPlayer();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        // TODO : MANAGE THE PLAYER AFTER THE FRAGMENT IS LEFT
    }

    private void prepareMediaPlayer() {
        try {
            mediaPlayer.setDataSource("https://filesamples.com/samples/audio/mp3/sample2.mp3");
            mediaPlayer.prepare();
            totalTime.setText(millisecondsToTimer(mediaPlayer.getDuration()));
        } catch (Exception e) {
            Toast.makeText(this.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Runnable updater = new Runnable() {
        @Override
        public void run() {
            updatePlayerStatus();
            long currentDuration = mediaPlayer.getCurrentPosition();
            currentTime.setText(millisecondsToTimer(currentDuration));
        }
    };

    private void updatePlayerStatus() {
        if (mediaPlayer.isPlaying()) {
            playerStatus.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration()) * 100));
            handler.postDelayed(updater, 1000);
        }
    }

    public String millisecondsToTimer(long milliseconds) {
        String rtnTimer = "";

        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000* 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000* 60)) / 1000;

        rtnTimer = (hours > 0) ? (hours + ":") : "";
        rtnTimer += (minutes + ":");
        rtnTimer += (seconds < 10) ? ("0" + seconds) : seconds;

        return rtnTimer;
    }

}