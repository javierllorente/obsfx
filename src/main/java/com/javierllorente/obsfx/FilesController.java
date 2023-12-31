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

import com.javierllorente.jobs.entity.OBSFile;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class FilesController implements Initializable {
    
    private String pkg;

    @FXML
    private TableView<OBSFile> filesTable;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initFileTableColumns();
    }
    
    private void initFileTableColumns() {
        filesTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("name"));
        filesTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("size"));
        filesTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("lastModified"));

        TableColumn<OBSFile, Date> lastModifiedColumn
                = (TableColumn<OBSFile, Date>) filesTable.getColumns().get(2);
        lastModifiedColumn.setCellFactory((TableColumn<OBSFile, Date> p) -> {
            TableCell<OBSFile, Date> tableCell = new TableCell<>() {
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
    }
    
    public void set(List<OBSFile> files) {
        filesTable.getItems().setAll(files);
        filesTable.sort();
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }    
    
    public void clear() {
        filesTable.getItems().clear();
        pkg = null;
    }
    
    public boolean isEmpty() {
        return filesTable.getItems().isEmpty();
    }    
}
