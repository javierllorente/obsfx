module com.javierllorente.obsfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires java.logging;
    requires java.prefs;
    requires java.xml;
    requires jakarta.ws.rs;
    requires com.javierllorente.jobs;
    requires org.kordamp.ikonli.javafx;
    requires org.controlsfx.controls;
    requires org.fxmisc.richtext;

    opens com.javierllorente.obsfx to javafx.fxml;
    exports com.javierllorente.obsfx;
}