/*
 * Copyright (C) 2023-2025 Javier Llorente <javier@opensuse.org>
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

import com.javierllorente.jobs.entity.OBSMetaConfig;
import com.javierllorente.jobs.entity.OBSPackage;
import com.javierllorente.jobs.entity.OBSResult;
import com.javierllorente.jobs.entity.OBSRevision;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class OverviewController extends DataController implements Initializable, 
        TableSetter<OBSResult> {

    @FXML
    private Label title;
    
    @FXML
    private Hyperlink link;
    
    @FXML
    private Label description;
    
    @FXML
    private Button viewLogButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Button downloadButton;
    
    @FXML
    private FontIcon historyIcon;
    
    @FXML
    private Label packagesLabel;
    
    @FXML
    private Label packages;
    
    @FXML
    private Label latestRevision;
    
    @FXML
    private BuildResultsController buildResultsController;    

    private StringProperty projectProperty;
    private StringProperty packageProperty;
    private StringProperty packageCountProperty;
    private final SimpleDateFormat dateFormat;
    private BrowserController browserController;
    private LogViewerController logViewerController;

    public OverviewController() {
        dateFormat = new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss");
    }
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        projectProperty = new SimpleStringProperty();
        packageProperty = new SimpleStringProperty();
        packageCountProperty = new SimpleStringProperty();
        
        viewLogButton.disableProperty().bind(packageProperty.isNull()
                .or(buildResultsController.selectedItemProperty().isNull()));        
        refreshButton.disableProperty().bind((packageProperty.isNull()));
        downloadButton.disableProperty().bind(packageProperty.isNull());
        link.managedProperty().bind(link.textProperty().isNotEmpty());
        buildResultsController.visibleProperty().bind(packageProperty.isNotNull());
        
        packagesLabel.visibleProperty().bind(projectProperty.isNotNull()
                .and(packageProperty.isNull()));
        packages.visibleProperty().bind(projectProperty.isNotNull()
                .and(packageProperty.isNull()));
        packages.textProperty().bind(packageCountProperty);
        
        historyIcon.visibleProperty().bind(packageProperty.isNotNull());
        latestRevision.visibleProperty().bind(packageProperty.isNotNull());
    }
    
    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }
    
    public void setMetaConfig(OBSMetaConfig metaConfig) {        
        String configTitle = metaConfig.getTitle();
        if (configTitle.isBlank()) {
            configTitle = metaConfig.getName();
        }
        title.setText(configTitle);
        
        if (metaConfig instanceof OBSPackage pkgMetaConfig) {            
            if (pkgMetaConfig.getUri() != null) {
                link.setText(pkgMetaConfig.getUri().toString());
                link.setOnAction((t) -> {
                    browserController.getHostServices().showDocument(link.getText());
                });
            } else {
                link.setText("");
            }
            projectProperty.set(pkgMetaConfig.getProject().getName());
            packageProperty.set(metaConfig.getName());
        } else {
            projectProperty.set(metaConfig.getName());
            packageProperty.set(null);
        }
        
        String configDescription = metaConfig.getDescription();
        if (configDescription.isBlank()) {
            configDescription = App.getBundle().getString("overview.nodescription");
        }        
        description.setText(configDescription);
        dataLoaded = true;
    }
    
    public void setBuildLog(String buildLog) {
        logViewerController.setText(buildLog);
    }

    public StringProperty packageCountProperty() {
        return packageCountProperty;
    }
    
    public void setLatestRevision(OBSRevision revision) {
        latestRevision.setText(dateFormat.format(revision.getTime()));
    }
    
    public void clearLatestRevision() {
        latestRevision.setText(null);
    }
    
    public boolean hasBuildResultSelection() {
        return buildResultsController.hasSelection();
    }
    
    @FXML
    public void handleViewLog() {
        try {
            FXMLLoader fXMLLoader = App.getFXMLLoader("LogViewer");
            Scene scene = new Scene(fXMLLoader.load(), 800, 600);
            logViewerController = fXMLLoader.getController();
            Stage stage = new Stage();
            stage.getIcons().add(new Image(App.class.getResourceAsStream(App.ICON)));
            stage.setTitle(App.NAME + " - " + MessageFormat.format(App.getBundle()
                    .getString("logviewer.title"), packageProperty.get()));
            stage.setScene(scene);
            stage.show();
            OBSResult result = buildResultsController.getSelectedItem();
            browserController.startBuildLogTask(result.getProject(), result.getRepository(),
                    result.getArch(), packageProperty.get());
        } catch (IOException | RuntimeException ex) {
            browserController.showExceptionAlert(ex);
            Logger.getLogger(OverviewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    public void handleRefresh() {
        if (projectProperty.get() == null || packageProperty.get() == null) {
            return;
        }
        buildResultsController.clear();
        browserController.startBuildResultsTask(projectProperty.get(), packageProperty.get(), this);
    }
    
    @FXML
    public void handleDownload() {
        browserController.getHostServices()
                .showDocument("https://software.opensuse.org/download.html?project=" 
                        + projectProperty.get() + "&package=" + packageProperty.get());
    }

    @Override
    public void setAll(List<OBSResult> items) {
        buildResultsController.setAll(items);
    }
    
    public String getPrj() {
        return projectProperty.get();
    }
    
    public String getPkg() {
        return packageProperty.get();
    }
    
    public boolean isEmpty() {
        return packageProperty.get() == null;
    }
    
    public void clearPkgData() {
        packageProperty.set(null);
        link.setText(null);
        clearLatestRevision();
        buildResultsController.clear();
    }
    
    public void clear() {
        title.setText(null);
        description.setText(null);
        projectProperty.set(null);
        clearPkgData();
        dataLoaded = false;
    }
    
    public void toggleButtons(boolean visible) {
        viewLogButton.setVisible(visible);
        refreshButton.setVisible(visible);
        downloadButton.setVisible(visible);
    }
    
}
