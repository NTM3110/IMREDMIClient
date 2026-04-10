package com.atdigital.imr;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Maps to (IMRCore.h / Edmi.h):
 *   typedef struct EDMI_DATE_TIME {
 *       unsigned char Year;   // 1 byte  (e.g. 25 = year 2025)
 *       unsigned char Month;  // 1 byte  (1–12)
 *       unsigned char Day;    // 1 byte  (1–31)
 *       unsigned char Hour;   // 1 byte  (0–23)
 *       unsigned char Minute; // 1 byte  (0–59)
 *       unsigned char Second; // 1 byte  (0–59)
 *       bool          IsNull; // 1 byte  (set true to mean "no filter")
 *   } EDMI_DATE_TIME;         // total = 7 bytes (pack=1)
 */
public class EdmiDateTime extends Structure {

    public byte Year;
    public byte Month;
    public byte Day;
    public byte Hour;
    public byte Minute;
    public byte Second;
    /** Set to 1 (true) to indicate "no date/time constraint" (used in profile reads) */
    public byte IsNull;

    public EdmiDateTime() {
        setAlignType(ALIGN_NONE);
    }

    public EdmiDateTime(int year, int month, int day, int hour, int minute, int second) {
        setAlignType(ALIGN_NONE);
        // EDMI Year field is offset from 2000 in some firmware; store raw value
        this.Year   = (byte) year;
        this.Month  = (byte) month;
        this.Day    = (byte) day;
        this.Hour   = (byte) hour;
        this.Minute = (byte) minute;
        this.Second = (byte) second;
        this.IsNull = 0;
    }

    /** Factory for a null/open-ended date (no filter) */
    public static EdmiDateTime nullDateTime() {
        EdmiDateTime dt = new EdmiDateTime();
        dt.IsNull = 1;
        return dt;
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("Year", "Month", "Day", "Hour", "Minute", "Second", "IsNull");
    }

    /** ByValue variant — used when the function takes the struct by value */
    public static class ByValue extends EdmiDateTime implements Structure.ByValue {
        public ByValue() { super(); }
        public ByValue(int year, int month, int day, int hour, int minute, int second) {
            super(year, month, day, hour, minute, second);
        }
    }

    @Override
    public String toString() {
        if (IsNull != 0) return "null";
        return String.format("20%02d-%02d-%02d %02d:%02d:%02d",
            Year & 0xFF, Month & 0xFF, Day & 0xFF,
            Hour & 0xFF, Minute & 0xFF, Second & 0xFF);
    }
}
