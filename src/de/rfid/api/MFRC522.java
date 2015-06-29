package de.rfid.api;

import com.pi4j.wiringpi.Spi;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MFRC522 {
    public static final int MAX_LEN = 16;
    public static final byte PICC_REQIDL = 0x26;
    public static final byte PCD_TRANSCEIVE = 0x0C;
    public static final byte PCD_AUTHENT = 0x0E;
    public static final byte PCD_IDLE = 0x00;
    public static final byte PCD_RESETPHASE = 0x0F;
    public static final byte PCD_CALCCRC = 0x03;
    public static final byte PICC_ANTICOLL = (byte) 0x93;
    public static final byte PICC_SELECTTAG = (byte) 0x93;
    public static final byte PICC_AUTHEN1A = 0x60;
    public static final byte PICC_AUTHEN1B = 0x61;
    public static final byte PICC_READ = 0x30;
    public static final byte PICC_WRITE = (byte) 0xA0;
    public static final byte FIFOLevelReg = 0x0A;
    public static final byte FIFODataReg = 0x09;
    public static final byte CommIEnReg = 0x02;
    public static final byte CommIrqReg = 0x04;
    public static final byte CommandReg = 0x01;
    public static final byte ErrorReg = 0x06;
    public static final byte ControlReg = 0x0C;
    public static final byte Status2Reg = 0x08;
    public static final byte TModeReg = 0x2A;
    public static final byte TPrescalerReg = 0x2B;
    public static final byte TReloadRegH = 0x2C;
    public static final byte TReloadRegL = 0x2D;
    public static final byte TxAutoReg = 0x15;
    public static final byte ModeReg = 0x11;
    public static final byte TxControlReg = 0x14;
    public static final byte CRCResultRegM = 0x21;
    public static final byte CRCResultRegL = 0x22;
    public static final byte DivIrqReg = 0x05;
    public static final byte BitFramingReq = 0x0D;
    public static final int MI_OK = 0;
    public static final int MI_NOTAGERR = 1;
    public static final int MI_ERR = 2;

    public static byte[] defaultKey = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    public MFRC522() {
        this.init();
    }

    public void init() {
        int fd = Spi.wiringPiSPISetup(Spi.CHANNEL_0, 10000000);
        if (fd <= -1) {
            System.out.println(" ==>> SPI SETUP FAILED");
            return;
        }
        MFRC522_Reset();
        write_MFRC522(TModeReg, (byte) 0x8D);
        write_MFRC522(TPrescalerReg, (byte) 0x3E);
        write_MFRC522(TReloadRegL, (byte) 0x30);
        write_MFRC522(TReloadRegH, (byte) 0x00);
        write_MFRC522(TxAutoReg, (byte) 0x40);
        write_MFRC522(ModeReg, (byte) 0x3D);
        antennaOn();
    }

    public void antennaOn() {
        byte temp = read_MFRC522(TxControlReg);
        if (0 != (~((temp & 0x03)))) {
            setBitMask(TxControlReg, (byte) 0x03);
        }
    }

    public void antennaOff() {
        clearBitMask(TxControlReg, (byte) 0x03);
    }

    public void MFRC522_Reset() {
        write_MFRC522(CommandReg, PCD_RESETPHASE);
    }

    public StatusTagType MFRC522_Request(byte regMode) {
        int status = 0;
        ByteArrayOutputStream tagType = new ByteArrayOutputStream();
        write_MFRC522(BitFramingReq, (byte) 0x07);
        tagType.write(regMode);
        StatusBackDataBackBits statusBackDataBackLen = MFRC522_ToCard(PCD_TRANSCEIVE, tagType.toByteArray());
        if (((statusBackDataBackLen.status != MI_OK) | (statusBackDataBackLen.backBits != 0x10))) {
            status = MI_ERR;
        }
        return new StatusTagType(status, tagType.toByteArray());
    }

    public StatusBackDataBackBits MFRC522_ToCard(byte command, byte[] sendData) {
        ByteArrayOutputStream backData = new ByteArrayOutputStream();
        int backLen = 0;
        int status = MI_ERR;
        byte irqEn = 0x00;
        byte waitIRq = 0x00;
        byte lastBits;
        int n;
        int i;
        if (command == PCD_AUTHENT) {
            irqEn = 0x12;
            waitIRq = 0x10;
        }
        if (command == PCD_TRANSCEIVE) {
            irqEn = 0x77;
            waitIRq = 0x30;
        }
        write_MFRC522(CommIEnReg, (byte) (irqEn | 0x80));
        clearBitMask(CommIrqReg, (byte) 0x80);
        setBitMask(FIFOLevelReg, (byte) 0x80);
        write_MFRC522(CommandReg, PCD_IDLE);
        for (byte b : sendData) {
            write_MFRC522(FIFODataReg, b);
        }
        write_MFRC522(CommandReg, command);
        if (command == PCD_TRANSCEIVE) {
            setBitMask(BitFramingReq, (byte) 0x80);
        }
        i = 2000;
        while (true) {
            n = read_MFRC522(CommIrqReg);
            i = i - 1;
            if (!((i != 0) && !(0 != (n & 0x01)) && !(0 != (n & waitIRq)))) {
                break;
            }
        }
        clearBitMask(BitFramingReq, (byte) 0x80);
        if (i != 0) {
            if ((read_MFRC522(ErrorReg) & 0x1B) == 0x00) {
                status = MI_OK;
                if ((n & irqEn & 0x01) != 0) {
                    status = MI_NOTAGERR;
                }
                if (command == PCD_TRANSCEIVE) {
                    n = read_MFRC522(FIFOLevelReg);
                    lastBits = (byte) (read_MFRC522(ControlReg) & 0x07);
                    if (lastBits != 0) {
                        backLen = (n - 1) * 8 + lastBits;
                    } else {
                        backLen = n * 8;
                    }
                    if (n == 0) {
                        n = 1;
                    }
                    if (n > MAX_LEN) {
                        n = MAX_LEN;
                    }
                    i = 0;
                    while (i < n) {
                        i = i + 1;
                        backData.write(read_MFRC522(FIFODataReg));
                    }
                }
            }
        } else {
            status = MI_ERR;
        }
        return new StatusBackDataBackBits(status, backData.toByteArray(), backLen);
    }

    public StatusSerNum MFRC22_Anticoll() {
        int status;
        byte[] backData;
        int serNumCheck = 0;
        ByteArrayOutputStream serNum = new ByteArrayOutputStream();
        write_MFRC522(BitFramingReq, (byte) 0x00);
        serNum.write(PICC_ANTICOLL);
        serNum.write(0x20);
        StatusBackDataBackBits statusBackDataBackBits = MFRC522_ToCard(PCD_TRANSCEIVE, serNum.toByteArray());
        status = statusBackDataBackBits.status;
        backData = statusBackDataBackBits.backData;
        if (status == MI_OK) {
            int i = 0;
            if (backData.length == 5) {
                while (i < 4) {
                    serNumCheck = serNumCheck ^ backData[i];
                    i = i + 1;
                }
                if (serNumCheck != backData[i]) {
                    status = MI_ERR;
                }
            } else {
                status = MI_ERR;
            }
        }
        return new StatusSerNum(status, backData);
    }

    public byte MFRC522_SelectTag(byte[] serNum) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(PICC_SELECTTAG);
        buf.write(0x70);
        int i = 0;
        while (i < 5) {
            buf.write(serNum[i]);
            i = i + 1;
        }
        byte[] pOut = calculateCRC(buf.toByteArray());
        buf.write(pOut[0]);
        buf.write(pOut[1]);

        StatusBackDataBackBits statusBackDataBackBits = MFRC522_ToCard(PCD_TRANSCEIVE, buf.toByteArray());

        if (statusBackDataBackBits.status == MI_OK && statusBackDataBackBits.backBits == 0x18) {
            return statusBackDataBackBits.backData[0];
        } else {
            return 0;
        }
    }

    public byte[] calculateCRC(byte[] pInData) {
        clearBitMask(DivIrqReg, (byte) 0x40);
        setBitMask(FIFOLevelReg, (byte) 0x80);
        ByteArrayOutputStream pOutData = new ByteArrayOutputStream();
        int i = 0;
        while (i < pInData.length) {
            write_MFRC522(FIFODataReg, pInData[i]);
            i = i + 1;
        }
        write_MFRC522(CommandReg, PCD_CALCCRC);
        i = 0xFF;
        while (true) {
            byte n = read_MFRC522(DivIrqReg);
            i = i - 1;
            if (!(((i != 0)) && !(0 != (n & 0x04)))) {
                break;
            }
        }
        pOutData.write(read_MFRC522(CRCResultRegL));
        pOutData.write(read_MFRC522(CRCResultRegM));
        return pOutData.toByteArray();
    }

    public int MFRC522_Auth(byte authMode, byte blockAddr, byte[] sectorKey, byte[] uid) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(authMode);
        buf.write(blockAddr);
        int i = 0;
        while (i < sectorKey.length) {
            buf.write(sectorKey[i]);
            i = i + 1;
        }
        for (byte b : uid) {
            buf.write(b);
        }
        StatusBackDataBackBits statusBackDataBackBits = MFRC522_ToCard(PCD_AUTHENT, buf.toByteArray());
        if (!(statusBackDataBackBits.status == MI_OK)) {
            System.out.println("AUTH ERROR!");
        }
        if (!(0 != (read_MFRC522(Status2Reg) & 0x08))) {
            System.out.println("AUTH ERROR (status2reg & 0x08) != 0");
        }
        return statusBackDataBackBits.status;
    }

    public void setBitMask(byte reg, byte mask) {
        byte tmp = read_MFRC522(reg);
        write_MFRC522(reg, (byte) (tmp | mask));
    }

    public void clearBitMask(byte reg, byte mask) {
        byte tmp = read_MFRC522(reg);
        write_MFRC522(reg, (byte) (tmp & (~mask)));
    }

    public byte read_MFRC522(byte addr) {
        byte[] packet = new byte[2];
        packet[0] = (byte) (((addr << 1) & 0x7E) | 0x80);
        packet[1] = 0;
        Spi.wiringPiSPIDataRW(Spi.CHANNEL_0, packet);
        return packet[1];
    }

    public byte[] MFRC522_Read(byte blockAddr) {
        ByteArrayOutputStream recvData = new ByteArrayOutputStream();
        recvData.write(PICC_READ);
        recvData.write(blockAddr);
        byte[] pOut = calculateCRC(recvData.toByteArray());
        recvData.write(pOut[0]);
        recvData.write(pOut[1]);
        StatusBackDataBackBits statusBackDataBackBits = MFRC522_ToCard(PCD_TRANSCEIVE, recvData.toByteArray());
        if (!(statusBackDataBackBits.status == MI_OK)) {
            System.out.println("ERROR while reading!");
        }
        if (statusBackDataBackBits.backData.length == 16) {
            return statusBackDataBackBits.backData;
        }
        return null;
    }

    public void MFRC522_Write(byte blockAddr, byte[] writeData) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(PICC_WRITE);
        buf.write(blockAddr);
        byte[] crc = calculateCRC(buf.toByteArray());
        buf.write(crc[0]);
        buf.write(crc[1]);
        StatusBackDataBackBits statusBackDataBackBits = MFRC522_ToCard(PCD_TRANSCEIVE, buf.toByteArray());
        int status = statusBackDataBackBits.status;
        if (!(statusBackDataBackBits.status == MI_OK) || !(statusBackDataBackBits.backBits == 4) || !((statusBackDataBackBits.backData[0] & 0x0F) == 0x0A)) {
            status = MI_ERR;
        }
        if (status == MI_OK) {
            int i = 0;
            buf = new ByteArrayOutputStream();
            while (i < 16) {
                buf.write(writeData[i]);
                i = i + 1;
            }
            crc = calculateCRC(buf.toByteArray());
            buf.write(crc[0]);
            buf.write(crc[1]);
            statusBackDataBackBits = MFRC522_ToCard(PCD_TRANSCEIVE, buf.toByteArray());
            if (!(statusBackDataBackBits.status == MI_OK) || !(statusBackDataBackBits.backBits == 4) || !((statusBackDataBackBits.backData[0] & 0x0F) == 0x0A)) {
                System.out.println("Error while writing");
            }
        }
    }

    public void write_MFRC522(byte addr, byte val) {
        byte[] packet = new byte[2];
        packet[0] = (byte) ((addr << 1) & 0x7E);
        packet[1] = val;
        Spi.wiringPiSPIDataRW(Spi.CHANNEL_0, packet);
    }

    public void MFRC522_StopCrypto1() {
        clearBitMask(Status2Reg, (byte) 0x08);
    }

    public List<byte[]> MFRC522_DumpClassic1K(byte[] key, byte[] uid) {
        List<byte[]> dataList = new ArrayList<>();
        int i = 0;
        while (i < 64) {
            int status = MFRC522_Auth(PICC_AUTHEN1A, (byte) i, key, uid);
            if (status == MI_OK) {
                dataList.add(MFRC522_Read((byte) i));
            } else {
                System.out.println("Authentification error");
            }
            i = i + 1;
        }
        MFRC522_StopCrypto1();
        return dataList;
    }

    public static byte[] serNumToUID(byte[] serNum) {
        if(serNum.length > 0) {
            ByteArrayOutputStream uid = new ByteArrayOutputStream();
            int i = 0;
            while (i < 4) {
                uid.write(serNum[i]);
                i = i + 1;
            }
            return uid.toByteArray();
        }
        return null;
    }

    public class StatusBackDataBackBits {
        public int status;
        public byte[] backData;
        public int backBits;

        public StatusBackDataBackBits(int status, byte[] backData, int backBits) {
            this.status = status;
            this.backData = backData;
            this.backBits = backBits;
        }
    }

    public static String convertHexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            String output = hex.substring(i, (i + 2));
            int decimal = Integer.parseInt(output, 16);
            sb.append((char) decimal);
            temp.append(decimal);
        }
        return sb.toString();
    }

    public class StatusSerNum {
        public int status;
        public byte[] backData;

        public StatusSerNum(int status, byte[] backData) {
            this.status = status;
            this.backData = backData;
        }
    }

    public class StatusTagType {
        public int status;
        public byte[] tagType;

        public StatusTagType(int status, byte[] tagType) {
            this.status = status;
            this.tagType = tagType;
        }
    }
}

