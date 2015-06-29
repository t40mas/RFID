package de.rfid.cam;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    ImageView imageView;

    @FXML
    ToggleButton buttonOn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String OS = System.getProperty("os.name").toLowerCase();

        new Thread(() -> {
            if (OS.indexOf("mac") >= 0) {
                System.load(new File("/usr/local/share/OpenCV/java/libopencv_java2411.dylib").getAbsolutePath());
            } else if (OS.indexOf("nux") >= 0) {
                System.load(new File("/usr/local/share/OpenCV/java/libopencv_java2411.so").getAbsolutePath());
            } else {
                System.out.println("no operation system detected");
            }

            CascadeClassifier faceDetector = null;
            faceDetector = new CascadeClassifier(getClass().getResource("haarcascade_frontalface_alt.xml").getPath());

            MatOfRect faceDetections = new MatOfRect();
            VideoCapture videoCapture = new VideoCapture(0);

            while (true) {

                while (videoCapture.grab() && !buttonOn.isSelected()) {
                    Mat image = new Mat();

                    while (!videoCapture.read(image));

                    Mat imageScaled = new Mat();

                    Imgproc.resize(image, imageScaled, new Size(600, 400));

                    faceDetector.detectMultiScale(imageScaled, faceDetections);

                    for (Rect rect : faceDetections.toArray()) {
                        Core.rectangle(imageScaled, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
                                new Scalar(0, 255, 0));
                    }

                    Highgui.imwrite(getClass().getResource("").getPath() + "image.png", imageScaled);

                    Platform.runLater(() -> {
                        imageView.setImage(new Image("de/rfid/cam/image.png"));
                    });


                }
            }
        }).start();
    }
}
