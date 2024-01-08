/*
 * Copyright (C) 2024 Javier Llorente <javier@opensuse.org>
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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class LogViewerController implements Initializable {

    @FXML
    TextArea textArea;
    
    @FXML
    TextField searchTextField;
    
    @FXML
    Button previousButton;
    
    @FXML
    Button nextButton;
    
    @FXML
    Label notFoundLabel;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        searchTextField.textProperty().addListener((o) -> {
            previousButton.setDisable(false);
            nextButton.setDisable(false);
            notFoundLabel.setVisible(false);
        });
    }
    
    public void setText(String text) {
        Platform.runLater(() -> {
            textArea.setText(text);
            textArea.positionCaret(textArea.getLength());
            searchTextField.requestFocus();
        });
    }
    
    @FXML    
    private void findPrevious() {
        String log = textArea.getText();
        String searchText = searchTextField.getText();

        if (log == null || log.isEmpty()) {
            textArea.selectRange(0,0);
            return;
        }

        IndexRange selection = textArea.getSelection();
        int end = (selection == null) || (selection.getStart() == 0) 
                ? log.length() : selection.getStart();
        int searchTextStart = log.substring(0, end).lastIndexOf(searchText);
        boolean found = (searchTextStart != -1);
        previousButton.setDisable(!found);
        nextButton.setDisable(false);
        notFoundLabel.setVisible(!found);
                
        if (!found) {
            textArea.selectRange(end, end);
            return;
        }

        textArea.selectRange(searchTextStart, searchTextStart + searchText.length());
    }
    
    @FXML
    private void findNext() {
        String log = textArea.getText();
        String searchText = searchTextField.getText();

        if (log == null || log.isEmpty()) {
            textArea.selectRange(0, 0);
            return;
        }

        IndexRange selection = textArea.getSelection();
        int start = (selection != null) ? selection.getEnd() : 0;
        int searchTextStart = log.indexOf(searchText, start);        
        boolean found = (searchTextStart != -1);
        previousButton.setDisable(false);
        nextButton.setDisable(!found);
        notFoundLabel.setVisible(!found);
         
        if (!found) {
            textArea.selectRange(start, start);
            return;
        }

        textArea.selectRange(searchTextStart, searchTextStart + searchText.length());
    }
    
}
