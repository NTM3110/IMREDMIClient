package com.atdigital.imr;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Maps to (Edmi.h):
 *   typedef struct EDMI_FILE_CHANNEL_INFO {
 *       char  Type;          // 1 byte  — EDMI_TYPE of values in this channel
 *       char  UnitCode;      // 1 byte  — EDMI_UNIT_CODE
 *       char  ScalingCode;   // 1 byte  — scaling exponent code
 *       float ScalingFactor; // 4 bytes — multiplier to apply to raw values
 *       char  Name[25];      // 25 bytes — null-terminated channel name
 *   } EDMI_FILE_CHANNEL_INFO; // total = 32 bytes (pack=1)
 */
public class EdmiFileChannelInfo extends Structure {

    public byte   Type;
    public byte   UnitCode;
    public byte   ScalingCode;
    public float  ScalingFactor;
    public byte[] Name = new byte[25];

    public EdmiFileChannelInfo() {
        setAlignType(ALIGN_NONE);
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("Type", "UnitCode", "ScalingCode", "ScalingFactor", "Name");
    }

    public String getChannelName() {
        int len = 0;
        while (len < Name.length && Name[len] != 0) len++;
        return new String(Name, 0, len, java.nio.charset.StandardCharsets.US_ASCII);
    }

    @Override
    public String toString() {
        return String.format("Channel{name='%s', type=%c, unit=%c, scale=%.4f}",
            getChannelName(), (char)(Type & 0xFF), (char)(UnitCode & 0xFF), ScalingFactor);
    }
}
