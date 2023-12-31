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
package com.javierllorente.obsfx;

import com.javierllorente.jobs.OBS;
import com.javierllorente.obsfx.crypto.AuthTokenEncryptor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ResourceBundle;
import javafx.scene.image.Image;

/**
 * JavaFX App
 */
public class App extends Application {
    
    public static final String NAME = "OBS FX";
    public static final String VERSION = "0.1";
    public static final int WINDOW_WIDTH = 1100;
    public static final int WINDOW_HEIGHT = 750;
    public static final String ICON = "/obs.png";
    
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String API_URI = "api_uri";
    public static final String AUTOLOGIN = "autologin"; 
    
    private final String applicationStyle = "style.css";
    private final String applicationBundle = getClass().getPackageName() + ".i18n.ApplicationBundle";
    
    private static Scene scene;
    private static AuthTokenEncryptor authTokenEncryptor;
    private static ResourceBundle bundle;
    private static OBS obs;
    
    @Override
    public void init() throws Exception {
        authTokenEncryptor = new AuthTokenEncryptor();
        bundle = ResourceBundle.getBundle(applicationBundle);
        obs = new OBS();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fXMLLoader = getFXMLLoader("Browser");
        scene = new Scene(fXMLLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(applicationStyle);
        BrowserController browserController = fXMLLoader.getController();
        browserController.setAccelerators(scene.getAccelerators());
        browserController.setHostServices(getHostServices());
        stage.getIcons().add(new Image(App.class.getResourceAsStream(ICON)));
        stage.setTitle(NAME);
        stage.setScene(scene);
        stage.show();
        browserController.autoLogin();
    }

    public static void setRoot(Parent parent) throws IOException {
        scene.setRoot(parent);
    }

    public static FXMLLoader getFXMLLoader(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"), bundle);
        return fxmlLoader;
    }
    
    public static AuthTokenEncryptor getAuthTokenEncryptor() {
        return authTokenEncryptor;
    }
    
    public static ResourceBundle getBundle() {
        return bundle;
    }
    
    public static OBS getOBS() {
        return obs;
    }

    public static void main(String[] args) {
        launch();
    }

}