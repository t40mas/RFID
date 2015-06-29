package de.rfid.rmi;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIServer {
    public static void main(String[] args) throws RemoteException, AlreadyBoundException {
        System.setProperty("java.rmi.server.hostname", "192.168.100.102");
        RemoteImpl remoteImpl = new RemoteImpl();
        Registry registry = LocateRegistry.createRegistry(RemoteInterface.RMI_PORT);
        registry.bind(RemoteInterface.RMI_ID, remoteImpl);
        System.out.println("RMIServer started");
    }
}
