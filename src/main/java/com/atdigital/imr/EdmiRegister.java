package com.atdigital.imr;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Maps to (Edmi.h):
 *   typedef struct EDMI_REGISTER {
 *       int  Address;     // 4 bytes — register number (e.g. 0x0001)
 *       char Type;        // 1 byte  — EDMI_TYPE char code ('F'=float, 'L'=long, etc.)
 *       char UnitCode;    // 1 byte  — EDMI_UNIT_CODE char code ('W'=watts, 'X'=Wh, etc.)
 *       char ErrorCode;   // 1 byte  — EDMI_ERROR_CODE (0 = no error)
 *       char Value[25];   // 25 bytes — raw value bytes (interpret using Type)
 *   } EDMI_REGISTER;      // total = 32 bytes (pack=1)
 *
 * Common Type codes (EDMI_TYPE):
 *   'F' = float (32-bit IEEE)     → read as float from Value[0..3]
 *   'D' = double (64-bit IEEE)    → read as double from Value[0..7]
 *   'L' = long (32-bit int)       → read as int from Value[0..3]
 *   'I' = short (16-bit int)      → read as short from Value[0..1]
 *   'A' = ASCII string            → read as null-terminated string
 *   'T' = time-date (6 bytes)     → {Date,Month,Year,Hour,Min,Sec}
 *   'O' = float energy            → float with transformer ratio applied
 *   'U' = double energy           → double with transformer ratio applied
 *
 * Common UnitCode codes (EDMI_UNIT_CODE):
 *   'W' = Watts   'X' = Wh   'R' = VARs   'Y' = VARh
 *   'S' = VA      'Z' = VAh  'V' = Volts   'A' = Amps
 */
public class EdmiRegister extends Structure {

    public int    Address;
    public byte   Type;
    public byte   UnitCode;
    public byte   ErrorCode;
    public byte[] Value = new byte[25];

    public EdmiRegister() {
        setAlignType(ALIGN_NONE);
    }

    public EdmiRegister(int address) {
        setAlignType(ALIGN_NONE);
        this.Address = address;
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("Address", "Type", "UnitCode", "ErrorCode", "Value");
    }

    // ─── Helpers to extract typed value from Value[] ──────────────────────

    /** Read Value as a 32-bit float (Type == 'F' or 'O') */
    public float getFloat() {
        return java.nio.ByteBuffer.wrap(Value, 0, 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /** Read Value as a 64-bit double (Type == 'D' or 'U') */
    public double getDouble() {
        return java.nio.ByteBuffer.wrap(Value, 0, 8)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    /** Read Value as a 32-bit signed int (Type == 'L') */
    public int getInt32() {
        return java.nio.ByteBuffer.wrap(Value, 0, 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /** Read Value as a 16-bit signed short (Type == 'I') */
    public short getInt16() {
        return java.nio.ByteBuffer.wrap(Value, 0, 2)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public String getString() {
        int len = 0;
        while (len < Value.length && Value[len] != 0) len++;
        return new String(Value, 0, len, java.nio.charset.StandardCharsets.US_ASCII);
    }

    public String getFormattedValue() {
        switch ((char)(Type & 0xFF)) {
            case 'F': case 'O': case 'P':
                return String.format("%.2f", getFloat());
            case 'D': case 'U':
                return String.format("%.2f", getDouble());
            case 'L':
                return String.valueOf(getInt32());
            case 'I':
                return String.valueOf(getInt16());
            case 'M': // Serial number (if DLL allows it through as a string now)
            case 'A': // ASCII String
            case 'K': // Error string
                return getString();
            
            // DLL parses 'R', 'Q', and 'T' into a 7-byte EDMI_DATE_TIME struct placed in Value[0..6]
            // Format: [0]=Year(since 2000), [1]=Month, [2]=Day, [3]=Hour, [4]=Min, [5]=Sec, [6]=IsNull
            case 'R': // Date
                return String.format("%04d-%02d-%02d", (Value[0] & 0xFF) + 2000, Value[1] & 0xFF, Value[2] & 0xFF);
            case 'Q': // Time
                return String.format("%02d:%02d:%02d", Value[3] & 0xFF, Value[4] & 0xFF, Value[5] & 0xFF);
            case 'T': // DateTime
                return String.format("%04d-%02d-%02d %02d:%02d:%02d", 
                        (Value[0] & 0xFF) + 2000, Value[1] & 0xFF, Value[2] & 0xFF,
                        Value[3] & 0xFF, Value[4] & 0xFF, Value[5] & 0xFF);
                        
            default:
                return "Unknown Type " + (char)(Type & 0xFF);
        }
    }

    public boolean hasError() { return ErrorCode != 0; }

    @Override
    public String toString() {
        return String.format("EdmiRegister{addr=0x%04X, type=%c, unit=%c, err=%d, val=%s}",
            Address, (char)(Type & 0xFF), (char)(UnitCode & 0xFF), ErrorCode & 0xFF, getFormattedValue());
    }
}
