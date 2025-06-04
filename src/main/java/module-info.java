module com.fa1_3 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.fa1_3 to javafx.fxml;
    exports com.fa1_3;
}
