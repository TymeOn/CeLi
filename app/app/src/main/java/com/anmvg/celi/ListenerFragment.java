package com.anmvg.celi;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.anmvg.celi.databinding.FragmentListenerBinding;
import com.skyfishjy.library.RippleBackground;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ListenerFragment extends Fragment {

    // Main binding
    private FragmentListenerBinding binding;

    // Permissions to RECORD_AUDIO
    static final int RecordAudioRequestCode = 1;
    private static final String PERMISSION_TAG = "Permission TAG";
    private static String fileName = null;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            Log.d(PERMISSION_TAG, "onActivityResult :");

            //Here we will handle the result of our intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //Android is 11(R) or above
                if (Environment.isExternalStorageManager()) {
                    // Manage External Storage Permission is granted
                    Log.d(PERMISSION_TAG, "onActivityResult : Manage External Storage Permission is granted");
                    createFile();
                } else {
                    // Manage External Storage Permission is denied
                    Log.d(PERMISSION_TAG, "onActivityResult : Manage External Storage Permission is denied");
                }
            }
        }
    );

    // UI variables
    private boolean listening;
    private RippleBackground rippleBg;

    // Media recorder
    private MediaRecorder recorder = null;

    // HTTP requests client
    private final OkHttpClient client = new OkHttpClient();


    // BASIC VIEW FUNCTIONS
    // --------------------

    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState
    ) {
        binding = FragmentListenerBinding.inflate(inflater, container, false);
        fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "AudioRecordCeli.mp3";
        uploadVoiceFile();
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check and request permissions
        if (checkPermissions()) {
            Log.d(PERMISSION_TAG, "onCLick: Permission already granted ...");
            createFile();
        } else {
            Log.d(PERMISSION_TAG, "onCLick: Permission was not granted, request ...");
            requestPermission();
        }

        // Listen button behavior
        rippleBg = binding.rippleBg;
        binding.listenButton.setOnClickListener(view1 -> {
            listening = !listening;
            binding.listenButton.setText(listening ? getString(R.string.listen_button_text_stop) : getString(R.string.listen_button_text_start));
            binding.helpLabel.setText(listening ? getString(R.string.press_stop) : getString(R.string.press_talk));

            if (listening) {
                rippleBg.startRippleAnimation();
            } else {
                rippleBg.stopRippleAnimation();
            }

            onRecord(listening);

            if (!listening) {
                // TODO VOSK CALL
            }
//            NavHostFragment.findNavController(ListenerFragment.this)
//                    .navigate(R.id.action_ListenerFragment_to_PlayerFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    // PERMISSIONS FUNCTIONS
    // ---------------------

    private boolean checkPermissions() {
        int record = ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.RECORD_AUDIO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above
            return record == PackageManager.PERMISSION_GRANTED &&
                    Environment.isExternalStorageManager();
        } else {
            //Android is below 11(R)
            int write = ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

            return record == PackageManager.PERMISSION_GRANTED &&
                    write == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above
            try {
                Log.d(PERMISSION_TAG, "requestPermission : try");

                ActivityCompat.requestPermissions(this.requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.requireActivity().getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            } catch (Exception e) {
                Log.d(PERMISSION_TAG, "requestPermission : catch", e);

                ActivityCompat.requestPermissions(this.requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);

                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        } else {
            //Android is below 11(R)
            ActivityCompat.requestPermissions(this.requireActivity(), permissions, RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // this method is called when user will
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // grant the permission for audio recording.
        if (requestCode == RecordAudioRequestCode) {
            if (grantResults.length > 0) {
                boolean record = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean write = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (record && write) {
                    makeText(this.getContext(), getString(R.string.permission_granted_text), LENGTH_SHORT).show();
                    createFile();
                } else {
                    makeText(this.getContext(), getString(R.string.permission_denied_text), LENGTH_SHORT).show();
                }
            }
        }
    }


    // VOICE RECORDING
    // ---------------

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(fileName);

        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    private void createFile() {
        File file = new File(fileName);

        try {
            if (file.exists()) {
                Log.d("File", "File exists");
            } else {
                boolean fileCreated = file.createNewFile();

                if (fileCreated) {
                    Log.d("File", "File created");
                } else {
                    Log.d("File", "File not created");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ASR
    // ---

    void uploadVoiceFile() {
        File file = new File(fileName);

        RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        file.getName(),
                        RequestBody.create(MediaType.parse("audio/mpeg"), file))
                .build();

        Request request = new Request.Builder()
                .url("http://<IP>:5000/nlp")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseText = Objects.requireNonNull(response.body()).string();
                Log.d("ASR", responseText);
            }
        });
    }

}