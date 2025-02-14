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

import com.javierllorente.jobs.entity.OBSRequest;
import com.javierllorente.jobs.entity.OBSResult;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class RequestViewerController implements Initializable, TableSetter<OBSResult> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
    private BrowserController browserController;
    
    @FXML
    private TabPane tabPane;
    
    @FXML
    Label idLabel;
    
    @FXML
    Label creatorLabel;
    
    @FXML
    Label sourceLabel;
    
    @FXML
    Label targetLabel;
    
    @FXML
    Label dateLabel;
    
    @FXML
    TextFlow textFlow;
    
    @FXML
    TextArea commentsTextArea;
    
    @FXML
    Button closeButton;
    
    @FXML
    BuildResultsController buildResultsController;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }
    
    public void setRequest(OBSRequest request) {
        idLabel.setText(request.getId());
        creatorLabel.setText(request.getCreator());
        sourceLabel.setText(request.getSource());
        targetLabel.setText(request.getTarget());
        dateLabel.setText(dateFormat.format(request.getDate()));        
    }
    
    public void setDiff(String diffStr) {
        String[] diff = diffStr.split("\n");
        
        for (String line : diff) {
            Text text = new Text();
            
            if (line.startsWith("+")) {
                text.setFill(Color.BLUE);
            } else if (line.startsWith("-") || line.startsWith("delete")) {
                text.setFill(Color.RED);
            }
            
            text.setText(line + "\n");
            textFlow.getChildren().add(text);
        }       
    }

    @Override
    public void setAll(List<OBSResult> items) {
        buildResultsController.setAll(items);
    }    
    
    @FXML
    private void handleAccept() {
        browserController.startChangeRequestTask(idLabel.getText(),
                commentsTextArea.getText(), true);
        handleClose();
    }
    
    @FXML
    private void handleCancel() {
        browserController.startChangeRequestTask(idLabel.getText(),
                commentsTextArea.getText(), false);
        handleClose();
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    public void removeBuildResultsTab() {
        tabPane.getTabs().remove(1);
    }
    
}
