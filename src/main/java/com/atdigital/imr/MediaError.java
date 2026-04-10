package com.atdigital.imr;

/**
 * Maps to MEDIA_ERROR enum in Media.h
 */
public final class MediaError {
    public static final int NONE                = 0;
    public static final int NOT_INITIATED       = 1;
    public static final int CAN_NOT_OPEN_DEVICE = 2;
    public static final int NOT_CONNECTED       = 3;
    public static final int WRITE_ERROR         = 4;
    public static final int READ_ERROR          = 5;
    public static final int RESPONSE_TIMEOUT    = 6;
    public static final int EXCEPTION           = 7;

    public static String name(int code) {
        switch (code) {
            case NONE:                return "NONE";
            case NOT_INITIATED:       return "NOT_INITIATED";
            case CAN_NOT_OPEN_DEVICE: return "CAN_NOT_OPEN_DEVICE";
            case NOT_CONNECTED:       return "NOT_CONNECTED";
            case WRITE_ERROR:         return "WRITE_ERROR";
            case READ_ERROR:          return "READ_ERROR";
            case RESPONSE_TIMEOUT:    return "RESPONSE_TIMEOUT";
            case EXCEPTION:           return "EXCEPTION";
            default:                  return "UNKNOWN(" + code + ")";
        }
    }

    private MediaError() {}
}
