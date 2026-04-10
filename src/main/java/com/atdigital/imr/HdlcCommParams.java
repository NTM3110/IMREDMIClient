package com.atdigital.imr;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Maps to (Hdlc.h):
 *   typedef struct HDLC_COMM_PARAMS {
 *       unsigned int  ServerID;           // 4 bytes
 *       unsigned char ClientID;           // 1 byte
 *       unsigned char MaxInfoLenTransmit; // 1 byte
 *       unsigned char MaxInfoLenReceive;  // 1 byte
 *       unsigned char WinSizeTransmit;    // 1 byte
 *       unsigned char WinSizeReceive;     // 1 byte
 *       unsigned char S;                  // 1 byte  (send sequence counter)
 *       unsigned char R;                  // 1 byte  (receive sequence counter)
 *   } HDLC_COMM_PARAMS;                  // total = 11 bytes (pack=1)
 *
 * Typical defaults:
 *   ServerID          = DlmsGetServerID(physicalAddr, logicalAddr)
 *   ClientID          = 0x01  (public client) or 0x03 (management client)
 *   MaxInfoLenTransmit = 0x80 (128)
 *   MaxInfoLenReceive  = 0x80 (128)
 *   WinSizeTransmit   = 0x01
 *   WinSizeReceive    = 0x01
 *   S and R are managed internally by the library — set to 0.
 */
public class HdlcCommParams extends Structure {

    public int           ServerID;            // unsigned int
    public byte          ClientID;
    public byte          MaxInfoLenTransmit;
    public byte          MaxInfoLenReceive;
    public byte          WinSizeTransmit;
    public byte          WinSizeReceive;
    public byte          S;
    public byte          R;

    public HdlcCommParams() {
        setAlignType(ALIGN_NONE);
    }

    /** Convenience constructor with the most common settings */
    public HdlcCommParams(int serverID, byte clientID) {
        setAlignType(ALIGN_NONE);
        this.ServerID           = serverID;
        this.ClientID           = clientID;
        this.MaxInfoLenTransmit = (byte) 0x80;
        this.MaxInfoLenReceive  = (byte) 0x80;
        this.WinSizeTransmit    = 0x01;
        this.WinSizeReceive     = 0x01;
        this.S                  = 0x00;
        this.R                  = 0x00;
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
            "ServerID", "ClientID",
            "MaxInfoLenTransmit", "MaxInfoLenReceive",
            "WinSizeTransmit", "WinSizeReceive",
            "S", "R"
        );
    }

    /** ByValue variant — used when the function takes the struct by value (not pointer) */
    public static class ByValue extends HdlcCommParams implements Structure.ByValue {
        public ByValue() { super(); }
        public ByValue(int serverID, byte clientID) { super(serverID, clientID); }
    }
}
