package com.anmvg.celi;

import static android.widget.Toast.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.anmvg.celi.databinding.FragmentListenerBinding;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class ListenerFragment extends Fragment {

    private FragmentListenerBinding binding;
    private boolean listening;

    SpeechRecognizer speechRecognizer;
    static final int RecordAudioRequestCode = 1;

    Button listenButton;

    TextView helpText;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentListenerBinding.inflate(inflater, container, false);
        listenButton = binding.listenButton;
        helpText = binding.helpLabel;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.getContext());

        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                ActivityCompat.requestPermissions(Objects.requireNonNull(this.getActivity()), new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                listenButton.setText(R.string.listen_button_text_start);
                helpText.setText("");
                helpText.setHint("...");
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int i) {
            }

            @Override
            public void onResults(Bundle bundle) {
                listenButton.setText(getString(R.string.listen_button_text_stop));
                helpText.setHint(getString(R.string.help_label_text));
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                helpText.setText(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {}

            @Override
            public void onEvent(int i, Bundle bundle) {}
        });

        listenButton.setOnTouchListener((view1, motionEvent) -> {

                    if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                    {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        listening = !listening;
                    }

                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        speechRecognizer.stopListening();
                        listening = !listening;
                        listenButton.setText(getString(R.string.listen_button_text_stop));
                        helpText.setHint(getString(R.string.help_label_text));
                    }

                    return false;
                }
        );


        Glide.with(view)
                .load(R.drawable.sound_listen)
                .into(binding.loadingImage);
        binding.loadingImage.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RecordAudioRequestCode && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            makeText(this.getContext(), getString(R.string.permission_granted_text), LENGTH_SHORT).show();
        }
        else
        {
            makeText(this.getContext(), getString(R.string.permission_denied_text), LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        speechRecognizer.destroy();
    }
}