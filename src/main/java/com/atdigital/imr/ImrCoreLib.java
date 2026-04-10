package com.atdigital.imr;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA interface to IMR.Core.x64.dll — EDMI protocol functions only.
 *
 * Place IMR.Core.x64.dll in the 'native/' folder of the project,
 * then run with: gradle run  (JVM arg -Djna.library.path=native is set in build.gradle)
 *
 * Or at runtime, set: System.setProperty("jna.library.path", "path/to/dll/folder");
 * before this class is loaded.
 */
public interface ImrCoreLib extends Library {

    ImrCoreLib INSTANCE = Native.load("IMR.Core.x64", ImrCoreLib.class);

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /** Must be called once before any other function. */
    void Init();

    /**
     * Frees memory allocated by the native library (e.g. the fields array
     * returned by EdmiReadProfile). Always call this after you're done reading.
     */
    void Release(Pointer buffer);

    // ─── EDMI Functions ───────────────────────────────────────────────────────

    /**
     * Login to the meter.
     *
     * @param readCallback   callback to read ONE byte from the medium (see ReadFromMediaCallback)
     * @param writeCallback  callback to write all bytes to the medium (see WriteAllToMediaCallback)
     * @param serial         meter serial number (unsigned int)
     * @param username       login username bytes
     * @param usernameLength length of username
     * @param password       login password bytes
     * @param passwordLength length of password
     * @param errCode        output: EDMI_ERROR_CODE (0 = success)
     * @return               MediaError constant
     */
    int EdmiLogin(
        ReadFromMediaCallback  readCallback,
        WriteAllToMediaCallback writeCallback,
        int                    serial,
        byte[]                 username,
        int                    usernameLength,
        byte[]                 password,
        int                    passwordLength,
        byte[]                 errCode         // single byte output
    );

    /**
     * Logout from the meter.
     *
     * @param errCode output: EDMI_ERROR_CODE (single byte)
     * @return        MediaError constant
     */
    int EdmiLogout(
        ReadFromMediaCallback  readCallback,
        WriteAllToMediaCallback writeCallback,
        int                    serial,
        byte[]                 errCode
    );

    /**
     * Read a single register from the meter.
     * Populate reg.Address before calling. After the call, check reg.ErrorCode,
     * then read reg.Value using reg.getFloat() / reg.getDouble() etc.
     *
     * @param reg    register to read (Address must be set beforehand)
     * @param errCode output: EDMI_ERROR_CODE
     * @return        MediaError constant
     */
    int EdmiReadRegister(
        ReadFromMediaCallback  readCallback,
        WriteAllToMediaCallback writeCallback,
        int                    serial,
        EdmiRegister           reg,
        byte[]                 errCode
    );

    /**
     * Read multiple registers in one request (more efficient than calling
     * EdmiReadRegister in a loop).
     *
     * @param regs      array of EdmiRegister (each must have Address set)
     * @param regsCount number of registers in the array
     * @param errCode   output: EDMI_ERROR_CODE
     * @return          MediaError constant
     */
    int EdmiReadRegisters(
        ReadFromMediaCallback  readCallback,
        WriteAllToMediaCallback writeCallback,
        int                    serial,
        EdmiRegister[]         regs,
        int                    regsCount,
        byte[]                 errCode
    );

    /**
     * Read load profile data between two timestamps.
     *
     * The native library allocates the fields array — you MUST call
     * Release(fieldsRef.getValue()) when done to avoid a memory leak.
     *
     * @param survey      survey/load-profile identifier (e.g. 0x0305 for LS01)
     * @param fromDateTime start of time range (use EdmiDateTime.nullDateTime() for "from beginning")
     * @param toDateTime   end of time range   (use EdmiDateTime.nullDateTime() for "to end")
     * @param profileSpec  output: filled with profile metadata (interval, channels, etc.)
     * @param fieldsRef    output: pointer-to-pointer — native allocates EDMI_FILE_FIELD array here
     * @param errCode      output: EDMI_ERROR_CODE
     * @return             MediaError constant
     *
     * After the call:
     *   int rows     = profileSpec.RecordsCount;
     *   int channels = profileSpec.ChannelsCount & 0xFF;
     *   Pointer p    = fieldsRef.getValue();
     *   // Each EDMI_FILE_FIELD is 25 bytes:
     *   for (int r = 0; r < rows; r++) {
     *       for (int c = 0; c < channels; c++) {
     *           long offset = (long)(r * channels + c) * 25;
     *           byte[] raw  = p.getByteArray(offset, 25);
     *           // interpret raw using profileSpec.ChannelsInfo[c].Type
     *       }
     *   }
     *   INSTANCE.Release(p);
     */
    int EdmiReadProfile(
        ReadFromMediaCallback   readCallback,
        WriteAllToMediaCallback  writeCallback,
        int                     serial,
        short                   survey,
        EdmiDateTime.ByValue    fromDateTime,
        EdmiDateTime.ByValue    toDateTime,
        EdmiProfileSpec         profileSpec,
        PointerByReference      fieldsRef,
        byte[]                  errCode
    );
}
