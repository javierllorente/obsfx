/*
 * Copyright (C) 2020-2025 Javier Llorente <javier@opensuse.org>
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 *
 * @author javier
 */
public class SettingsDialog extends Dialog<Map<String, String>> {
    
    private TextField apiUriField;
    private TextField homepageField;

    public SettingsDialog(Window window, Preferences preferences) {
        super();
        initOwner(window);
        setTitle(App.getBundle().getString("settings"));
        setHeaderText(App.getBundle().getString("settings.header"));
        setGraphic(new FontIcon("icm-user"));
        
        getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        
        ButtonType saveButtonType = new ButtonType(App.getBundle().getString("settings.save"), 
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        ButtonBar buttonBar = (ButtonBar) getDialogPane().lookup(".button-bar");
        buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_WINDOWS);
                
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(30, 40, 30, 30));

        TextField nameField = new TextField();
        nameField.setMaxWidth(260.0);
        nameField.setText(preferences.get(App.USERNAME, ""));
        
        PasswordField passwordField = new PasswordField();
        passwordField.setMaxWidth(260.0);
        try {
            passwordField.setText(App.getAuthTokenEncryptor().decrypt(preferences.get(App.PASSWORD, "")));
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        apiUriField = new TextField();
        apiUriField.setPrefWidth(260.0);
        apiUriField.setText(preferences.get(App.API_URI, ""));
        
        homepageField = new TextField();
        homepageField.setPrefWidth(260.0);
        homepageField.setText(preferences.get(App.HOMEPAGE, ""));
        
        CheckBox autoLoginCheckBox = new CheckBox();
        autoLoginCheckBox.setSelected(preferences.getBoolean(App.AUTOLOGIN, false));
        
        CheckBox homeProjectsCheckBox = new CheckBox();
        homeProjectsCheckBox.setSelected(preferences.getBoolean(App.HOME_PROJECTS, false));

        gridPane.add(new Label(App.getBundle().getString("settings.username")), 0, 0);
        gridPane.add(nameField, 1, 0);
        gridPane.add(new Label(App.getBundle().getString("settings.password")), 0, 1);
        gridPane.add(passwordField, 1, 1);
        gridPane.add(new Label(App.getBundle().getString("settings.apiuri")), 0, 2);
        gridPane.add(apiUriField, 1, 2);
        gridPane.add(new Label(App.getBundle().getString("settings.homepage")), 0, 3);
        gridPane.add(homepageField, 1, 3);
        gridPane.add(new Label(App.getBundle().getString("settings.homeprojects")), 0, 4);
        gridPane.add(homeProjectsCheckBox, 1, 4);
        gridPane.add(new Label(App.getBundle().getString("settings.autologin")), 0, 5);
        gridPane.add(autoLoginCheckBox, 1, 5);
        
        getDialogPane().setContent(gridPane);
        setResizable(true); // FIXME: Workaround for JavaFX 11

        Platform.runLater(() -> {
            nameField.requestFocus();
            nameField.selectEnd();
        });
        
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Map<String, String> data = new HashMap<>();
                data.put(App.USERNAME, nameField.getText());
                try {
                    data.put(App.PASSWORD, App.getAuthTokenEncryptor().encrypt(passwordField.getText()));
                } catch (IllegalBlockSizeException | BadPaddingException ex) {
                    // FIXME: show ExceptionAlert
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
                data.put(App.API_URI, apiUriField.getText());
                data.put(App.HOMEPAGE, homepageField.getText());
                data.put(App.HOME_PROJECTS, String.valueOf(homeProjectsCheckBox.isSelected()));
                data.put(App.AUTOLOGIN, String.valueOf(autoLoginCheckBox.isSelected()));
                return data;
            }
            return null;
        });
    }
    
    public void focusApiUriField() {
        Platform.runLater(() -> {
            apiUriField.requestFocus();
            apiUriField.end();
        });
    }
    
}
