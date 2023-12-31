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

import com.javierllorente.jobs.entity.OBSPerson;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class BookmarksController implements Initializable {
    
    private static final Logger logger = Logger.getLogger(BookmarksController.class.getName());
    private String prj;
    private String pkg;
    private int initialSize;
    private BrowserController browserController;
    private OBSPerson person;
    
    @FXML
    private MenuButton bookmarksButton;
    
    @FXML
    private MenuItem addBookmarkItem;
    
    @FXML
    private MenuItem deleteBookmarkItem;
    
    @FXML
    private SeparatorMenuItem bookmarkSeparator;

    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // initalSize = add + delete + separator items (hidden); 
        // does not count empty item (to be deleted on addBookmarks())
        initialSize = bookmarksButton.getItems().size() - 1;
        
        addBookmarkItem.setOnAction((t) -> {
           logger.info("clicked!");
           String item = browserController.locationTextField.getText();
           MenuItem entry = new MenuItem(item);
           entry.setOnAction((ActionEvent e) -> {
                logger.log(Level.INFO, "Bookmark clicked: {0}", item);
                browserController.goTo(item);
            });
           bookmarksButton.getItems().add(entry);
           person.addWatchItem(browserController.locationTextField.getText());
           updatePerson();
        });
        deleteBookmarkItem.setOnAction((t) -> {
            for (MenuItem item : bookmarksButton.getItems()) {
                if (item.getText() != null && item.getText()
                        .equals(browserController.locationTextField.getText())) {
                    bookmarksButton.getItems().remove(item);
                    person.removeWatchItem(item.getText());
                    updatePerson();
                    break;
                }
            }
            
        });
        
        bookmarksButton.setOnShowing((Event t) -> {            
            if (prj == null) {
                return;
            }
            toggle(prj + ((pkg != null && !pkg.isEmpty()) ? "/" + pkg : ""));
        });
    }

    private void updatePerson() {
        try {
            App.getOBS().updatePerson(person);
        } catch (ParserConfigurationException | TransformerException | IOException | SAXException ex) {
            Logger.getLogger(BookmarksController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    public void setPrj(String prj) {
        this.prj = prj;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public void addBookmarks(OBSPerson person) {
        this.person = person;
        bookmarksButton.getItems().remove(initialSize, bookmarksButton.getItems().size());
        
        for (String item : person.getWatchList()) {
            MenuItem entry = new MenuItem(item);
            entry.setOnAction((ActionEvent t) -> {
                logger.log(Level.INFO, "Bookmark clicked: {0}", item);
                browserController.goTo(item);
            });
            bookmarksButton.getItems().add(entry);
        }
    }
    
    private void toggle(String location) {
        logger.log(Level.INFO, "location = {0}", location);
        boolean found = false;

        for (int i = initialSize; i < bookmarksButton.getItems().size(); i++) {
            String item = bookmarksButton.getItems().get(i).getText();
            logger.log(Level.INFO, "str: {0} - {1}", new Object[]{item, location});
            
            if (item.equals(location)) {
                found = true;
                break;
            }
        }
        
        addBookmarkItem.setVisible(!found);
        deleteBookmarkItem.setVisible(found);
        bookmarkSeparator.setVisible(true);
    }
    
    public void showBookmarks() {
        if (bookmarksButton.isShowing()) {
            bookmarksButton.hide();
        } else {
            bookmarksButton.show();
        }
    }
    
    public void clear() {
        bookmarksButton.getItems().clear();
    }
}
