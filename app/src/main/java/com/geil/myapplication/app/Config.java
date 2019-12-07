package com.geil.myapplication.app;

public class Config {

    // broadcast receiver intent filters
    public class Action {
        public static final String REGISTRATION_COMPLETE = "registrationComplete";
        public static final String PUSH_NOTIFICATION = "pushNotification";
        public static final String BLUETOOTH_CONNECTED = "bluetoothConnected";
        public static final String BLUETOOTH_DISCONNECTED = "bluetoothDisconnected";
        public static final String MESSAGE = "message";
    }

    public class Extra {
        public static final String DEVICE_NAME = "deviceName";
        public static final String REG_TOKEN = "regToken";
        public static final String COMMAND = "command";
    }

    public class Database {
        public static final String BRAND = "brand";
        public static final String MODEL = "model";
        public static final String NAME = "name";
        public static final String NUMBER = "number";
        public static final String REG_DATE = "regDate";
        public static final String REG_TOKEN = "regToken";
        public static final String SERIAL = "serial";
        public static final String CARRIER = "carrier";
        public static final String MESSAGES = "messages";
    }

    public class JSON {
        public static final String TIMESTAMP = "timestamp";
        public static final String MESSAGE_DB_KEY = "messageDbKey";
        public static final String COMMAND = "command";
    }

    // id to handle the notification in the notification tray
    public static final int NOTIFICATION_ID = 100;
    public static final int NOTIFICATION_ID_BIG_IMAGE = 101;
    public static final int PERMISSIONS_CODE = 1337;

    public static final int MESSAGE_TIMEOUT = 25000;
}
