module lk.ijse.chatapplicationexample {
    requires javafx.controls;
    requires javafx.fxml;


    opens lk.ijse.chatapplication to javafx.fxml;
    exports lk.ijse.chatapplication;
}