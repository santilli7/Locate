package com.example.locate.ui.searchBleDevices;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SearchBleDevicesViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public SearchBleDevicesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is share fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}