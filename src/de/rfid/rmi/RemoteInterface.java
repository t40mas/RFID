package de.rfid.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteInterface extends Remote {
    String RMI_ID = "RMI_ID";
    int RMI_PORT = 1337;
    int[] forbidden_sectors = new int[]{0, 3, 7, 11, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51, 55, 59, 63};

    byte[] getCardUID() throws RemoteException;
    List<byte[]> getDumpClassic1K() throws RemoteException;
    void writeSectorData(int sector, String stringData) throws IOException;
    byte[] getImageFromCamAdmin() throws IOException, InterruptedException;
    byte[] getImageFromCam() throws IOException, InterruptedException;
}
