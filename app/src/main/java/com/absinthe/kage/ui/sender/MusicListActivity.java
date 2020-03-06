package com.absinthe.kage.ui.sender;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.adapter.MusicListAdapter;
import com.absinthe.kage.databinding.ActivityMusicListBinding;
import com.absinthe.kage.media.audio.LocalMusic;
import com.absinthe.kage.ui.media.MusicActivity;
import com.absinthe.kage.viewmodel.MusicViewModel;

import java.util.ArrayList;
import java.util.List;

public class MusicListActivity extends BaseActivity {

    public static List<LocalMusic> sMusicList = new ArrayList<>();
    private ActivityMusicListBinding mBinding;
    private MusicViewModel mViewModel;
    private MusicListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMusicListBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mViewModel = new ViewModelProvider(this).get(MusicViewModel.class);

        initView();
        initData();
    }

    private void initView() {
        mAdapter = new MusicListAdapter();
        mBinding.rvMusicList.setAdapter(mAdapter);
        mBinding.rvMusicList.setLayoutManager(new LinearLayoutManager(this));

        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            LocalMusic localMusic = (LocalMusic) adapter.getData().get(position);
            if (localMusic != null) {
                Intent intent = new Intent(MusicListActivity.this, MusicActivity.class);
                intent.putExtra(MusicActivity.EXTRA_MUSIC_INFO, localMusic);
                intent.putExtra(MusicActivity.EXTRA_DEVICE_TYPE, MusicActivity.TYPE_SENDER);
                startActivity(intent);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initData() {
        mViewModel.getMusicList().observe(this, localMusics -> {
            mBinding.srlContainer.setRefreshing(false);
            mAdapter.setNewData(localMusics);
            sMusicList = localMusics;
            mBinding.srlContainer.setEnabled(false);
        });

        mBinding.srlContainer.setRefreshing(true);
        mViewModel.loadMusic(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
