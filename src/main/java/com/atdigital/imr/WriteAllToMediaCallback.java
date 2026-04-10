package com.atdigital.imr;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * Maps to: typedef MEDIA_ERROR(CALLBACK* WRITE_ALL_TO_MEDIA_DELEGATE)(char* buffer, int length);
 *
 * The native code calls this to write a full byte frame to the communication medium.
 * NOTE: JNA does not allow byte[] in callback parameters — must use Pointer.
 */
public interface WriteAllToMediaCallback extends Callback {
    /**
     * @param buffer  native pointer to bytes to send — use buffer.getByteArray(0, length)
     * @param length  number of bytes to send
     * @return        MediaError constant (0 = NONE = success)
     */
    int invoke(Pointer buffer, int length);
}
