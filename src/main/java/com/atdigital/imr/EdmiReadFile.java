package com.atdigital.imr;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Maps to (Edmi.h):
 *   typedef struct EDMI_READ_FILE {
 *       int   StartRecord;   // 4 bytes
 *       short RecordsCount;  // 2 bytes
 *       short RecordOffset;  // 2 bytes
 *       short RecordSize;    // 2 bytes
 *   } EDMI_READ_FILE;        // total = 10 bytes (pack=1)
 */
public class EdmiReadFile extends Structure {

    public int   StartRecord;
    public short RecordsCount;
    public short RecordOffset;
    public short RecordSize;

    public EdmiReadFile() {
        setAlignType(ALIGN_NONE);
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
            "StartRecord", "RecordsCount", "RecordOffset", "RecordSize"
        );
    }
}