package de.rfid.rmi;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import de.rfid.api.MFRC522;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RemoteImpl extends UnicastRemoteObject implements RemoteInterface {
    protected RemoteImpl() throws RemoteException {
        super();
    }

    @Override
    public byte[] getCardUID() {
        MFRC522 mfrc522 = new MFRC522();
        MFRC522.StatusTagType statusTagType = mfrc522.MFRC522_Request(MFRC522.PICC_REQIDL);
        MFRC522.StatusSerNum statusSerNum = mfrc522.MFRC22_Anticoll();
        if (statusTagType.status == 0 && statusSerNum.status == MFRC522.MI_OK) {
            System.out.println("Card detected " + HexBin.encode(MFRC522.serNumToUID(statusSerNum.backData)));
            mfrc522.MFRC522_SelectTag(statusSerNum.backData);
            int status = mfrc522.MFRC522_Auth(MFRC522.PICC_AUTHEN1A, (byte) 0x00, MFRC522.defaultKey, MFRC522.serNumToUID(statusSerNum.backData));
            if (status == MFRC522.MI_OK) {
                mfrc522.MFRC522_StopCrypto1();
            } else {
                System.out.println("Authentification error");
            }
        }
        return MFRC522.serNumToUID(statusSerNum.backData);
    }

    @Override
    public List<byte[]> getDumpClassic1K() throws RemoteException {
        MFRC522 mfrc522 = new MFRC522();
        MFRC522.StatusTagType statusTagType = mfrc522.MFRC522_Request(MFRC522.PICC_REQIDL);
        MFRC522.StatusSerNum statusSerNum = mfrc522.MFRC22_Anticoll();
        List<byte[]> dataList = null;
        if (statusTagType.status == 0 && statusSerNum.status == MFRC522.MI_OK) {
            mfrc522.MFRC522_SelectTag(statusSerNum.backData);
            dataList = mfrc522.MFRC522_DumpClassic1K(MFRC522.defaultKey, MFRC522.serNumToUID(statusSerNum.backData));
        }
        return dataList;
    }

    @Override
    public void writeSectorData(int sector, String stringData) throws IOException {
        boolean forbidden = false;
        for (int i : forbidden_sectors) {
            if (i == sector) {
                forbidden = true;
                break;
            }
        }

        if (!forbidden) {
            MFRC522 mfrc522 = new MFRC522();

            MFRC522.StatusTagType statusTagType = mfrc522.MFRC522_Request(MFRC522.PICC_REQIDL);
            MFRC522.StatusSerNum statusSerNum = mfrc522.MFRC22_Anticoll();
            if (statusTagType.status == MFRC522.MI_OK && statusSerNum.status == MFRC522.MI_OK) {
                System.out.println("Card detected " + HexBin.encode(MFRC522.serNumToUID(statusSerNum.backData)));
                mfrc522.MFRC522_SelectTag(statusSerNum.backData);
                int status = mfrc522.MFRC522_Auth(MFRC522.PICC_AUTHEN1A, (byte) sector, MFRC522.defaultKey, MFRC522.serNumToUID(statusSerNum.backData));

                if (status == MFRC522.MI_OK) {
                    System.out.println("Sector " + sector + " looked like this:");
                    System.out.println(HexBin.encode(mfrc522.MFRC522_Read((byte) sector)));

                    mfrc522.MFRC522_Write((byte) sector, HexBin.decode(stringData));
                    System.out.println("It now looks like this:");
                    System.out.println(HexBin.encode(mfrc522.MFRC522_Read((byte) sector)));
                    mfrc522.MFRC522_StopCrypto1();
                }
            }
        } else {
            System.out.println("Sector access denied");
        }
    }

    @Override
    public byte[] getImageFromCamAdmin() throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("raspistill --brightness 60 --colfx 128:128 -n --nopreview -t 250 -w 125 -h 150 -e png -o -");
        byte[] currentImage = IOUtils.toByteArray(p.getInputStream());

        return currentImage;
    }

    @Override
    public byte[] getImageFromCam() throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("raspistill --colfx 128:128 -t 3000 -w 125 -h 150 -e png -o -");
        p.waitFor(500, TimeUnit.MILLISECONDS);
        byte[] currentImage = IOUtils.toByteArray(p.getInputStream());

        return currentImage;
    }
}
