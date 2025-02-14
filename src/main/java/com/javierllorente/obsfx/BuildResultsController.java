/*
 * Copyright (C) 2025 Javier Llorente <javier@opensuse.org>
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

import com.javierllorente.jobs.entity.OBSResult;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class BuildResultsController implements Initializable {
    
    @FXML
    private TableView<OBSResult> table;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        table.getColumns().get(0).setCellValueFactory(
                new PropertyValueFactory<>("repository")
        );
        table.getColumns().get(1).setCellValueFactory(
                new PropertyValueFactory<>("arch")
        );
        TableColumn<OBSResult, String> statusColumn
                = (TableColumn<OBSResult, String>) table.getColumns().get(2);
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
    
    public BooleanProperty visibleProperty() {
        return table.visibleProperty();
    }
    
    public ReadOnlyObjectProperty<OBSResult> selectedItemProperty() {
        return table.getSelectionModel().selectedItemProperty();
    }
    
    public OBSResult getSelectedItem() {
        return table.getSelectionModel().getSelectedItem();
    }
    
    public boolean hasSelection() {
        return (table.getSelectionModel().getSelectedIndex() != -1);
    }
    
    public void setAll(List<OBSResult> results) {
        table.getItems().setAll(results);
        table.sort();
    }
    
    public void clear() {
        table.getItems().clear();
    }
    
}
