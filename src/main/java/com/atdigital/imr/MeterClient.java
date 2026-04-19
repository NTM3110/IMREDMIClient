package com.atdigital.imr;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * EDMI meter client over serial port (RS-232 / RS-485) using IMR.Core.x64.dll via JNA.
 *
 * HOW TO RUN:
 *   1. Build IMR.Core in Release|x64 → copy IMR.Core.x64.dll to: IMRJavaClient/native/
 *   2. Run: gradle run
 *      Or:  java -Djna.library.path=native -jar build/libs/imr-java-client-1.0.0.jar
 *
 * List available ports:
 *   SerialPort.getCommPorts() — prints all ports on the machine.
 */
public class MeterClient {

    // ─── Change these to match your meter ────────────────────────────────────
    private static final String COM_PORT      = "COM3";     // e.g. COM1, COM3, COM5
    private static final int    BAUD_RATE     = 9600;       // typical EDMI baud: 9600 or 19200
    private static final int    DATA_BITS     = 8;
    private static final int    STOP_BITS     = SerialPort.ONE_STOP_BIT;
    private static final int    PARITY        = SerialPort.NO_PARITY;
    private static final int    READ_TIMEOUT  = 5_000;      // ms — per-byte read timeout

    private static final int    METER_SERIAL   = 251308613;
    private static final String METER_USERNAME = "EDMI";
    private static final String METER_PASSWORD = "IMDEIMDE";
    // ─────────────────────────────────────────────────────────────────────────

    private final ImrCoreLib lib = ImrCoreLib.INSTANCE;

    private SerialPort   port;
    private InputStream  in;
    private OutputStream out;

    // ─── Media callbacks ──────────────────────────────────────────────────────

    /**
     * READ_FROM_MEDIA_DELEGATE — called by native code to read ONE byte.
     * Writes the received byte into byteOut[0].
     */
    private final ReadFromMediaCallback reader = byteOut -> {
        try {
            int b = in.read();          // blocks until byte arrives or timeout
            if (b < 0) return MediaError.RESPONSE_TIMEOUT;
            byteOut.setByte(0, (byte) b);
            return MediaError.NONE;
        } catch (IOException e) {
            return MediaError.EXCEPTION;
        }
    };

    /**
     * WRITE_ALL_TO_MEDIA_DELEGATE — called by native code to send a full frame.
     * Uses Pointer (not byte[]) because JNA disallows byte[] in callback params.
     */
    private final WriteAllToMediaCallback writer = (buffer, length) -> {
        try {
            byte[] data = buffer.getByteArray(0, length);
            out.write(data, 0, length);
            out.flush();
            return MediaError.NONE;
        } catch (IOException e) {
            return MediaError.EXCEPTION;
        }
    };

    // ─── Serial port management ───────────────────────────────────────────────

    public void connect() {
        port = SerialPort.getCommPort(COM_PORT);
        port.setComPortParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY);

