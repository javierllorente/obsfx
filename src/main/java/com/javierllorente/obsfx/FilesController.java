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

import com.javierllorente.jobs.entity.OBSEntry;
import com.javierllorente.obsfx.adapter.FileAdapter;
import com.javierllorente.obsfx.alert.ConfirmAlert;
import com.javierllorente.obsfx.util.Utils;
import jakarta.ws.rs.ClientErrorException;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class FilesController extends DataController implements Initializable {
    
    private String prj;
    private String pkg;
    private BrowserController browserController;

    @FXML
    private Button downloadButton;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private TableView<FileAdapter> filesTable;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initFileTableColumns();
        
        BooleanBinding noSelection = filesTable.getSelectionModel().selectedItemProperty().isNull();
        downloadButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
        
        filesTable.setOnDragOver((t) -> {
            if (t.getGestureSource() != filesTable && t.getDragboard().hasFiles()) {
                t.acceptTransferModes(TransferMode.COPY);
            }
            t.consume();
        });
        
        filesTable.setOnDragDropped((t) -> {
            Dragboard dragBoard = t.getDragboard();
            boolean success = false;
            if (dragBoard.hasFiles()) {
                dragBoard.getFiles().forEach((file) -> {
                    browserController.startUploadTask(prj, pkg, file);                    
                });                
                success = true;
            }
            t.setDropCompleted(success);
            t.consume();
        });
        
        filesTable.setOnKeyPressed((t) -> {
            if (t.getCode() == KeyCode.DELETE) {
                handleDelete();
            }
        });
    }
    
    private void initFileTableColumns() {
        filesTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("name"));
        filesTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("size"));
        filesTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("lastModified"));

        TableColumn<FileAdapter, String> sizeColumn
                = (TableColumn<FileAdapter, String>) filesTable.getColumns().get(1);        
        sizeColumn.setComparator((t, t1) -> {
            long bytes1 = Utils.humanReadableFormatToBytes(t);
            long bytes2 = Utils.humanReadableFormatToBytes(t1);
            return Long.compare(bytes1, bytes2);
        });
        
        TableColumn<FileAdapter, Date> lastModifiedColumn
                = (TableColumn<FileAdapter, Date>) filesTable.getColumns().get(2);
        lastModifiedColumn.setCellFactory((TableColumn<FileAdapter, Date> p) -> {
            TableCell<FileAdapter, Date> tableCell = new TableCell<>() {
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
        
        filesTable.setRowFactory((p) -> {
            TableRow<FileAdapter> row = new TableRow<>();
            final FileAdapter[] item = {null};

            row.setOnDragDetected((t) -> {
                Dragboard dragboard = row.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString("");
                dragboard.setContent(content);
                
                if (!row.isEmpty()) {
                    item[0] = row.getItem();
                    t.consume();
                }
            });            
            
            row.setOnDragDone((t) -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle(App.getBundle().getString("files.download.save_as"));
                fileChooser.setInitialFileName(item[0].getName());

                File destinationFile = fileChooser.showSaveDialog(App.getWindow());
                if (destinationFile != null) {
                    browserController.startDownloadTask(prj, pkg, item[0].getName(), destinationFile);
                }
                t.consume();
            });
            
            return row;
        });
    }
    
    @FXML
    public void handleDownload() {
        String fileName = filesTable.getSelectionModel().getSelectedItem().getName();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(App.getBundle().getString("files.download.save_as"));
        fileChooser.setInitialFileName(fileName);

        File destinationFile = fileChooser.showSaveDialog(App.getWindow());
        if (destinationFile != null) {
            browserController.startDownloadTask(prj, pkg, fileName, destinationFile);
        }
    }
    
    @FXML
    public void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(App.getBundle().getString("files.upload.select"));
  
        File selectedFile = fileChooser.showOpenDialog(App.getWindow());
        if (selectedFile != null) {            
            browserController.startUploadTask(prj, pkg, selectedFile);
        }
    }
    
    @FXML
    public void handleDelete() {
        ConfirmAlert alert = new ConfirmAlert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(App.getWindow());
        alert.setTitle(App.getBundle().getString("files.delete.title"));
        alert.setHeaderText(App.getBundle().getString("files.delete.header_text"));
        alert.setContentText(filesTable.getSelectionModel().getSelectedItem().getName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            FileAdapter selectedItem = filesTable.getSelectionModel().getSelectedItem();
            try {
                browserController.deleteFile(prj, pkg, selectedItem.getName());
                filesTable.getItems().remove(selectedItem);
            } catch (ClientErrorException ex) {
                browserController.showExceptionAlert(ex);
            }
        }
    }
    
    public void set(List<OBSEntry> files) {
        files.stream().map(FileAdapter::new).forEach(filesTable.getItems()::add);
        filesTable.sort();
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
    
    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }
    
    public void clear() {
        filesTable.getItems().clear();
        prj = null;
        pkg = null;
        dataLoaded = false;
    }
    
    public boolean isEmpty() {
        return filesTable.getItems().isEmpty();
    } 
    
}
