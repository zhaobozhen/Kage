package com.absinthe.kage.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.absinthe.kage.media.audio.LocalMusic;
import com.absinthe.kage.media.audio.MusicHelper;

import java.util.List;

public class MusicViewModel extends AndroidViewModel {

    private MutableLiveData<List<LocalMusic>> mMusicList = new MutableLiveData<>();

    public MusicViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<List<LocalMusic>> getMusicList() {
        return mMusicList;
    }

    public void loadMusic(Context context) {
        new Thread(() -> {
            List<LocalMusic> list = MusicHelper.getAllLocalMusic(context);
            new Handler(Looper.getMainLooper()).post(() -> mMusicList.setValue(list));
        }).start();
    }
}
