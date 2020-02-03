package com.example.locate.ui.send_position;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SendPositionViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public SendPositionViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is send_position fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}