        // Blocking read with timeout — each in.read() waits up to READ_TIMEOUT ms
        port.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_BLOCKING,
            READ_TIMEOUT,
            0
        );

        if (!port.openPort()) {
            throw new RuntimeException("Failed to open " + COM_PORT
                + " — check the port name and that no other app is using it.");
        }

        in  = port.getInputStream();
        out = port.getOutputStream();

        System.out.printf("Serial port %s opened at %d baud%n", COM_PORT, BAUD_RATE);
    }

    public void disconnect() {
        if (port != null && port.isOpen()) {
            port.closePort();
            System.out.println("Serial port closed.");
        }
    }

    /** Print all COM ports available on this machine — useful for finding your port */
    public static void listPorts() {
        System.out.println("Available serial ports:");
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            System.out.println("  (none found)");
        }
        for (SerialPort p : ports) {
            System.out.printf("  %-10s — %s%n",
                p.getSystemPortName(), p.getDescriptivePortName());
        }
    }

    // ─── EDMI operations ──────────────────────────────────────────────────────

    public boolean login() {
        // IEC 62056-21 wake-up — required before EDMI login on serial meters.
        // IMR.WebApp does exactly this: serialMedia.WriteAll("/?!\r\n") then sleep 500ms.
        try {
            byte[] wakeUp = "/?!\r\n".getBytes();
            out.write(wakeUp);
            out.flush();
            Thread.sleep(500);
            // Drain any echo or garbage bytes the meter may send back
            while (in.available() > 0) in.read();
        } catch (Exception e) {
            System.out.println("Wake-up warning: " + e.getMessage());
        }

        byte[] username = METER_USERNAME.getBytes();
        byte[] password = METER_PASSWORD.getBytes();
        byte[] errCode  = new byte[1];

        int result = lib.EdmiLogin(
            reader, writer,
            METER_SERIAL,
            username, username.length,
            password, password.length,
            errCode
        );

        System.out.printf("EdmiLogin → mediaErr=%s, edmiErr=%d%n",
            MediaError.name(result), errCode[0] & 0xFF);
        return result == MediaError.NONE && errCode[0] == 0;
    }

    public void logout() {
        byte[] errCode = new byte[1];
        lib.EdmiLogout(reader, writer, METER_SERIAL, errCode);
        System.out.println("EdmiLogout done.");
    }

    /**
     * Read a single register.
     * Common EDMI register addresses:
     *   0x0001 = Meter serial number  (type 'L')
     *   0x0003 = Firmware version     (type 'A')
     *   0x0100 = Active energy import (type 'O' or 'U', unit 'X' = Wh)
     *   0x0101 = Active energy export
     *   0x0200 = Phase A voltage      (type 'F', unit 'V')
     *   0x0201 = Phase B voltage
     *   0x0202 = Phase C voltage
     *   0x0203 = Phase A current      (type 'F', unit 'A')
     */
    public EdmiRegister readRegister(int address) {
        EdmiRegister reg     = new EdmiRegister();
        byte[]       errCode = new byte[1];

        // CRITICAL: pre-set Type so DLL knows how to decode the response bytes.
        // The DLL does NOT read type from the wire — it uses reg->Type directly.
        // This mirrors how C# pre-sets EdmiType (e.g., EdmiType.FloatEnergy) in
        // EdmiRegister.CreatePhaseAVoltageRegister() etc.
        reg.Address = address;
        reg.Type    = EdmiRegisters.getType(address);
        reg.write(); // flush Java fields to native memory before DLL call

        int result = lib.EdmiReadRegister(reader, writer, METER_SERIAL, reg, errCode);
        System.out.printf("EdmiReadRegister(0x%04X) → mediaErr=%s, edmiErr=%d%n",
            address, MediaError.name(result), errCode[0] & 0xFF);

        if (result == MediaError.NONE && errCode[0] == 0) {
            reg.read(); // sync back from native memory
            System.out.printf("  value (type %c) = %s%n", (char)(reg.Type & 0xFF), reg.getFormattedValue());
        }
        return reg;
    }

    /**
     * Read multiple registers in one request (more efficient than calling
     * readRegister() in a loop — the meter processes them in a single transaction).
     */
    public EdmiRegister[] readRegisters(int... addresses) {
        // JNA requires Structure arrays to be contiguous in native memory.
        // toArray() allocates one block for all elements — new EdmiRegister[] does NOT.
        EdmiRegister   template = new EdmiRegister();
        EdmiRegister[] regs     = (EdmiRegister[]) template.toArray(addresses.length);
        byte[]         errCode  = new byte[1];
        for (int i = 0; i < addresses.length; i++) {
            regs[i].Address = addresses[i];
            regs[i].Type = EdmiRegisters.getType(addresses[i]);
            regs[i].write();
        }

        int result = lib.EdmiReadRegisters(reader, writer, METER_SERIAL, regs, regs.length, errCode);
        System.out.printf("EdmiReadRegisters(%d regs) → mediaErr=%s, edmiErr=%d%n",
            addresses.length, MediaError.name(result), errCode[0] & 0xFF);

        for (EdmiRegister reg : regs) {
            reg.read();
            char type = (char)(reg.Type & 0xFF);
            System.out.printf("  0x%04X type=%c err=%d", reg.Address, type, reg.ErrorCode & 0xFF);
            switch (type) {
                case 'F': case 'O': System.out.printf(" val=%.4f%n", reg.getFloat()); break;
                case 'D': case 'U': System.out.printf(" val=%.6f%n", reg.getDouble()); break;
                case 'L':           System.out.printf(" val=%d%n",   reg.getInt32()); break;
                case 'I':           System.out.printf(" val=%d%n",   reg.getInt16()); break;
                case 'A':           System.out.printf(" val=\"%s\"%n", reg.getString()); break;
                default:            System.out.println();
            }
        }
        return regs;
    }

    /**
     * Read load profile data between two timestamps.
     *
     * @param survey  EDMI survey code — e.g. 0x0305 for LS01 (30-min profile)
     *                Common: LS01=0x0305, LS02=0x0325, LS03=0x0345
     * @param from    start (use EdmiDateTime.nullDateTime() for "from beginning")
     * @param to      end   (use EdmiDateTime.nullDateTime() for "to end")
     */
    public void readProfile(short survey, EdmiDateTime.ByValue from, EdmiDateTime.ByValue to) {
        EdmiProfileSpec    profileSpec = new EdmiProfileSpec();
        PointerByReference fieldsRef   = new PointerByReference();
        byte[]             errCode     = new byte[1];

        System.out.printf("Reading profile from %04d-%02d-%02d %02d:%02d:%02d to %04d-%02d-%02d %02d:%02d:%02d%n",
            from.Year + 2000, from.Month, from.Day, from.Hour, from.Minute, from.Second,
            to.Year + 2000, to.Month, to.Day, to.Hour, to.Minute, to.Second);

        int result = lib.EdmiReadProfile(
            reader, writer, METER_SERIAL,
            survey, from, to,
            profileSpec, fieldsRef, errCode
        );

        System.out.printf("EdmiReadProfile(survey=0x%04X) → mediaErr=%s, edmiErr=%d%n",
            survey & 0xFFFF, MediaError.name(result), errCode[0] & 0xFF);

        if (result != MediaError.NONE || errCode[0] != 0) return;

        int rows     = profileSpec.RecordsCount;
        int channels = profileSpec.ChannelsCount & 0xFF;
        System.out.printf("  Profile '%s' | interval=%ds | rows=%d | channels=%d%n",
            profileSpec.getProfileName(), profileSpec.Interval, rows, channels);

        // Print channel header line
        System.out.print("  ");
        for (int c = 0; c < channels; c++) {
            System.out.printf("%-14s ", profileSpec.ChannelsInfo[c].getChannelName());
        }
        System.out.println();

        // Print channel types and scaling
        System.out.print("  ");
        for (int c = 0; c < channels; c++) {
            char type = (char)(profileSpec.ChannelsInfo[c].Type & 0xFF);
            float scale = profileSpec.ChannelsInfo[c].ScalingFactor;
            System.out.printf("%c:%.3f        ", type, scale);
        }
        System.out.println();

        // ── Native field array ─────────────────────────────────────────────────
        // Layout: [row0_ch0][row0_ch1]...[row1_ch0]... each slot is EDMI_FILE_FIELD.Value[25].
        final int FIELD_STRIDE  = 25;

        Pointer p = fieldsRef.getValue();
        if (p == null) return;

        int recordStride = FIELD_STRIDE * channels;
        final Instant base1996 = Instant.parse("1996-01-01T00:00:00Z");

        for (int r = 0; r < rows; r++) {
            System.out.print("  ");
            for (int c = 0; c < channels; c++) {
                long slotBase = (long)r * recordStride + (long)c * FIELD_STRIDE;
                char type = (char)(profileSpec.ChannelsInfo[c].Type & 0xFF);

                switch (type) {

                    case 'H': {
                        // HEX_SHORT: unsigned 16-bit LE at offset 0
                        byte[] raw = p.getByteArray(slotBase, 2);
                        System.out.printf("%-14d",
                            ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF);
                        break;
                    }

                    case 'T': {
                        byte[] raw = p.getByteArray(slotBase, 4);
                        long seconds = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
                        LocalDateTime ldt = LocalDateTime.ofInstant(base1996.plusSeconds(seconds), ZoneOffset.UTC);
                        System.out.printf("%04d-%02d-%02d %02d:%02d:%02d  ",
                            ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(),
                            ldt.getHour(), ldt.getMinute(), ldt.getSecond());
                        break;
                    }

                    case 'U': {
                        byte[] raw = p.getByteArray(slotBase, 8);
                        long rawValue = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getLong();
                        double scaledValue = rawValue * profileSpec.ChannelsInfo[c].ScalingFactor;
                        System.out.printf("%-14.4f",
                            scaledValue);
                        break;
                    }

                    case 'F': {
                        byte[] raw = p.getByteArray(slotBase, 4);
                        System.out.printf("%-14.4f",
                            ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getFloat());
                        break;
                    }

                    case 'O': {
                        byte[] raw = p.getByteArray(slotBase, 4);
                        long rawValue = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
                        double scaledValue = rawValue * profileSpec.ChannelsInfo[c].ScalingFactor;
                        System.out.printf("%-14.4f",
                            scaledValue);
                        break;
                    }

                    case 'D': {
                        byte[] raw = p.getByteArray(slotBase, 8);
                        System.out.printf("%-14.4f",
                            ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getDouble());
                        break;
                    }

                    case 'L': {
                        byte[] raw = p.getByteArray(slotBase, 4);
                        System.out.printf("%-14d",
                            ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getInt());
                        break;
                    }

                    case 'I': {
                        byte[] raw = p.getByteArray(slotBase, 2);
                        System.out.printf("%-14d",
                            (int) ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getShort());
                        break;
                    }

                    default:
                        System.out.printf("%-14s", "?");
                }
            }
            System.out.println();
        }

        // IMPORTANT: always release native-allocated memory
        lib.Release(p);
        System.out.println("  Profile memory released.");
    }

    // ─── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Uncomment to discover available COM ports:
        listPorts();

        ImrCoreLib.INSTANCE.Init();
        System.out.println("IMR.Core initialized.");

        MeterClient client = new MeterClient();
        client.connect();

        try {
            if (!client.login()) {
                System.out.println("Login failed — check COM port, baud rate, serial, credentials.");
                return;
            }


            // // ── Meter info (serial number uses type 'M' — not supported by DLL) ──

            client.readRegister(EdmiRegisters.METER_SERIAL_NUMBER);
            client.readRegister(EdmiRegisters.CURRENT_DATE);
            client.readRegister(EdmiRegisters.CURRENT_TIME);

            // ── Instantaneous values ──
            client.readRegister(EdmiRegisters.PHASE_A_VOLTAGE);
            client.readRegister(EdmiRegisters.PHASE_B_VOLTAGE);
            client.readRegister(EdmiRegisters.PHASE_C_VOLTAGE);
            client.readRegister(EdmiRegisters.PHASE_A_CURRENT);
            client.readRegister(EdmiRegisters.PHASE_B_CURRENT);
            client.readRegister(EdmiRegisters.PHASE_C_CURRENT);
            client.readRegister(EdmiRegisters.FREQUENCY);
            client.readRegister(EdmiRegisters.POWER_FACTOR);
            client.readRegister(EdmiRegisters.P_TOTAL);
            client.readRegister(EdmiRegisters.Q_TOTAL);

            // ── Energy ──
            client.readRegister(EdmiRegisters.TOTAL_IMPORT_KWH);
            client.readRegister(EdmiRegisters.TOTAL_EXPORT_KWH);
            client.readRegister(EdmiRegisters.TOTAL_IMPORT_KVAR);

            // ── CT / VT ratios ──
            client.readRegister(EdmiRegisters.CT_RATIO_PRIMARY);
            client.readRegister(EdmiRegisters.VT_RATIO_PRIMARY);

            short survey = 0x0325;
            // Query from 2026-01-01 to open-ended (null = "to end of available data")
            // This returns all records the DLL has buffered, including today's intervals.
            EdmiDateTime.ByValue from = new EdmiDateTime.ByValue(26, 4, 15, 0, 0, 0);
            EdmiDateTime.ByValue to = new EdmiDateTime.ByValue(26, 4, 16, 0, 0, 0);
            client.readProfile(survey, from, to);
        } finally {
            client.logout();
            client.disconnect();
        }
    }
}
