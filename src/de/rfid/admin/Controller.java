package de.rfid.admin;


import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import de.rfid.rmi.RemoteInterface;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    Registry registry;
    RemoteInterface remoteInterface;

    @FXML
    ImageView imageView;
    @FXML
    Button takePicture;
    @FXML
    TextField surname;
    @FXML
    TextField firstName;
    @FXML
    TextField rights;
    @FXML
    TableView<TableItem> tableView;
    @FXML
    Button saveButton;
    @FXML
    Button deleteButton;
    @FXML
    TextField uid;
    @FXML
    ToggleButton activateRecognizing;
    @FXML
    Label lastLogin;
    @FXML
    ImageView lastLoginImageView;

    Image image;
    ObservableList<TableItem> tableData;
    byte[] imageByte;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.load(new File("/usr/local/share/OpenCV/java/libopencv_java2411.dylib").getAbsolutePath());
        tableData = FXCollections.observableArrayList();
        tableView.setItems(tableData);
        try {
            registry = LocateRegistry.getRegistry("192.168.100.102", RemoteInterface.RMI_PORT);
            remoteInterface = (RemoteInterface) registry.lookup(RemoteInterface.RMI_ID);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

        File imagesFolder = new File("images");
        if (imagesFolder.exists() && imagesFolder.list().length > 0) {
            String files[] = imagesFolder.list();
            for (String file : files) {
                if (!file.startsWith(".") && !file.startsWith("mycache")) {
                    String[] values = file.split("_");
                    System.out.println(file);
                    TableItem tableItem = new TableItem(new ImageView(new Image("file:///Users/thomas/Documents/Programming/workspaces/IntelliJ/RFID/images/" + file)), values[0], values[1], values[2], values[3].split(".png")[0]);
                    tableData.add(tableItem);
                }
            }
        }

        Platform.runLater(() -> surname.requestFocus());

        setdummyPicture();

        takePicture.setOnAction(event -> {
            try {
                surname.setText("");
                firstName.setText("");
                uid.setText("");
                rights.setText("");
                imageByte = remoteInterface.getImageFromCamAdmin();

                FileOutputStream fileOutputStream = new FileOutputStream("image.png");
                fileOutputStream.write(imageByte);

                CascadeClassifier faceDetector = new CascadeClassifier("haarcascade_frontalface_alt.xml");
                MatOfRect faceDetections = new MatOfRect();
                Mat imageMat = Highgui.imread("image.png");
                Mat imageScaled = new Mat();
                Imgproc.resize(imageMat, imageScaled, new Size(125, 150));
                faceDetector.detectMultiScale(imageScaled, faceDetections);
                Mat face_cropped = imageMat.submat(faceDetections.toArray()[0]);
                Mat resized = new Mat();
                Imgproc.resize(face_cropped, resized, new Size(125, 150));
                Highgui.imwrite("croppedResized.png", resized);
                this.imageByte = Files.readAllBytes(Paths.get("croppedResized.png"));
                ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
                BufferedImage read = ImageIO.read(bis);
                image = SwingFXUtils.toFXImage(read, null);
                imageView.setImage(image);
                uid.setText(HexBin.encode(remoteInterface.getCardUID()));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        saveButton.setOnAction(event -> {
            try {
                String uid = HexBin.encode(remoteInterface.getCardUID());
                File file = new File("images/" + surname.getText().toUpperCase() + "_" + firstName.getText().toUpperCase() + "_" + uid + "_" + rights.getText().toUpperCase() + ".png");
                if (!file.exists() && !uid.equals("") && !surname.getText().equals("") && !firstName.getText().equals("") && !rights.getText().equals("") && !this.uid.getText().equals("")) {
                    tableData.add(new TableItem(new ImageView(image), surname.getText(), firstName.getText(), uid, rights.getText()));
                    File theDir = new File("images");
                    if (!theDir.exists()) {
                        theDir.mkdir();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream("images/" + surname.getText().toUpperCase() + "_" + firstName.getText().toUpperCase() + "_" + uid + "_" + rights.getText().toUpperCase() + ".png");
                    fileOutputStream.write(imageByte);
                    surname.setText("");
                    firstName.setText("");
                    rights.setText("");
                    this.uid.setText("");
                    setdummyPicture();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (observable.getValue() != null) {
                surname.setText(observable.getValue().getSurname());
                firstName.setText(observable.getValue().getFirstName());
                rights.setText(observable.getValue().getRights());
                uid.setText(observable.getValue().getUid());
                imageView.setImage(new Image("file:////Users/thomas/Documents/Programming/workspaces/IntelliJ/RFID/images/" + observable.getValue().getSurname() + "_" + observable.getValue().getFirstName() + "_" + observable.getValue().getUid() + "_" + observable.getValue().getRights() + ".png"));
            }
        });

//        tableView.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
//            if (newPropertyValue) {
//                deleteButton.setDisable(false);
//            } else {
//                deleteButton.setDisable(true);
//            }
//        });

        deleteButton.setOnAction(event -> {
            try {
                Files.delete(Paths.get("/Users/thomas/Documents/Programming/workspaces/IntelliJ/RFID/images/" + tableView.getSelectionModel().getSelectedItem().getSurname() + "_" + tableView.getSelectionModel().getSelectedItem().getFirstName() + "_" + tableView.getSelectionModel().getSelectedItem().getUid() + "_" + tableView.getSelectionModel().getSelectedItem().getRights() + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            tableData.remove(tableView.getSelectionModel().getSelectedItem());
        });

        activateRecognizing.setOnAction(event -> {
            new Thread(() -> {
                while (activateRecognizing.isSelected()) {
                    try {
                        byte[] uid = remoteInterface.getCardUID();
//                        ByteArrayInputStream bis = new ByteArrayInputStream(remoteInterface.getImageFromCam());
//                        BufferedImage read = ImageIO.read(bis);
                        Platform.runLater(()->{
                            lastLogin.setText(HexBin.encode(uid));
                            for(TableItem t : tableData){
                                if(t.getUid().equals(HexBin.encode(uid))){
                                    lastLoginImageView.setImage(t.imageView.getImage());
                                    break;
                                } else if (!t.getUid().equals(HexBin.encode(uid))) {
                                    lastLoginImageView.setImage(new Image("file:////Users/thomas/Documents/Programming/workspaces/IntelliJ/RFID/dummy.jpg"));
                                }
                            }
                        });

                        Thread.sleep(1000);
                    } catch (InterruptedException | ArrayIndexOutOfBoundsException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        });
    }

    void setdummyPicture() {
        image = new Image("file:////Users/thomas/Documents/Programming/workspaces/IntelliJ/RFID/dummy.jpg");
        imageView.setImage(image);
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        try {
            ImageIO.write(bImage, "jpg", s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageByte = s.toByteArray();

    }

    public static class TableItem {
        private final SimpleStringProperty surname;
        private final SimpleStringProperty firstName;
        private final SimpleStringProperty uid;
        private final SimpleStringProperty rights;
        private final ImageView imageView;

        public TableItem(ImageView imageView, String surname, String firstName, String uid, String rights) {
            this.surname = new SimpleStringProperty(surname);
            this.firstName = new SimpleStringProperty(firstName);
            this.uid = new SimpleStringProperty(uid);
            this.rights = new SimpleStringProperty(rights);
            this.imageView = imageView;
        }

        public String getSurname() {
            return surname.get();
        }

        public SimpleStringProperty surnameProperty() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname.set(surname);
        }

        public String getFirstName() {
            return firstName.get();
        }

        public SimpleStringProperty firstNameProperty() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName.set(firstName);
        }

        public String getUid() {
            return uid.get();
        }

        public SimpleStringProperty uidProperty() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid.set(uid);
        }

        public String getRights() {
            return rights.get();
        }

        public SimpleStringProperty rightsProperty() {
            return rights;
        }

        public void setRights(String rights) {
            this.rights.set(rights);
        }

        public ImageView getImageView() {
            imageView.fitHeightProperty().set(40);
            imageView.fitWidthProperty().set(40);
            return imageView;
        }
    }
}