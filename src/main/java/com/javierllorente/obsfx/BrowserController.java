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
import com.javierllorente.jobs.entity.OBSPackage;
import com.javierllorente.jobs.entity.OBSPerson;
import com.javierllorente.jobs.entity.OBSProject;
import com.javierllorente.jobs.entity.OBSRequest;
import com.javierllorente.jobs.entity.OBSResult;
import com.javierllorente.jobs.entity.OBSRevision;
import com.javierllorente.jobs.entity.OBSStatus;
import com.javierllorente.obsfx.alert.ExceptionAlert;
import com.javierllorente.obsfx.alert.ShortcutsAlert;
import com.javierllorente.obsfx.dialog.LoginDialog;
import com.javierllorente.obsfx.dialog.SettingsDialog;
import com.javierllorente.obsfx.task.BuildLogTask;
import com.javierllorente.obsfx.task.BuildResultsTask;
import com.javierllorente.obsfx.task.ChangeRequestTask;
import com.javierllorente.obsfx.task.DiffTask;
import com.javierllorente.obsfx.task.DownloadTask;
import com.javierllorente.obsfx.task.FilesTask;
import com.javierllorente.obsfx.task.PackagesTask;
import com.javierllorente.obsfx.task.PkgMetaConfigTask;
import com.javierllorente.obsfx.task.PrjMetaConfigTask;
import com.javierllorente.obsfx.task.ProjectRequestsTask;
import com.javierllorente.obsfx.task.ProjectsTask;
import com.javierllorente.obsfx.task.PackageRequestsTask;
import com.javierllorente.obsfx.task.RevisionsTask;
import com.javierllorente.obsfx.task.SearchTask;
import com.javierllorente.obsfx.task.UploadTask;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServerErrorException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import javafx.util.Pair;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.textfield.TextFields;

/**
 * FXML Controller class
 *
 * @author javier
 */
public class BrowserController implements Initializable {

    private static final Logger logger = Logger.getLogger(BrowserController.class.getName());
    
    private Preferences preferences;
    private List<String> projects;
    private FilteredList<String> filteredPackages;
    private ObservableList<String> packagesObservableList;
    private ListProperty<String> packagesListProperty;
    private HostServices hostServices;
    private ObservableList<OBSPackage> searchResults;
    private boolean tabsChanged;
    private boolean loaded;
    private String currentProject;
    private String currentPackage;
    private String lastProject;
    private String lastPackage;
    private final AtomicInteger runningTasks = new AtomicInteger(0);

    @FXML
    private BorderPane borderPane;

    @FXML
    private BookmarksController bookmarksController;

    @FXML
    private TextField packageFilter;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private ListView<String> packagesListView;

    @FXML
    private TabPane tabPane;
    
    @FXML
    private Tab filesTab;
    
    @FXML
    private Tab revisionsTab;

    @FXML
    private OverviewController overviewController;

    @FXML
    private FilesController filesController;

    @FXML
    private RevisionsController revisionsController;

    @FXML
    private RequestsController requestsController;

    @FXML
    TextField locationTextField;
    
    @FXML
    TextField searchTextField;

    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        preferences = Preferences.userNodeForPackage(getClass());
        tabsChanged = false;
        loaded = false;
        currentProject = "";
        currentPackage = "";
        lastProject = "";
        lastPackage = "";
        
        initLocationTextField();
        initSearchTextField();
        initPackageListView();
        initTabPane();      

        Platform.runLater(() -> {
            locationTextField.requestFocus();
            locationTextField.end();
        });        

        bookmarksController.setBrowserController(this);
        overviewController.setBrowserController(this);
        filesController.setBrowserController(this);
        requestsController.setBrowserController(this);
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void initLocationTextField() {        
        var autoComplete = TextFields.bindAutoCompletion(locationTextField, (input) -> {
            return projects.stream()
                    .filter((p) -> p.toLowerCase().contains(input.getUserText().toLowerCase()))
                    .collect(Collectors.toCollection(ArrayList::new));
        });
        autoComplete.prefWidthProperty().bind(locationTextField.widthProperty());
        autoComplete.setVisibleRowCount(7);
    }
    
