package com.app.moodtrack_android.model.log;

public enum LogEntryAction {
    APP_RECEIVED_FCM_MESSAGE,
    USER_SIGNED_IN,
    USER_SIGNED_UP,
    USER_SIGNED_OUT,
    /*
    These may or may not be neccessary
    Depending on the load we want to
    put on our server/Firestore quota.
    USER_RESPONSE_SUBMITTED,
    USER_VISITED_HOME,
    USER_VISITED_SETTINGS,
    */
}
