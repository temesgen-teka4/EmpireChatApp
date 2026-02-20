module com.example.javachatapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop; // <--- ADD THIS LINE

    opens com.example.javachatapp to javafx.graphics, javafx.fxml;
    exports com.example.javachatapp;
}