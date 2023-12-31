/*
 * Copyright (C) 2023 Javier Llorente <javier@opensuse.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.javierllorente.obsfx.dialog;

import com.javierllorente.obsfx.App;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.util.Pair;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 *
 * @author javier
 */
public class LoginDialog extends Dialog<Pair<String, String>> {

    public LoginDialog(Window window, Preferences preferences) {
        super();
        initOwner(window);
        setTitle(App.getBundle().getString("login"));

        ButtonType loginButtonType = new ButtonType(App.getBundle().getString("login"), 
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        ButtonBar buttonBar = (ButtonBar) getDialogPane().lookup(".button-bar");
        buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_WINDOWS);        

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 40, 20, 40));

        TextField usernameField = new TextField();
        usernameField.setText(preferences.get(App.USERNAME, ""));
        usernameField.setPrefWidth(250.0);
        gridPane.add(new Label(App.getBundle().getString("settings.username")), 0, 0);
        gridPane.add(usernameField, 0, 1);
        
        PasswordField passwordField = new PasswordField();
        try {
            passwordField.setText(App.getAuthTokenEncryptor().decrypt(preferences.get(App.PASSWORD, "")));
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(LoginDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        passwordField.setPrefWidth(250.0);
        gridPane.add(new Label(App.getBundle().getString("settings.password")), 0, 2);
        gridPane.add(passwordField, 0, 3);

        Node loginButton = getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        loginButton.disableProperty().bind(Bindings.or(usernameField.textProperty().isEmpty(), 
                passwordField.textProperty().isEmpty()));
        getDialogPane().setContent(gridPane);

        Platform.runLater(() -> {
            if (usernameField.getText().isBlank()) {
                usernameField.requestFocus();
            } else {
                passwordField.requestFocus();
            }            
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(usernameField.getText(), passwordField.getText());
            }
            return null;
        });
    }    
}
