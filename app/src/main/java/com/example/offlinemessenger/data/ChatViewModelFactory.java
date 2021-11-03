package com.example.offlinemessenger.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.offlinemessenger.db.AppDatabase;

public class ChatViewModelFactory implements ViewModelProvider.Factory {

    private final AppDatabase mAppDb;

    public ChatViewModelFactory(AppDatabase db) {
        mAppDb = db;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ChatViewModel.class)) {
            return (T) new ChatViewModel(mAppDb);
        } else {
            throw new IllegalArgumentException("Invalid ViewModel class");
        }
    }

}
