package de.rfid.cam_pi;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Created by thomas on 09.06.15.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("START PROGRAM");
        long start = System.currentTimeMillis();
        try
        {

            Process p = Runtime.getRuntime().exec("raspistill -t 2000 -o image.jpg");
            BufferedInputStream bis = new BufferedInputStream(p.getInputStream());

            System.out.println("start writing");

            bis.close();

        }
        catch (IOException ieo)
        {
            ieo.printStackTrace();
        }
        System.out.println("END PROGRAM");
        System.out.println("Duration in ms: " + (System.currentTimeMillis() - start));
    }
}

