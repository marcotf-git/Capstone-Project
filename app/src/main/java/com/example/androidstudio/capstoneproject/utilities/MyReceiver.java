package com.example.androidstudio.capstoneproject.utilities;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;



public class MyReceiver extends ResultReceiver {

    private Receiver receiver;


    public MyReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }


    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (receiver != null) {
            super.onReceiveResult(resultCode, resultData);
        }
    }

}
