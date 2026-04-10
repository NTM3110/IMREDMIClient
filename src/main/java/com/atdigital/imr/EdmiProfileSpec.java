package com.atdigital.imr;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Maps to (Edmi.h):
 *   #define EDMI_MAX_CHANNELS_COUNT 16
 *   typedef struct EDMI_PROFILE_SPEC {
 *       short                  Survey;          //  2 bytes
 *       int                    Interval;        //  4 bytes  (seconds between readings)
 *       EDMI_DATE_TIME         FromDateTime;    //  7 bytes
 *       EDMI_DATE_TIME         ToDateTime;      //  7 bytes
 *       int                    RecordsCount;    //  4 bytes
 *       unsigned char          ChannelsCount;   //  1 byte
 *       EDMI_FILE_CHANNEL_INFO ChannelsInfo[16];// 32*16 = 512 bytes
 *       char                   Name[25];        // 25 bytes
 *   } EDMI_PROFILE_SPEC;                        // total = 562 bytes (pack=1)
 *
 * This struct is filled by EdmiReadProfile(). After the call, read:
 *   - Interval         → recording interval in seconds
 *   - RecordsCount     → how many rows were returned
 *   - ChannelsCount    → how many channels (columns) per row
 *   - ChannelsInfo[]   → type/unit/name of each channel
 */
public class EdmiProfileSpec extends Structure {

    public short                Survey;
    public int                  Interval;
    public EdmiDateTime         FromDateTime  = new EdmiDateTime();
    public EdmiDateTime         ToDateTime    = new EdmiDateTime();
    public int                  RecordsCount;
    public byte                 ChannelsCount;
    public EdmiFileChannelInfo[]ChannelsInfo  = new EdmiFileChannelInfo[16];
    public byte[]               Name          = new byte[25];

    public EdmiProfileSpec() {
        setAlignType(ALIGN_NONE);
        for (int i = 0; i < 16; i++) {
            ChannelsInfo[i] = new EdmiFileChannelInfo();
        }
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
            "Survey", "Interval", "FromDateTime", "ToDateTime",
            "RecordsCount", "ChannelsCount", "ChannelsInfo", "Name"
        );
    }

    public String getProfileName() {
        int len = 0;
        while (len < Name.length && Name[len] != 0) len++;
        return new String(Name, 0, len, java.nio.charset.StandardCharsets.US_ASCII);
    }
}
