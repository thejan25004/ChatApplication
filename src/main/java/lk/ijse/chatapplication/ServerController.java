package lk.ijse.chatapplication;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerController {

    @FXML
    private TextArea serverTextArea;

    @FXML
    private TextField serverTextField;

    @FXML
    private ImageView serverImageView;

    @FXML
    private TextArea serverFileTextArea;


    ServerSocket serverSocket;
    Socket socket;
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;

    private void showFileDialog(){
        String filePath = "src/main/resources/lk/ijse/chatapplication/files/received_file.txt";
        File file = new File(filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            serverFileTextArea.clear();
            serverFileTextArea.setText(content.toString());
            System.out.println("File content displayed in TextArea.");
        } catch (IOException e) {
            e.printStackTrace();
            serverFileTextArea.setText("Failed to load file: " + e.getMessage());
        }
    }

    public void initialize() {
        try {
            serverSocket = new ServerSocket(3001);
            socket = serverSocket.accept();
            System.out.println("client Accept");

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        new Thread(() -> {
            try {
                while (true) {
                    if (dataInputStream != null) {
                        String dataType = dataInputStream.readUTF();

                        if (dataType.equals("text")){
                            String outPutMessage = dataInputStream.readUTF();
                            serverTextArea.appendText("\nClient : " + outPutMessage);
                        } else if (dataType.equals("image")){
                            int imageSize = dataInputStream.readInt();

                            byte[] imageBytes = new byte[imageSize];
                            dataInputStream.readFully(imageBytes);

                            String filePath = "src/main/resources/lk/ijse/chatapplication/files/received_image.jpg";
                            FileOutputStream fos = new FileOutputStream(filePath);
                            fos.write(imageBytes);
                            fos.close();

                            File imageFile = new File(filePath);
                            if (imageFile.exists()) {
                                Image image = new Image(imageFile.toURI().toString());

                                Platform.runLater(() -> {
                                    serverImageView.setImage(image);
                                    System.out.println("Image displayed in ImageView.");
                                });
                            } else {
                                System.out.println("Image file not found at: " + filePath);
                            }
                        } else if (dataType.equals("file")) {
                            int fileSize = dataInputStream.readInt();

                            String filePath = "src/main/resources/lk/ijse/chatapplication/files/received_file.txt";

                            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                int totalBytesRead = 0;

                                if (totalBytesRead < fileSize) {
                                    bytesRead = dataInputStream.read(buffer);

                                    if (bytesRead == -1) {
                                        System.out.println("Unexpected end of stream. Total bytes read: " + totalBytesRead);
                                        break;
                                    }

                                    fos.write(buffer, 0, bytesRead);
                                    totalBytesRead += bytesRead;
                                    System.out.println("Bytes read: " + bytesRead + ", Total bytes read: " + totalBytesRead);
                                    showFileDialog();
                                    fos.close();
                                }

                                System.out.println("File successfully saved at: " + filePath);

                                if (totalBytesRead < fileSize) {
                                    System.out.println("Warning: File transfer incomplete. Expected: " + fileSize + ", Received: " + totalBytesRead);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                serverFileTextArea.setText("Failed to save file: " + e.getMessage());
                                return;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error receiving or displaying image: " + e.getMessage());
            }
        }).start();
    }




    @FXML
    void serverSendBtnOnAction(ActionEvent event) {
        try {

            String command = serverTextField.getText();
            serverTextArea.appendText("\nServer : " + command);

            dataOutputStream.writeUTF("text");
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void serverSendFileBtnOnAction(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a File");

            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );

            File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

            if (selectedFile != null) {
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());

                FileInputStream fileInputStream = new FileInputStream(selectedFile.getAbsolutePath());

                byte[] buffer = new byte[4096];
                int bytesRead;

                dataOutputStream.writeUTF("file");

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dataOutputStream.write(buffer, 0, bytesRead);
                }
                dataOutputStream.flush();

                System.out.println("File sent successfully");

            } else {
                System.out.println("No file selected.");
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    void serverSendImageBtnOnAction(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select an Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

            if (selectedFile != null) {
                String imagePath = selectedFile.getAbsolutePath();
                System.out.println("Selected image path: " + imagePath);

                FileInputStream fis = new FileInputStream(imagePath);
                byte[] imageBytes = fis.readAllBytes();

                dataOutputStream.writeUTF("image");
                dataOutputStream.writeInt(imageBytes.length);
                dataOutputStream.flush();

                dataOutputStream.write(imageBytes);
                dataOutputStream.flush();

                fis.close();
                serverTextArea.appendText("\nImage sent to client.");
            } else {
                System.out.println("No file was selected.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            serverTextArea.appendText("\nError sending image: " + e.getMessage());
        }
    }

}
