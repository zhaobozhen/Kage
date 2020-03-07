package com.absinthe.kage.media;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayList implements Parcelable {
    private static final String TAG = PlayList.class.getSimpleName();
    private int mCurrentIndex = -1;
    private List<LocalMedia> mList = new ArrayList<>();

    public PlayList() {
    }

    private PlayList(Parcel in) {
        mCurrentIndex = in.readInt();
        mList = in.createTypedArrayList(LocalMedia.CREATOR);
    }

    public static final Creator<PlayList> CREATOR = new Creator<PlayList>() {
        @Override
        public PlayList createFromParcel(Parcel in) {
            return new PlayList(in);
        }

        @Override
        public PlayList[] newArray(int size) {
            return new PlayList[size];
        }
    };

    public void setCurrentIndex(int currentIndex) {
        if (currentIndex < 0 || currentIndex > this.mList.size()) {
            this.mCurrentIndex = 0;
        } else {
            this.mCurrentIndex = currentIndex;
        }
    }

    public int getCurrentIndex() {
        return this.mCurrentIndex;
    }

    public LocalMedia getCurrentMedia() {
        if (mList == null || mCurrentIndex < 0 || mCurrentIndex >= mList.size()) {
            return null;
        }

        return mList.get(mCurrentIndex);
    }

    public boolean hasNextMedia() {
        return mList != null && mCurrentIndex >= 0 && mCurrentIndex < mList.size() - 1;
    }

    public boolean hasPreviousMedia() {
        return mList != null && mCurrentIndex > 0;
    }

    public LocalMedia getPreviousMedia(boolean isRepeat, boolean isShuffled) {
        if (mList == null || !hasPreviousMedia() || mCurrentIndex == -1) {
            return null;
        }

        if (isShuffled) {
            double size = mList.size();
            double random = Math.random();
            mCurrentIndex = (int) (size * random);
        } else if (mCurrentIndex != 0) {
            mCurrentIndex--;
        } else if (!isRepeat) {
            return null;
        } else {
            mCurrentIndex = mList.size() - 1;
        }

        return mList.get(mCurrentIndex);
    }

    public LocalMedia getNextMedia(boolean isRepeat, boolean isShuffled) {
        if (mList == null || !hasNextMedia() || mCurrentIndex == -1) {
            return null;
        }

        if (isShuffled) {
            double size = mList.size();
            double random = Math.random();
            mCurrentIndex = (int) (size * random);
        } else if (mCurrentIndex != mList.size() - 1) {
            mCurrentIndex++;
        } else if (!isRepeat) {
            return null;
        } else {
            mCurrentIndex = 0;
        }

        return mList.get(mCurrentIndex);
    }

    public LocalMedia getMedia(int pos) {
        if (mList == null || pos < 0 || pos >= mList.size()) {
            return null;
        }
        this.mCurrentIndex = pos;
        return mList.get(pos);
    }

    public void setList(List<LocalMedia> list) {
        setList(list, mCurrentIndex);
    }

    public void setList(List<LocalMedia> list, int index) {
        if (mList == null) {
            mList = list;
        } else {
            mList.clear();
            mList.addAll(list);
        }
        setCurrentIndex(index);
    }

    public List<LocalMedia> getList() {
        return mList;
    }

    public void addNextMedia(LocalMedia localMedia) {
        addNextMedia(localMedia, false);
    }

    public void addNextMedia(LocalMedia localMedia, boolean isMulti) {
        addMedia(mCurrentIndex + 1, localMedia, isMulti);
    }

    public void addMedia(LocalMedia localMedia) {
        addMedia(mList.size(), localMedia, false);
    }

    private void addMedia(int position, LocalMedia localMedia, boolean isMulti) {
        if (position > mList.size() || position < mCurrentIndex + 1) {
            Log.e(TAG, "position IndexOutOfBounds");
            return;
        }
        if (isMulti) {
            mList.add(position, localMedia);
        } else {
            int index = mList.indexOf(localMedia);
            if (index < 0) {
                mList.add(position, localMedia);
            } else if (index != position) {
                mList.remove(index);
                mList.add(position, localMedia);
            } else {
                Collections.swap(mList, position, index);
            }
        }
        if (mCurrentIndex == -1) {
            mCurrentIndex = 0;
        }
    }

    public int queryMediaIndex(LocalMedia localMedia) {
        return mList.indexOf(localMedia);
    }

    public void clearPlaylist() {
        mList.clear();
        mCurrentIndex = -1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCurrentIndex);
        dest.writeTypedList(mList);
    }
}
