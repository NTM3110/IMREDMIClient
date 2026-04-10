package com.atdigital.imr;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Maps to (Edmi.h):
 *   typedef struct EDMI_FILE_FIELD {
 *       char Value[25]; // 25 bytes — raw field value (interpret with channel type)
 *   } EDMI_FILE_FIELD;   // total = 25 bytes (pack=1)
 *
 * EdmiReadProfile() allocates an array of these: rows * channelsCount elements.
 * Layout: [row0_ch0][row0_ch1]...[row0_chN][row1_ch0]...
 * You MUST call ImrCoreLib.INSTANCE.Release(pointer) when done.
 */
public class EdmiFileField extends Structure {

    public byte[] Value = new byte[25];

    public EdmiFileField() {
        setAlignType(ALIGN_NONE);
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("Value");
    }

    /** Get value as float (for Type 'F' or 'O' channels) */
    public float getFloat() {
        return java.nio.ByteBuffer.wrap(Value, 0, 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /** Get value as double (for Type 'D' or 'U' channels) */
    public double getDouble() {
        return java.nio.ByteBuffer.wrap(Value, 0, 8)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    /** Get value as 32-bit int (for Type 'L' channels) */
    public int getInt32() {
        return java.nio.ByteBuffer.wrap(Value, 0, 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    }
}
