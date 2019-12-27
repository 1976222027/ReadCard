package com.apicloud.myReadCard;

import android.content.Intent;
import android.nfc.Tag;

import org.json.JSONObject;

public interface NFCInstans {
    void getIntent(Intent intent);
    void getDestory();
}
