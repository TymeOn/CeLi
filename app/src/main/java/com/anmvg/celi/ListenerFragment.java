package com.anmvg.celi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.anmvg.celi.databinding.FragmentListenerBinding;
import com.bumptech.glide.Glide;

public class ListenerFragment extends Fragment {

    private FragmentListenerBinding binding;
    private boolean listening;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentListenerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Glide.with(view)
                .load(R.drawable.sound_listen)
                .into(binding.loadingImage);
        binding.loadingImage.setVisibility(View.INVISIBLE);

        binding.listenButton.setOnClickListener(view1 -> {
            listening = !listening;
            binding.listenButton.setText(listening ? getString(R.string.listen_button_text_stop) : getString(R.string.listen_button_text_start));
            binding.loadingImage.setVisibility(listening ? View.VISIBLE : View.INVISIBLE);
            NavHostFragment.findNavController(ListenerFragment.this)
                .navigate(R.id.action_ListenerFragment_to_PlayerFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}