    private void initSearchTextField() {
        searchResults = FXCollections.observableArrayList();
        var autoComplete = TextFields.bindAutoCompletion(searchTextField, (input) -> {
            // Do not show results for previous query (results arrive late)
            return searchResults.stream()
                    .filter((p) -> p.getName().toLowerCase()
                            .contains(input.getUserText().toLowerCase()))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
        });

        autoComplete.setPrefWidth(searchTextField.getPrefWidth());
        autoComplete.setVisibleRowCount(7);
        autoComplete.setHideOnEscape(true);
        autoComplete.setDelay(700);
        
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!App.getOBS().isAuthenticated()) {
                return;
            }            
            if (!newValue.isBlank()) {
                startSearchTask(newValue.toLowerCase());
            } else {
                searchResults.clear();
            }
        });
    }

    private String getLocationProject() {
        String location = locationTextField.getText();
        if (location.contains("/")) {
            location = location.split("/", 2)[0];
        }
        return location;
    }
    
    private String getLocationPackage() {
        String location = locationTextField.getText();
        String pkg = "";
        if (location.contains("/")) {
            pkg = location.split("/", 2)[1].replace("/", "");
        }
        return pkg;
    }
    
    private void load(String location) {
        logger.log(Level.INFO, "location = {0}", location);
        
        if (!App.getOBS().isAuthenticated()) {
            showNotAuthenticatedAlert();
            return;
        }
        
        currentProject = getLocationProject();
        currentPackage = getLocationPackage();
        startPackagesTask();
        
        overviewController.setDataLoaded(false);
        requestsController.clear();
        
        if (getLocationPackage().isBlank()) {
            tabPane.getTabs().remove(filesTab);
            tabPane.getTabs().remove(revisionsTab);
            handleProjectTasks();
        }
        
        loaded = true;
    }
    
    public void goTo(String location) {
        lastProject = getLocationProject();
        lastPackage = getLocationPackage();
        
        locationTextField.setText(location);
        locationTextField.end();
        load(location);
        tabPane.requestFocus();
    }
    
    private void handleProjectTasks() {
        int tabIndex = tabPane.getSelectionModel().getSelectedIndex();
        logger.log(Level.INFO, "tabIndex = {0}", tabIndex);
        overviewController.clearPkgData();
        switch (tabIndex) {
            case 0 -> {
                startPrjMetaConfigTask(getLocationProject());
                tabsChanged = false;
            }
            case 1 -> {
                startProjectRequestsTask(getLocationProject());
            }
        }
    }

    private void initPackageListView() {
        packagesListView.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends String> ov,
                        String oldValue, String newValue) -> {
                    String selectedPackage = (newValue == null) ? "" : newValue;                    
                    currentPackage = selectedPackage;
                    
                    logger.log(Level.INFO, "locationPackage = {0}, selectedPackage = {1}", 
                            new Object[]{getLocationPackage(), selectedPackage});
                    bookmarksController.setPkg(selectedPackage);
                    overviewController.toggleButtons(!selectedPackage.isEmpty());                    

                    overviewController.setDataLoaded(false);
                    filesController.clear();
                    revisionsController.clear();
                    requestsController.setDataLoaded(false);
                    
                    // project/package -> project
                    if (selectedPackage.isEmpty() && getLocationPackage().isBlank()) {                        
                        // Package list arrives after requests,
                        // so do not clear requests. See load()
                        if (loaded) {
                            loaded = false;
                        } else {
                            requestsController.clear();
                        }

                        return;
                    }
                    
                    // project -> project/package
                    if (tabPane.getTabs().indexOf(filesTab) == -1 && 
                            tabPane.getTabs().indexOf(revisionsTab) == -1) {
                        tabPane.getTabs().add(1, filesTab);
                        tabPane.getTabs().add(2, revisionsTab);
                    }
                    
                    // Achtung! selectedPackage is empty when projectA/package -> projectB/package
                    if (selectedPackage.isEmpty() && !getLocationPackage().isBlank()) {
                        return;
                    }
                    
                    requestsController.clear();
                    
                    locationTextField.setText(currentProject + "/" + selectedPackage);
                    int tabIndex = tabPane.getSelectionModel().getSelectedIndex();
                    logger.log(Level.INFO, "Tab index = {0}", tabIndex);
                    
                    switch (tabIndex) {
                        case 0:
                            startPkgMetaConfigTask(currentProject, selectedPackage);
                            startLatestRevisionTask(currentProject, selectedPackage);
                            startBuildResultsTask(currentProject, selectedPackage, overviewController);
                            tabsChanged = false;
                            break;
                        case 1:
                            startFilesTask(currentProject, selectedPackage);
                            break;
                        case 2:
                            startRevisionsTask(currentProject, selectedPackage);
                            break;
                        case 3:                            
                            startPackageRequestsTask(currentProject, selectedPackage);
                            tabsChanged = false;
                            break;
                    }
                    
                    lastProject = currentProject;
                    lastPackage = currentPackage;
                });

        packagesObservableList = FXCollections.observableArrayList();
        packagesListProperty = new SimpleListProperty<>(packagesObservableList);
        overviewController.packageCountProperty().bind(packagesListProperty.sizeProperty().asString());
        filteredPackages = new FilteredList<>(packagesObservableList, (String s) -> true);
        SortedList<String> sortedPackages = new SortedList<>(filteredPackages, new Comparator<String>() {
            @Override
            public int compare(String t, String t1) {
                return t.compareTo(t1);
            }
        });

        packageFilter.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredPackages.setPredicate(s -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                if (!getLocationPackage().isBlank()) {
                    locationTextField.setText(getLocationProject());
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return s.contains(lowerCaseFilter);
            });
        });

        packagesListView.setItems(sortedPackages);
    }

    private void initTabPane() {
        tabPane.getTabs().remove(filesTab);
        tabPane.getTabs().remove(revisionsTab);
        
        // Tabs have been added/removed
        tabPane.getTabs().addListener((ListChangeListener.Change<? extends Tab> change) -> {
            // Listener is called twice (once per tab)
            // Set tabsChanged to true only once
            if (change.getList().size() == 3) {
                tabsChanged = true;
            }
        });

        tabPane.getSelectionModel().selectedIndexProperty().addListener((var ov, var t, var t1) -> {            
            logger.log(Level.INFO, "old tab {0}, new tab {1}", 
                    new Object[]{t.intValue(), t1.intValue()});

            // Avoid fetching requests twice when going from project to project/package (and viceversa);
            // tabs are added/removed, so tab index changes which triggers this listener
            if (tabsChanged) {
                tabsChanged = false;
                return;
            }
            
            if (currentProject.isBlank()) {
                return;
            }

            switch (t1.intValue()) {
                case 0:
                    logger.info("Overview tab visible");
                    if (currentPackage.isBlank() && !overviewController.isDataLoaded()) {
                        overviewController.clearPkgData();
                        startPrjMetaConfigTask(currentProject);
                    } else if (!currentPackage.isBlank() && !overviewController.isDataLoaded()) {
                        overviewController.clear();
                        startPkgMetaConfigTask(currentProject, currentPackage);
                        startLatestRevisionTask(currentProject, currentPackage);
                        startBuildResultsTask(currentProject, currentPackage, overviewController);
                    }
                    break;
                case 1:
                    if (currentPackage.isBlank() && getLocationPackage().isBlank()
                            && !requestsController.isDataLoaded()) {
                        logger.info("Requests tab visible");
                        requestsController.clear();
                        startProjectRequestsTask(currentProject);
                    } else if (!currentPackage.isBlank() && !filesController.isDataLoaded()) {
                        logger.info("Files tab visible");
                        filesController.clear();
                        startFilesTask(currentProject, currentPackage);
                    }
                    break;
                case 2:
                    logger.info("Revision tab visible");
                    if (!currentPackage.isBlank() && !revisionsController.isDataLoaded()) {
                        revisionsController.clear();
                        startRevisionsTask(currentProject, currentPackage);
                    }
                    break;
                case 3:
                    logger.info("Requests tab visible");
                    if (!currentPackage.isBlank() && !requestsController.isDataLoaded()) {
                        requestsController.clear();
                        startPackageRequestsTask(currentProject, currentPackage);
                    }
                    break;
            }
        });
    }

    public void setAccelerators(ObservableMap<KeyCombination, Runnable> accelerators) {
        KeyCombination projectsShortcut = new KeyCodeCombination(KeyCode.L,
                KeyCombination.CONTROL_DOWN);
        KeyCombination searchShortcut = new KeyCodeCombination(KeyCode.S,
                KeyCombination.CONTROL_DOWN);
        KeyCombination packagesShortcut = new KeyCodeCombination(KeyCode.P,
                KeyCombination.CONTROL_DOWN);
        KeyCombination bookmarksShortcut = new KeyCodeCombination(KeyCode.B,
                KeyCombination.CONTROL_DOWN);
        KeyCombination viewLogShortcut = new KeyCodeCombination(KeyCode.G,
                KeyCombination.CONTROL_DOWN);
        KeyCombination refreshShortcut = new KeyCodeCombination(KeyCode.R,
                KeyCombination.CONTROL_DOWN);
        KeyCombination switchTabsShortcut = new KeyCodeCombination(KeyCode.T,
                KeyCombination.CONTROL_DOWN);
        KeyCombination downloadShortcut = new KeyCodeCombination(KeyCode.D,
                KeyCombination.CONTROL_DOWN);
        accelerators.putAll(Map.of(
                projectsShortcut, () -> {
                    locationTextField.requestFocus();
                },
                searchShortcut, () -> {
                    searchTextField.requestFocus();
                },
                packagesShortcut, () -> {
                    packageFilter.requestFocus();
                },
                bookmarksShortcut, () -> {
                    bookmarksController.showBookmarks();
                },
                viewLogShortcut, () -> {
                    if (!App.getOBS().isAuthenticated()) {
                        showNotAuthenticatedAlert();
                        return;
                    }
                    if (overviewController.hasBuildResultSelection()) {
                        overviewController.handleViewLog();
                    }
                },
                refreshShortcut, () -> {
                    if (!App.getOBS().isAuthenticated()) {
                        showNotAuthenticatedAlert();
                        return;
                    }
                    if (tabPane.getSelectionModel().isSelected(0)) {
                        overviewController.handleRefresh();
                    }
                },
                switchTabsShortcut, () -> {
                    if (tabPane.getSelectionModel().isSelected(tabPane.getTabs().size() - 1)) {
                        tabPane.getSelectionModel().selectFirst();
                    } else {
                        tabPane.getSelectionModel().selectNext();
                    }
                },
                downloadShortcut, () -> {
                    if (tabPane.getSelectionModel().isSelected(0)) {
                        overviewController.handleDownload();
                    }
                }
        ));
    }

    private void startPrjMetaConfigTask(String prj) {       
        PrjMetaConfigTask prjMetaConfigTask = new PrjMetaConfigTask(prj);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(prjMetaConfigTask).start();

        prjMetaConfigTask.setOnSucceeded((e) -> {
            OBSProject project = prjMetaConfigTask.getValue();
            overviewController.setMetaConfig(project);
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        prjMetaConfigTask.setOnFailed((t) -> {
            overviewController.clear();
            showExceptionAlert(prjMetaConfigTask.getException());
        });
    }
    
    private void startPkgMetaConfigTask(String prj, String pkg) {
        PkgMetaConfigTask pkgMetaConfigTask = new PkgMetaConfigTask(prj, pkg);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(pkgMetaConfigTask).start();

        pkgMetaConfigTask.setOnSucceeded((e) -> {
            OBSPackage obsPackage = pkgMetaConfigTask.getValue();
            overviewController.setMetaConfig(obsPackage);
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        pkgMetaConfigTask.setOnFailed((t) -> {
            overviewController.clear();
            showExceptionAlert(pkgMetaConfigTask.getException());
        });
    }

    public void startBuildLogTask(String prj, String repository, String arch, String pkg) {
        BuildLogTask buildLogTask = new BuildLogTask(prj, repository, arch, pkg);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(buildLogTask).start();
        buildLogTask.setOnSucceeded((e) -> {
            overviewController.setBuildLog(buildLogTask.getValue());
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        buildLogTask.setOnFailed((t) -> {
            showExceptionAlert(buildLogTask.getException());
        });
    }
        
    private void startLatestRevisionTask(String prj, String pkg) {
        RevisionsTask latestRevisionsTask = new RevisionsTask(prj, pkg, true);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(latestRevisionsTask).start();
        latestRevisionsTask.setOnSucceeded((e) -> {
            if (!latestRevisionsTask.getValue().isEmpty()) {
                overviewController.setLatestRevision(latestRevisionsTask.getValue().get(0));
            } else {
                overviewController.clearLatestRevision();
            }
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        latestRevisionsTask.setOnFailed((t) -> {
            showExceptionAlert(latestRevisionsTask.getException());
        });

    }

    public void startBuildResultsTask(String prj, String pkg, TableSetter table) {
        BuildResultsTask buildResultsTask = new BuildResultsTask(prj, pkg);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(buildResultsTask).start();
        buildResultsTask.setOnSucceeded((e) -> {
            List<OBSResult> buildResults = buildResultsTask.getValue();
            if (buildResults != null) {
                table.setAll(buildResults);
            }
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        buildResultsTask.setOnFailed((t) -> {
            showExceptionAlert(buildResultsTask.getException());
        });
    }

    public void startFilesTask(String prj, String pkg) {
        FilesTask filesTask = new FilesTask(prj, pkg);
        runningTasks.incrementAndGet();        
        progressIndicator.setVisible(true);
        new Thread(filesTask).start();        
        filesTask.setOnSucceeded((e) -> {
            List<OBSEntry> files = filesTask.getValue();
            filesController.set(files);
            filesController.setPrj(prj);
            filesController.setPkg(pkg);
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        filesTask.setOnFailed((t) -> {
            showExceptionAlert(filesTask.getException());
        });
    }

    public void startRevisionsTask(String prj, String pkg) {
        RevisionsTask revisionsTask = new RevisionsTask(prj, pkg, false);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(revisionsTask).start();        
        revisionsTask.setOnSucceeded((e) -> {
            List<OBSRevision> revisions = revisionsTask.getValue();
            revisionsController.set(revisions);
            revisionsController.setPkg(pkg);
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        revisionsTask.setOnFailed((t) -> {
            showExceptionAlert(revisionsTask.getException());
        });
    }

    public void startProjectRequestsTask(String prj) {
        ProjectRequestsTask projectRequestsTask = new ProjectRequestsTask(prj);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(projectRequestsTask).start();
        projectRequestsTask.setOnSucceeded((e) -> {
            List<OBSRequest> requests = projectRequestsTask.getValue();
            requestsController.set(requests);
            requestsController.setPrj(prj);
            requestsController.setPkg(null);
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        projectRequestsTask.setOnFailed((t) -> {
            showExceptionAlert(projectRequestsTask.getException());
        });
    }
    
    public void startPackageRequestsTask(String prj, String pkg) {
        PackageRequestsTask packageRequestsTask = new PackageRequestsTask(prj, pkg);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(packageRequestsTask).start();
        packageRequestsTask.setOnSucceeded((e) -> {
            List<OBSRequest> requests = packageRequestsTask.getValue();
            requestsController.set(requests);
            requestsController.setPrj(prj);            
            requestsController.setPkg(pkg);
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        packageRequestsTask.setOnFailed((t) -> {
            showExceptionAlert(packageRequestsTask.getException());
        });
    }

    public void startProjectsTask(boolean includeHomePrjs) {        
        ProjectsTask projectsTask = new ProjectsTask(includeHomePrjs);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);        
        new Thread(projectsTask).start();
        projectsTask.setOnSucceeded((t) -> {
            projects = projectsTask.getValue();
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
        });
        projectsTask.setOnFailed((t) -> {
            showExceptionAlert(projectsTask.getException());
        });
    }
    
    public void startSearchTask(String pkg) {
        SearchTask searchTask = new SearchTask(pkg);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(searchTask).start();
        
        searchTask.setOnSucceeded((t) -> {
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
            searchResults.clear();
            if (searchTask.getValue() != null) {
                searchResults.addAll(searchTask.getValue());
            }
        });
        
        searchTask.setOnFailed((t) -> {
            showExceptionAlert(searchTask.getException());
        });
    }
    
    public void startDiffTask(String id) {
        DiffTask diffTask = new DiffTask(id);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(diffTask).start();
        
        diffTask.setOnSucceeded((t) -> {
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
            if (diffTask.getValue() != null) {
                requestsController.setDiff(diffTask.getValue());
            }
        });
        
        diffTask.setOnFailed((t) -> {
            showExceptionAlert(diffTask.getException());
        });
    }
    
    public void startChangeRequestTask(String id, String comments, boolean accepted) {
        ChangeRequestTask changeRequestTask = new ChangeRequestTask(id, comments, accepted);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(changeRequestTask).start();
        
        changeRequestTask.setOnSucceeded((t) -> {
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }

            if (changeRequestTask.getValue() != null) {
                OBSStatus status = changeRequestTask.getValue();
                startPackageRequestsTask(currentProject, currentPackage);
                
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.initOwner(borderPane.getScene().getWindow());
                alert.setTitle("Request change");
                alert.setHeaderText(null);
                alert.setContentText("Result: " + status.getCode());
                alert.showAndWait();
            }
        });
        
        changeRequestTask.setOnFailed((t) -> {
            showExceptionAlert(changeRequestTask.getException());
        });
    }
    
    public void startDownloadTask(String prj, String pkg, String fileName, File destinationFile) {
        DownloadTask downloadTask = new DownloadTask(prj, pkg, fileName);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(downloadTask).start();
        
        downloadTask.setOnSucceeded((t) -> {
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }

            if (downloadTask.getValue() != null) {   
                try (FileOutputStream outputStream = new FileOutputStream(destinationFile.getAbsolutePath())) {
                    InputStream is = downloadTask.getValue();
                    is.transferTo(outputStream);

                    Notifications.create()
                            .owner(App.getWindow())
                            .title(App.getBundle().getString("files.download.completed"))
                            .text(destinationFile.getName())
                            .hideAfter(Duration.seconds(5))
                            .showInformation();
                } catch (IOException ex) {
                    showExceptionAlert(ex);
                }                
            }
        });
        
        downloadTask.setOnFailed((t) -> {
            showExceptionAlert(downloadTask.getException());
        });
    }
    
    public void startUploadTask(String prj, String pkg, File file) {
        UploadTask uploadTask = new UploadTask(prj, pkg, file);
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(uploadTask).start();
        
        uploadTask.setOnSucceeded((t) -> {
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }
            filesController.clear();
            startFilesTask(prj, pkg);

            Notifications.create()
                    .owner(App.getWindow())
                    .title(App.getBundle().getString("files.upload.completed"))
                    .text(file.getName())
                    .hideAfter(Duration.seconds(5))
                    .showInformation();
        });
        
        uploadTask.setOnFailed((t) -> {
            showExceptionAlert(uploadTask.getException());
        });
    }
    
    public void deleteFile(String prj, String pkg, String fileName){
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        App.getOBS().deleteFile(prj, pkg, fileName);
        if (runningTasks.decrementAndGet() == 0) {
            progressIndicator.setVisible(false);
        }
    }

    public void loadBookmarks() {
        OBSPerson person = App.getOBS().getPerson();
        bookmarksController.addBookmarks(person);
    }

    @FXML
    private void handleSettings() {
        showSettingsDialog(false, false);
    }
    
    @FXML
    private void handleShortcuts() {
        ShortcutsAlert alert = new ShortcutsAlert(borderPane.getScene().getWindow());
        alert.showAndWait();
    }
    
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.initOwner(borderPane.getScene().getWindow());
        alert.getDialogPane().setMinSize(480, 320);
        alert.setTitle(App.getBundle().getString("about.title"));
        ImageView icon = new ImageView(App.class.getResource(App.ICON).toString());
        icon.setFitWidth(100);
        icon.setPreserveRatio(true);
        alert.setGraphic(icon);
        alert.setHeaderText(App.NAME + " " + App.VERSION + "\n"
                + App.getBundle().getString("about.description"));
        alert.setContentText("Java: "
                + System.getProperty("java.runtime.name") + " "
                + System.getProperty("java.runtime.version") + "\n"
                + "JavaFX: " + System.getProperty("javafx.runtime.version") + "\n"
                + App.getBundle().getString("about.libraries")
                + "jobs, controlsfx, "
                + "ikonli-javafx, ikonli-icomoon-pack, "
                + "richtextfx" + "\n"
                + App.getBundle().getString("about.locale") + Locale.getDefault() + "\n\n"
                + App.getBundle().getString("about.copyright") + "\n"
                + App.getBundle().getString("about.license"));

        alert.showAndWait();
    }

    public void autoLogin() {
        if (preferences.getBoolean(App.AUTOLOGIN, false)) {
            handleLogin();            
        }
    }
    
    @FXML
    private void handleLogin() {        
        if (App.getOBS().isAuthenticated()) {
            clear();
            App.getOBS().logout();
        } else {
            try {
                runningTasks.incrementAndGet();
                progressIndicator.setVisible(true);
                String usernameText = preferences.get(App.USERNAME, "");
                String passwordText = App.getAuthTokenEncryptor()
                        .decrypt(preferences.get(App.PASSWORD, ""));
                String apiUri = preferences.get(App.API_URI, "");

                if (apiUri.isEmpty()) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.initOwner(borderPane.getScene().getWindow());
                    alert.setResizable(true);
                    alert.setTitle(App.getBundle().getString("error.title"));
                    alert.setHeaderText(App.getBundle().getString("error.api_uri.empty.header"));
                    alert.setContentText(App.getBundle().getString("error.api_uri.empty.content"));
                    alert.showAndWait();
                    showSettingsDialog(true, true);
                } else if (usernameText.isBlank() || passwordText.isBlank()) {
                    LoginDialog dialog = new LoginDialog(borderPane.getScene().getWindow(),
                            preferences);
                    Optional<Pair<String, String>> result = dialog.showAndWait();
                    result.ifPresentOrElse((credentialsEntered) -> {
                        authenticate(credentialsEntered.getKey(), credentialsEntered.getValue());
                    }, () -> {
                        // Do nothing
                    });
                } else {                    
                    if (apiUri.endsWith("/")) {
                        apiUri = apiUri.substring(0, apiUri.length() - 1);
                    }                    
                    App.getOBS().setApiUri(new URI(apiUri));
                    authenticate(usernameText, passwordText);
                }
                if (runningTasks.decrementAndGet() == 0) {
                    progressIndicator.setVisible(false);
                }
            } catch (IllegalBlockSizeException | BadPaddingException 
                    | URISyntaxException ex) {
                showExceptionAlert(ex);
                Logger.getLogger(BrowserController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void authenticate(String username, String password) {      
        Task<Void> authTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                runningTasks.incrementAndGet();
                progressIndicator.setVisible(true);
                App.getOBS().setUsername(username);
                App.getOBS().setPassword(password);
                App.getOBS().authenticate();
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                if (App.getOBS().isAuthenticated()) {
                    String homepage = preferences.get(App.HOMEPAGE,
                            "home:" + App.getOBS().getUsername());
                    boolean includeHomePrjs = preferences.getBoolean(App.HOME_PROJECTS, false);
                    goTo(homepage);
                    startProjectsTask(includeHomePrjs);
                    loadBookmarks();
                }
                if (runningTasks.decrementAndGet() == 0) {
                    progressIndicator.setVisible(false);
                }
            }

            @Override
            protected void failed() {
                super.failed();
                logger.info("auth failed!");
                
                Throwable exception = getException();                
                if (exception instanceof ClientErrorException || exception instanceof ServerErrorException) {

                    switch (((ClientErrorException) exception).getResponse().getStatusInfo().toEnum()) {
                        case UNAUTHORIZED:
                            Platform.runLater(() -> {
                                LoginDialog dialog = new LoginDialog(borderPane
                                        .getScene().getWindow(), preferences);
                                Optional<Pair<String, String>> result = dialog.showAndWait();
                                result.ifPresentOrElse((t) -> {
                                    try {
                                        preferences.put(App.USERNAME, t.getKey());
                                        preferences.put(App.PASSWORD, App
                                                .getAuthTokenEncryptor().encrypt(t.getValue()));
                                        if (runningTasks.decrementAndGet() == 0) {
                                            progressIndicator.setVisible(false);
                                        }
                                        authenticate(t.getKey(), t.getValue());
                                    } catch (IllegalBlockSizeException | BadPaddingException ex) {
                                        showExceptionAlert(ex);
                                        Logger.getLogger(BrowserController.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }, () -> {
                                    // Do nothing
                                }
                                );
                            });
                            logger.info("Not authorized");
                            break;
                        case NOT_FOUND:
                            Platform.runLater(() -> {
                                if (runningTasks.decrementAndGet() == 0) {
                                    progressIndicator.setVisible(false);
                                }
                                Alert alert = new Alert(AlertType.ERROR);
                                alert.initOwner(borderPane.getScene().getWindow());
                                alert.setResizable(true);
                                alert.setTitle(App.getBundle().getString("error.title"));
                                alert.setHeaderText(App.getBundle().getString("error.api_uri.not_found.header"));
                                alert.setContentText(preferences.get(App.API_URI, ""));
                                alert.showAndWait();

                                showSettingsDialog(true, true);
                            });
                            break;
                    }
                } else {
                    if (runningTasks.decrementAndGet() == 0) {
                        progressIndicator.setVisible(false);
                    }
                }
            }
        };

        new Thread(authTask).start();
    }
    
    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void goHome() {
        String username = App.getOBS().getUsername();
        if (username != null) {
            String userHome = "home:" + username;
            goTo(userHome);
        } else {
            showNotAuthenticatedAlert();
        }
    }

    @FXML
    private void handleNavigation(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            load(locationTextField.getText());
        }
    }
    
    @FXML
    private void handleSearch(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && !searchTextField.getText().isBlank()) {
            goTo(searchTextField.getText());
        }
    }
    
    private void showNotAuthenticatedAlert() {
        Alert alert = new Alert(AlertType.ERROR);
        alert.initOwner(borderPane.getScene().getWindow());
        alert.setTitle(App.getBundle().getString("alert.notauthenticated.title"));
        alert.setHeaderText(App.getBundle().getString("alert.notauthenticated.header"));
        alert.setContentText(App.getBundle().getString("alert.notauthenticated.content"));
        alert.show();
    }

    public void startPackagesTask() {        
        if (!App.getOBS().isAuthenticated()) {
            showNotAuthenticatedAlert();
            return;
        }

        final String prj = getLocationProject();
        final String pkg = getLocationPackage();
        logger.log(Level.INFO, "project = {0}", prj);
        
        PackagesTask packagesTask = new PackagesTask(prj);
        overviewController.toggleButtons(!pkg.isBlank());
        runningTasks.incrementAndGet();
        progressIndicator.setVisible(true);
        new Thread(packagesTask).start();

        packagesTask.setOnSucceeded((e) -> {
            logger.info("packages fetched");
            bookmarksController.setPrj(prj);
            bookmarksController.setPkg(pkg);
            List<String> packages = packagesTask.getValue();
            packagesObservableList.setAll(packages);
            if (runningTasks.decrementAndGet() == 0) {
                progressIndicator.setVisible(false);
            }

            logger.log(Level.INFO, "package = {0}", pkg);
            if (!pkg.isEmpty()) {
                logger.log(Level.INFO, "Selecting package = {0}", pkg);
                packagesListView.getSelectionModel().select(pkg);
                packagesListView.scrollTo(pkg);
            }
        });
        
        packagesTask.setOnFailed((t) -> {
            // FIXME: clear?
            packagesListView.getSelectionModel().clearSelection();
            packagesObservableList.clear();
            showExceptionAlert(packagesTask.getException());
        });
    }

    private void showSettingsDialog(boolean focusApiUriField, boolean callHandleLogin) {
        SettingsDialog settingsDialog = new SettingsDialog(borderPane
                .getScene().getWindow(), preferences);
        if (focusApiUriField) {
            settingsDialog.focusApiUriField();
        }
        Optional<Map<String, String>> result = settingsDialog.showAndWait();
        result.ifPresent(data -> {
            boolean oldIncludeHomePrjs = preferences.getBoolean(App.HOME_PROJECTS, false);
            updatePreferences(data);
            if (callHandleLogin) {
                handleLogin();
            } else {
                boolean newIncludeHomePrjs = preferences.getBoolean(App.HOME_PROJECTS, false);
                if (oldIncludeHomePrjs != newIncludeHomePrjs 
                        && App.getOBS().isAuthenticated()) {
                    startProjectsTask(newIncludeHomePrjs);
                }
            }
        });
    }
    
    public void showExceptionAlert(Throwable throwable) {
        if (runningTasks.decrementAndGet() == 0) {
            progressIndicator.setVisible(false);
        }
        ExceptionAlert exceptionAlert = new ExceptionAlert(borderPane.getScene().getWindow());
        exceptionAlert.setHeader(throwable.getMessage());        
        exceptionAlert.setThrowable(throwable);
        exceptionAlert.showAndWait();
    }

    private void updatePreferences(Map<String, String> data) {
        preferences.put(App.USERNAME, data.get(App.USERNAME));
        preferences.put(App.PASSWORD, data.get(App.PASSWORD));
        preferences.put(App.API_URI, data.get(App.API_URI));
        preferences.put(App.HOMEPAGE, data.get(App.HOMEPAGE));
        preferences.put(App.HOME_PROJECTS, data.get(App.HOME_PROJECTS));
        preferences.put(App.AUTOLOGIN, data.get(App.AUTOLOGIN));
    }
    
    private void clear() {
        locationTextField.clear();
        currentProject = "";
        currentPackage = "";
        overviewController.clear();
        filesController.clear();
        revisionsController.clear();
        requestsController.clear();
        packagesListView.getSelectionModel().clearSelection();
        packagesObservableList.clear();
    }

}
