/*
 * Copyright (C) 2023-2024 Javier Llorente <javier@opensuse.org>
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
import com.javierllorente.jobs.entity.OBSPkgMetaConfig;
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
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class OverviewController implements Initializable {

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
    private Label latestRevision;
    
    @FXML
    private TableView<OBSResult> buildResultsTable;

    StringProperty projectProperty;
    StringProperty packageProperty;
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
        initBuildResultsColumns();
        projectProperty = new SimpleStringProperty();
        packageProperty = new SimpleStringProperty();
        
        viewLogButton.disableProperty().bind(packageProperty.isNull()
                .or(buildResultsTable.getSelectionModel().selectedItemProperty().isNull()));        
        refreshButton.disableProperty().bind((packageProperty.isNull()));
        downloadButton.disableProperty().bind(packageProperty.isNull());
        link.managedProperty().bind(link.textProperty().isNotEmpty());
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
        
        if (metaConfig instanceof OBSPkgMetaConfig pkgMetaConfig) {            
            if (pkgMetaConfig.getUrl() != null) {
                link.setText(pkgMetaConfig.getUrl().toString());
                link.setOnAction((t) -> {
                    browserController.getHostServices().showDocument(link.getText());
                });
            } else {
                link.setText("");
            }
            projectProperty.set(pkgMetaConfig.getProject());
        }        
        
        String configDescription = metaConfig.getDescription();
        if (configDescription.isBlank()) {
            configDescription = App.getBundle().getString("overview.nodescription");
        }        
        description.setText(configDescription);
        
        packageProperty.set(metaConfig.getName());
    }
    
    public void setBuildLog(String buildLog) {
        logViewerController.setText(buildLog);
    }
    
    public void setLatestRevision(OBSRevision revision) {
        latestRevision.setText(dateFormat.format(revision.getTime()));
        historyIcon.setVisible(true);
    }
    
    public void clearLatestRevision() {
        latestRevision.setText(null);
        historyIcon.setVisible(false);
    }
    
    public boolean isBuildResultSelected() {
        return (buildResultsTable.getSelectionModel().getSelectedIndex() != -1);
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
            OBSResult result = buildResultsTable.getSelectionModel().getSelectedItem();
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
        buildResultsTable.getItems().clear();
        browserController.startBuildResultsTask(projectProperty.get(), packageProperty.get());
    }
    
    @FXML
    public void handleDownload() {
        browserController.getHostServices()
                .showDocument("https://software.opensuse.org/download.html?project=" 
                        + projectProperty.get() + "&package=" + packageProperty.get());
    }
    
    private void initBuildResultsColumns() {
        buildResultsTable.getColumns().get(0).setCellValueFactory(
                new PropertyValueFactory<>("repository")
        );
        buildResultsTable.getColumns().get(1).setCellValueFactory(
                new PropertyValueFactory<>("arch")
        );
        TableColumn<OBSResult, String> statusColumn
                = (TableColumn<OBSResult, String>) buildResultsTable.getColumns().get(2);
        statusColumn.setCellValueFactory(cellData
                -> new ReadOnlyStringWrapper(cellData.getValue().getStatus().getCode()));

        statusColumn.setCellFactory((TableColumn<OBSResult, String> p) -> {
            TableCell<OBSResult, String> tableCell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        Color color = Color.BLACK;                        
                        switch (item) {
                            case "succeeded":
                                color = Color.GREEN;
                                break;
                            case "failed":
                            case "unresolvable":
                            case "broken":
                                color = Color.RED;
                                break;
                            case "disabled":
                                color = Color.GRAY;
                                break;
                        }                        
                        setText(item);
                        setTextFill(color);
                    }
                }
                
            };
            
            return tableCell;
        });        
    }

    public void setResults(List<OBSResult> results) {
        buildResultsTable.getItems().setAll(results);
        buildResultsTable.sort();
    }

    public String getPkg() {
        return packageProperty.get();
    }
    
    public boolean isEmpty() {
        return packageProperty.get() == null;
    }
    
    public void clear() {
        projectProperty.set(null);
        packageProperty.set(null);
        title.setText(null);
        link.setText(null);
        description.setText(null);
        historyIcon.setVisible(false);
        latestRevision.setText(null);
        buildResultsTable.getItems().clear();
    }

}
