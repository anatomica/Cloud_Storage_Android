package ru.anatomica.cloud_storage.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

public class PageViewModel extends ViewModel {

    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();
    private LiveData<String> mText = Transformations.map(mIndex, input -> {
        if (input == 1) return "Первая таблица";
        else return "Вторая таблица";
    });

    public void setIndex(int index) { mIndex.setValue(index); }

    public int getIndex() { return mIndex.getValue(); }

    public LiveData<String> getText() { return mText; }
}