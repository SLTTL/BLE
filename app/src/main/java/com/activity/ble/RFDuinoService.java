package com.activity.ble;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RFDuinoService extends Service {
    public RFDuinoService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
