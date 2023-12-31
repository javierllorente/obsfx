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

import com.javierllorente.jobs.entity.OBSRequest;
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
import javafx.scene.text.Text;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class RequestsController implements Initializable {

    private String pkg;
    
    @FXML
    TableView<OBSRequest> requestsTable;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTableColumns();
    }
    
    private void initTableColumns() {
        requestsTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("created"));
        requestsTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("source"));
        requestsTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("target"));
        requestsTable.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("creator"));
        requestsTable.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("actionType"));
        requestsTable.getColumns().get(5).setCellValueFactory(new PropertyValueFactory<>("id"));

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
        
        TableColumn<OBSRequest, String> commentsColumn 
                = (TableColumn<OBSRequest, String>) requestsTable.getColumns().get(3);
        commentsColumn.setCellFactory((TableColumn<OBSRequest, String> p) -> {
            TableCell<OBSRequest, String> tableCell = new TableCell<>() {
                
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        Text text = new Text(item);
                        setGraphic(text);
                        text.wrappingWidthProperty().bind(getTableColumn().widthProperty().subtract(5));
                        text.fillProperty().bind(textFillProperty());
                    }
                }
            };
            return tableCell;
        });        
    }
    
    public void set(List<OBSRequest> requests) {
        requestsTable.getItems().setAll(requests);
        requestsTable.sort();
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }
    
    public void clear() {
        requestsTable.getItems().clear();
    }
    
    public boolean isEmpty() {
        return requestsTable.getItems().isEmpty();
    }

}
