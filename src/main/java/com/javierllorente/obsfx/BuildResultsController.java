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
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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
                -> new ReadOnlyStringWrapper((cellData.getValue().getStatus() == null) 
                        ? ""
                        : cellData.getValue().getStatus().getCode()));
        
        statusColumn.setCellFactory((TableColumn<OBSResult, String> p) -> {
            return new TableCell<OBSResult, String>() {
                private Color statusColor = Color.BLACK;

                {
                    table.focusedProperty().addListener((ov, t, t1) -> {
                        updateColor();
                    });
                    table.getSelectionModel().getSelectedIndices().addListener(
                            (ListChangeListener.Change<? extends Integer> change) -> {
                                updateColor();
                            });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        statusColor = (switch (item) {
                            case "succeeded" ->
                                Color.GREEN;
                            case "failed", "unresolvable", "broken" ->
                                Color.RED;
                            case "disabled" ->
                                Color.GRAY;
                            default ->
                                Color.BLACK;
                        });

                        setText(item);
                        updateColor();
                    }
                }

                private void updateColor() {
                    TableRow<OBSResult> row = getTableRow();
                    boolean selected = row != null && row.isSelected();
                    boolean focused = table.isFocused();
                    setTextFill(selected ? (focused ? Color.WHITE : Color.BLACK) : statusColor);
                }
            };
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
