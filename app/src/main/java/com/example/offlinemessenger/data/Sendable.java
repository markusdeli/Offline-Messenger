package com.example.offlinemessenger.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public abstract class Sendable implements Serializable, Parcelable {

    private Action mAction;

    public Sendable(Action action) {
        mAction = action;
    }

    public enum Action {

        ADD((byte) 1),
        REMOVE((byte) 2),
        NONE((byte) 0);

        private final byte mVal;

        Action(byte val) {
            mVal = val;
        }

        public byte getValue() {
            return mVal;
        }

        static Action fromValue(byte value) {
            switch (value) {
                case 1: return ADD;
                case 2: return REMOVE;
                default: return NONE;
            }
        }

    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeSerializable(this);
    }

    @Override
    public abstract String toString();

    public final Action getAction() {
        return mAction;
    }

    public final void setAction(Action action) {
        mAction = action;
    }

}
