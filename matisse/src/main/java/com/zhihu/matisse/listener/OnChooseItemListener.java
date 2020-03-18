package com.zhihu.matisse.listener;

public interface OnChooseItemListener {
    void onChoose(String itemUri);
    void onStop();
    void onPreview();
    void onNext();
    void onNotConnect();
}
