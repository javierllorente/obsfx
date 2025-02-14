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

import com.javierllorente.jobs.entity.OBSRequest;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class RequestsController extends DataController implements Initializable {

    private String prj;
    private String pkg;
    private BrowserController browserController;
    private RequestViewerController requestViewerController;    
    
    @FXML
    private TableView<OBSRequest> requestsTable;
    
    @FXML
    private TextArea descriptionTextArea;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
    }
    
    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }
    
    private void viewItem() {
        try {
            OBSRequest request = requestsTable.getSelectionModel().getSelectedItem();
            FXMLLoader fXMLLoader = App.getFXMLLoader("RequestViewer");
            Parent root = fXMLLoader.load();
            Scene scene = new Scene(root);
            requestViewerController = fXMLLoader.getController();
            requestViewerController.setBrowserController(browserController);
            Stage stage = new Stage();
            stage.initOwner(App.getWindow());
            stage.getIcons().add(new Image(App.class.getResourceAsStream(App.ICON)));
            stage.setTitle(App.getBundle().getString("requestviewer.request") + " " 
                    + request.getId());
            stage.setScene(scene);
            stage.show();
            requestViewerController.setRequest(request);
            
            if (request.getActionType().equals("submit")) {
                browserController.startDiffTask(request.getId());
                browserController.startBuildResultsTask(request.getSourceProject(), 
                        request.getSourcePackage(), requestViewerController);
            } else {
                requestViewerController.setDiff(request.getActionType() + " " + request.getTarget());
                requestViewerController.removeBuildResultsTab();
            }
            
            
            
        } catch (IOException ex) {
            Logger.getLogger(RequestsController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void initTable() {
        requestsTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("created"));
        requestsTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("id"));
        requestsTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("source"));
        requestsTable.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("target"));
        requestsTable.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("creator"));
        requestsTable.getColumns().get(5).setCellValueFactory(new PropertyValueFactory<>("actionType"));
        requestsTable.getColumns().get(6).setCellValueFactory(new PropertyValueFactory<>("state"));

        TableColumn<OBSRequest, Date> createdColumn
                = (TableColumn<OBSRequest, Date>) requestsTable.getColumns().get(0);
        requestsTable.getSortOrder().add(createdColumn);

        createdColumn.setCellFactory((TableColumn<OBSRequest, Date> p) -> {
            TableCell<OBSRequest, Date> tableCell = new TableCell<>() {
                private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        setText(dateFormat.format(item));
                    }
                }
            };
            return tableCell;
        });
        
        requestsTable.setRowFactory((TableView<OBSRequest> p) -> {
            TableRow<OBSRequest> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem viewItem = new MenuItem("View details");
            viewItem.setOnAction((t) -> {
                viewItem();
            });
            contextMenu.getItems().addAll(viewItem);
            row.contextMenuProperty().bind(Bindings
                    .when(row.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(contextMenu));
            return row;
        });
        
        requestsTable.getSelectionModel().selectedItemProperty().addListener((ov, t, t1) -> {
            if (t1 != null) {
                descriptionTextArea.setText(t1.getDescription());
            }
        });
        
        requestsTable.setOnMouseClicked((t) -> {
            if (t.getClickCount() == 2) {
                viewItem();
            }
        });
    }
    
    public void set(List<OBSRequest> requests) {
        requestsTable.getItems().setAll(requests);
        requestsTable.sort();
        dataLoaded = true;
    }

    public String getPrj() {
        return prj;
    }

    public void setPrj(String prj) {
        this.prj = prj;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }
    
    public void clear() {
        prj = null;
        requestsTable.getItems().clear();
        descriptionTextArea.clear();
        dataLoaded = false;
    }
    
    public boolean isEmpty() {
        return requestsTable.getItems().isEmpty();
    }

    void setDiff(String diff) {
        requestViewerController.setDiff(diff);
    }

}
