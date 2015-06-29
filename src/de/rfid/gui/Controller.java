package de.rfid.gui;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import de.rfid.api.MFRC522;
import de.rfid.rmi.RemoteInterface;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    Registry registry;
    RemoteInterface remoteInterface;

    @FXML
    Button buttonRead;
    @FXML
    TableView<TableItem> tableView;
    @FXML
    Label labelUID;
    @FXML
    TextField textFieldWriteSector;
    @FXML
    TextField textFieldWriteData;
    @FXML
    Button buttonWrite;
    @FXML
    Label labelHexLength;
    @FXML
    Button buttonConnect;
    @FXML
    TextField textFieldIP;
    @FXML
    Circle circleStatus;
    @FXML
    TextField textFieldText;
    @FXML
    Button buttonConvertTextToHex;
    @FXML
    Button buttonDisconnect;

    byte[] currentUID;

    URL location;
    ResourceBundle resources;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.location = location;
        this.resources = resources;
        ObservableList<TableItem> tableData = FXCollections.observableArrayList();
        tableView.setItems(tableData);
        buttonRead.setDisable(true);
        buttonWrite.setDisable(true);
        circleStatus.setFill(Color.RED);
        textFieldIP.setDisable(false);
        buttonConnect.setDisable(false);
        buttonDisconnect.setDisable(true);

        buttonConnect.setOnAction(event -> {
            try {
                registry = LocateRegistry.getRegistry(textFieldIP.getText(), RemoteInterface.RMI_PORT);
                remoteInterface = (RemoteInterface) registry.lookup(RemoteInterface.RMI_ID);
//                currentUID = remoteInterface.getCardUID();
                buttonConnect.setDisable(true);
                textFieldIP.setDisable(true);
                circleStatus.setFill(Color.GREEN);
                buttonRead.setDisable(false);
                buttonWrite.setDisable(false);
                buttonConnect.setDisable(true);
                buttonDisconnect.setDisable(false);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        });

        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (observable.getValue() != null) {
                textFieldWriteSector.setText(observable.getValue().getSector().toString());
                textFieldWriteData.setText(observable.getValue().getData());
            }
        });

        buttonRead.setOnAction(event -> {
            try {
                byte[] uid = remoteInterface.getCardUID();
                currentUID = uid;
                labelUID.setText(HexBin.encode(uid));
                List<byte[]> dataList = remoteInterface.getDumpClassic1K();
                tableData.clear();
                for (int i = 0; i < dataList.size(); i++) {
                    tableData.add(new TableItem((i), HexBin.encode(dataList.get(i)), MFRC522.convertHexToString(HexBin.encode(dataList.get(i)))));
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        buttonWrite.setOnAction(event -> {
            try {
                if (textFieldWriteData.getLength() == 32 && textFieldWriteData.getText().matches("[0-9A-F]+") && Arrays.equals(currentUID, remoteInterface.getCardUID())) {
                    remoteInterface.writeSectorData(Integer.valueOf(textFieldWriteSector.getText()), textFieldWriteData.getText());
                    List<byte[]> dataList = remoteInterface.getDumpClassic1K();
                    tableData.clear();
                    for (int i = 0; i < dataList.size(); i++) {
                        tableData.add(new TableItem((i), HexBin.encode(dataList.get(i)), MFRC522.convertHexToString(HexBin.encode(dataList.get(i)))));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        buttonDisconnect.setOnAction(event -> initialize(this.location, this.resources));

        textFieldWriteData.textProperty().addListener((observable, oldValue, newValue) -> {
            labelHexLength.setText(String.valueOf(textFieldWriteData.getText().length()));
        });

        buttonConvertTextToHex.setOnAction(event -> {
            byte[] text = textFieldText.getText().getBytes(Charset.forName("UTF-8"));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byteArrayOutputStream.write(text);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (text.length < 16) {
                int count = 16 - text.length;
                for (int i = 0; i < count; i++) {
                    byteArrayOutputStream.write(0);
                }
            }
            textFieldWriteData.setText(HexBin.encode(byteArrayOutputStream.toByteArray()));
        });
    }

    public static class TableItem {
        private final SimpleIntegerProperty sector;
        private final SimpleStringProperty data;
        private final SimpleStringProperty dataString;

        public TableItem(Integer sector, String data, String dataString) {
            this.sector = new SimpleIntegerProperty(sector);
            this.data = new SimpleStringProperty(data);
            this.dataString = new SimpleStringProperty(dataString);
        }

        public Integer getSector() {
            return sector.get();
        }

        public void setSector(Integer sector) {
            this.sector.set(sector);
        }

        public String getData() {
            return data.get();
        }

        public void setData(String data) {
            this.data.set(data);
        }

        public String getDataString() {
            return dataString.get();
        }

        public void setDataString(String dataString) {
            this.dataString.set(dataString);
        }
    }
}
