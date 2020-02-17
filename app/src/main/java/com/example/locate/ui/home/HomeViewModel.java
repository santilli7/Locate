package com.example.locate.ui.home;

import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;
    private MutableLiveData<Color> color;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public HomeViewModel() {
        mText = new MutableLiveData<>();
        color = new MutableLiveData<>();
        mText.setValue("Stopped");
        color.setValue(Color.valueOf(Color.BLUE));
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<Color> getColor() {
        return color;
    }


}