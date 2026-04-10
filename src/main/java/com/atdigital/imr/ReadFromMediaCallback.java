package com.atdigital.imr;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * Maps to: typedef MEDIA_ERROR(CALLBACK* READ_FROM_MEDIA_DELEGATE)(char* byte);
 *
 * The native code calls this to read ONE byte from the communication medium.
 * Your implementation must:
 *   1. Read one byte from the stream (TCP socket / serial port).
 *   2. Write it into byteOut[0].
 *   3. Return a MediaError constant.
 *
 * NOTE: CALLBACK == __stdcall on Windows. JNA handles this automatically
 *       since we're on Windows and all JNA callbacks use stdcall.
 */
public interface ReadFromMediaCallback extends Callback {
    /**
     * @param byteOut  pointer to a single char — write the received byte here
     * @return         MediaError constant (0 = NONE = success)
     */
    int invoke(Pointer byteOut);
}
