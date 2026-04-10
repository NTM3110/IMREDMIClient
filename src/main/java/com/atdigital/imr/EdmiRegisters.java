package com.atdigital.imr;

/**
 * EDMI register addresses and their types.
 *
 * CRITICAL: The DLL parses response values using the Type field pre-set in the
 * EDMI_REGISTER struct — it does NOT read the type from the wire response.
 * Always call EdmiRegister(address, EdmiRegisters.getType(address)) so the
 * DLL knows how to decode bytes from the meter. Default type=0 → error 18.
 *
 * Type bytes match EdmiType enum in EdmiTypes.cs / EDMI_TYPE in Edmi.h.
 */
public final class EdmiRegisters {

    // ─── EDMI wire type codes (ASCII, matches C EDMI_TYPE enum) ─────────────
    public static final byte TYPE_STRING        = 'A'; // null-terminated string
    public static final byte TYPE_FLOAT         = 'F'; // IEEE 32-bit float
    public static final byte TYPE_FLOAT_ENERGY  = 'O'; // 32-bit micropulses → float
    public static final byte TYPE_POWER_FACTOR  = 'P'; // int16 → float -1..1
    public static final byte TYPE_LONG          = 'L'; // 32-bit signed int
    public static final byte TYPE_DOUBLE        = 'D'; // IEEE 64-bit double
    public static final byte TYPE_DOUBLE_ENERGY = 'U'; // 64-bit micropulses → double
    public static final byte TYPE_DATE          = 'R'; // 3 bytes: day, month, year
    public static final byte TYPE_TIME          = 'Q'; // 3 bytes: hour, min, sec
    public static final byte TYPE_TIME_DATE     = 'T'; // 6 bytes: date + time
    public static final byte TYPE_SERIAL_NUMBER = 'M'; // ASCII serial (unsupported by DLL)

    /**
     * Returns the DLL wire type byte for the given register address.
     * Must be pre-set in EdmiRegister.Type before calling EdmiReadRegister.
     * The DLL uses this to decode the meter response — it does NOT read the
     * type from the wire. Default is TYPE_FLOAT_ENERGY for unknown addresses.
     */
    public static byte getType(int address) {
        switch (address) {
            // ── Transformer ratios → Float ('F') ──────────────────────────────
            case 0xF700: case 0xF701: case 0xF702: case 0xF703:
                return TYPE_FLOAT;

            // ── Instantaneous → FloatEnergy ('O') ────────────────────────────
            case 0xE000: case 0xE001: case 0xE002: // voltage
            case 0xE010: case 0xE011: case 0xE012: // current
            case 0xE020: case 0xE021: case 0xE022: // angle
            case 0xE023: case 0xE024:              // VT angles
            case 0xE030: case 0xE031: case 0xE032: case 0xE033: // watts
            case 0xE040: case 0xE041: case 0xE042: case 0xE043: // vars
            case 0xE050: case 0xE051: case 0xE052: case 0xE053: // VA
            case 0xE060:                           // frequency
            case 0x9000: case 0x9100: case 0x9200: // THD current
            case 0x9300: case 0x9400: case 0x9500: // THD voltage
                return TYPE_FLOAT_ENERGY;

            // ── Power factor → PowerFactor ('P') ─────────────────────────────
            case 0xE026:
                return TYPE_POWER_FACTOR;

            // ── Energy registers → DoubleEnergy ('U') ────────────────────────
            case 0x0060: case 0x0061: case 0x0062: case 0x0069: // import kWh
            case 0x0160: case 0x0161: case 0x0162: case 0x0169: // export kWh
            case 0x0269: case 0x0369:              // kVARh
            case 0x1069: case 0x1169:              // max demand
                return TYPE_DOUBLE_ENERGY;

            // ── Date / Time ───────────────────────────────────────────────────
            case 0xF010: return TYPE_DATE;      // current date
            case 0xF011: return TYPE_TIME;      // current time
            case 0xF03D: return TYPE_TIME_DATE; // date + time

            // ── Serial number — EDMI uses 'M' but the DLL doesn't support 'M'.
            // However, the wire format for 'M' is identical to 'A' (ASCII String).
            // By telling the DLL it's TYPE_STRING ('A'), it decodes it perfectly!
            case 0xF002: return TYPE_STRING;

            default:     return TYPE_FLOAT_ENERGY; // safe fallback
        }
    }

    // ─── Meter Information ────────────────────────────────────────────────────
    public static final int METER_SERIAL_NUMBER = 0xF002;
    public static final int CURRENT_DATE        = 0xF010;
    public static final int CURRENT_TIME        = 0xF011;
    public static final int DATE_TIME           = 0xF03D;
    public static final int ERROR_CODE          = 0xF016;

    // ─── Transformer Ratios ───────────────────────────────────────────────────
    public static final int CT_RATIO_PRIMARY    = 0xF700;  // same as CURRENT_MULTIPLIER
    public static final int VT_RATIO_PRIMARY    = 0xF701;  // same as VOLTAGE_MULTIPLIER
    public static final int CT_RATIO_SECONDARY  = 0xF702;  // same as CURRENT_DIVISOR
    public static final int VT_RATIO_SECONDARY  = 0xF703;  // same as VOLTAGE_DIVISOR

    // ─── Voltage (V) ──────────────────────────────────────────────────────────
    public static final int PHASE_A_VOLTAGE     = 0xE000;
    public static final int PHASE_B_VOLTAGE     = 0xE001;
    public static final int PHASE_C_VOLTAGE     = 0xE002;

    // ─── Current (A) ──────────────────────────────────────────────────────────
    public static final int PHASE_A_CURRENT     = 0xE010;
    public static final int PHASE_B_CURRENT     = 0xE011;
    public static final int PHASE_C_CURRENT     = 0xE012;

    // ─── Angles (degrees) ─────────────────────────────────────────────────────
    public static final int PHASE_A_ANGLE       = 0xE020;
    public static final int PHASE_B_ANGLE       = 0xE021;
    public static final int PHASE_C_ANGLE       = 0xE022;
    public static final int VTA_VTB_ANGLE       = 0xE023;
    public static final int VTA_VTC_ANGLE       = 0xE024;

    // ─── Power Factor & Frequency ─────────────────────────────────────────────
    public static final int POWER_FACTOR        = 0xE026;
    public static final int FREQUENCY           = 0xE060;

    // ─── Active Power (W) ─────────────────────────────────────────────────────
    public static final int PHASE_A_WATTS       = 0xE030;
    public static final int PHASE_B_WATTS       = 0xE031;
    public static final int PHASE_C_WATTS       = 0xE032;
    public static final int P_TOTAL             = 0xE033;

    // ─── Reactive Power (VAR) ─────────────────────────────────────────────────
    public static final int PHASE_A_VARS        = 0xE040;
    public static final int PHASE_B_VARS        = 0xE041;
    public static final int PHASE_C_VARS        = 0xE042;
    public static final int Q_TOTAL             = 0xE043;

    // ─── Apparent Power (VA) ──────────────────────────────────────────────────
    public static final int PHASE_A_VA          = 0xE050;
    public static final int PHASE_B_VA          = 0xE051;
    public static final int PHASE_C_VA          = 0xE052;
    public static final int S_TOTAL             = 0xE053;

    // ─── Energy Import (kWh / kVARh) ──────────────────────────────────────────
    public static final int RATE_1_IMPORT_KWH   = 0x0060;
    public static final int RATE_2_IMPORT_KWH   = 0x0061;
    public static final int RATE_3_IMPORT_KWH   = 0x0062;
    public static final int TOTAL_IMPORT_KWH    = 0x0069;
    public static final int TOTAL_IMPORT_KVAR   = 0x0269;

    // ─── Energy Export (kWh / kVARh) ──────────────────────────────────────────
    public static final int RATE_1_EXPORT_KWH   = 0x0160;
    public static final int RATE_2_EXPORT_KWH   = 0x0161;
    public static final int RATE_3_EXPORT_KWH   = 0x0162;
    public static final int TOTAL_EXPORT_KWH    = 0x0169;
    public static final int TOTAL_EXPORT_KVAR   = 0x0369;

    // ─── Demand ───────────────────────────────────────────────────────────────
    public static final int MAX_DEMAND_KWH_IMPORT = 0x1069;
    public static final int MAX_DEMAND_KWH_EXPORT = 0x1169;

    // ─── THD ──────────────────────────────────────────────────────────────────
    public static final int THD_CURRENT_A       = 0x9000;
    public static final int THD_CURRENT_B       = 0x9100;
    public static final int THD_CURRENT_C       = 0x9200;
    public static final int THD_VOLTAGE_A       = 0x9300;
    public static final int THD_VOLTAGE_B       = 0x9400;
    public static final int THD_VOLTAGE_C       = 0x9500;

    private EdmiRegisters() {}
}
