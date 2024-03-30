/*
 * Copyright (C) 2023-2024 Javier Llorente <javier@opensuse.org>
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

import com.javierllorente.obsfx.task.BuildResultsTask;
import com.javierllorente.obsfx.task.RevisionsTask;
import com.javierllorente.jobs.entity.OBSFile;
import com.javierllorente.jobs.entity.OBSPackage;
import com.javierllorente.jobs.entity.OBSPerson;
import com.javierllorente.jobs.entity.OBSPkgMetaConfig;
import com.javierllorente.jobs.entity.OBSRequest;
import com.javierllorente.jobs.entity.OBSResult;
import com.javierllorente.jobs.entity.OBSRevision;
import com.javierllorente.obsfx.alert.ExceptionAlert;
import com.javierllorente.obsfx.alert.ShortcutsAlert;
import com.javierllorente.obsfx.dialog.LoginDialog;
import com.javierllorente.obsfx.dialog.SettingsDialog;
import com.javierllorente.obsfx.task.BuildLogTask;
import com.javierllorente.obsfx.task.FilesTask;
import com.javierllorente.obsfx.task.PackagesTask;
import com.javierllorente.obsfx.task.PkgMetaConfigTask;
import com.javierllorente.obsfx.task.ProjectsTask;
import com.javierllorente.obsfx.task.RequestsTask;
import com.javierllorente.obsfx.task.SearchTask;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.xml.parsers.ParserConfigurationException;
import org.controlsfx.control.textfield.TextFields;
import org.xml.sax.SAXException;

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
    private HostServices hostServices;
    private ObservableList<OBSPackage> searchResults;

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
        
        initLocationTextField();
        initSearchTextField();
        initPackageListView();
        initTabPane();

        Platform.runLater(() -> locationTextField.requestFocus());

        bookmarksController.setBrowserController(this);
        overviewController.setBrowserController(this);
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void initLocationTextField() {
        locationTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                packagesListView.getSelectionModel().clearSelection();
                packagesObservableList.clear();
            }
        });
    }
    
    private void initSearchTextField() {
        searchResults = FXCollections.observableArrayList();
        var autoComplete = TextFields.bindAutoCompletion(searchTextField, input -> {
            // Do not show results for previous query (results arrive late)
            return searchResults.stream()
                    .filter(p -> p.getName().contains(input.getUserText()))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
        });

        autoComplete.setPrefWidth(searchTextField.getPrefWidth());
        autoComplete.setVisibleRowCount(7);
        autoComplete.setHideOnEscape(true);
        
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isBlank()) {
                startSearchTask(newValue);
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
    
    public void goTo(String location) {
        locationTextField.setText(location);
        startPackagesTask(location);        
    }

    private void initPackageListView() {
        packagesListView.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends String> ov,
                        String oldValue, String newValue) -> {
                    String prj = getLocationProject();
                    String pkg = newValue;
                    logger.log(Level.INFO, "Selected package: {0}", pkg);
                    bookmarksController.setPkg(pkg);

                    if (pkg == null) {
                        overviewController.clear();
                        filesController.clear();
                        revisionsController.clear();
                        requestsController.clear();
                        return;
                    }

                    locationTextField.setText(prj + "/" + pkg);

                    switch (tabPane.getSelectionModel().getSelectedIndex()) {
                        case 0:
                            startPkgMetaConfigTask(prj, pkg);
                            startLatestRevisionTask(prj, pkg);
                            startBuildResultsTask(prj, pkg);
                            break;
                        case 1:
                            startFilesTask(prj, pkg);
                            break;
                        case 2:
                            startRevisionsTask(prj, pkg);
                            break;
                        case 3:
                            startRequestsTask(prj, pkg);
                            break;
                    }
                });

        packagesObservableList = FXCollections.observableArrayList();
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
                String lowerCaseFilter = newValue.toLowerCase();
                return s.contains(lowerCaseFilter);
            });
        });

        packagesListView.setItems(sortedPackages);
    }

    private void initTabPane() {
        tabPane.getSelectionModel().selectedIndexProperty().addListener((var ov, var t, var t1) -> {
            String prj = getLocationProject();
            String pkg = packagesListView.getSelectionModel().getSelectedItem();

            switch (t1.intValue()) {
                case 0:
                    logger.info("Overview tab visible");
                    if (!prj.isBlank() && pkg != null && !pkg.equals(overviewController.getPkg())) {
                        overviewController.clear();
                        startPkgMetaConfigTask(prj, pkg);
                        startLatestRevisionTask(prj, pkg);
                        startBuildResultsTask(prj, pkg);
                    }
                    break;
                case 1:
                    if (!prj.isBlank() && pkg != null && !pkg.equals(filesController.getPkg())) {
                        filesController.clear();
                        startFilesTask(prj, pkg);
                    }
                    break;
                case 2:
                    logger.info("Revision tab visible");
                    if (!prj.isBlank() && pkg != null && !pkg.equals(revisionsController.getPkg())) {
                        revisionsController.clear();
                        startRevisionsTask(prj, pkg);
                    }
                    break;
                case 3:
                    logger.info("Requests tab visible");
                    if (!prj.isBlank() && pkg != null && !pkg.equals(requestsController.getPkg())) {
                        requestsController.clear();
                        startRequestsTask(prj, pkg);
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
                    if (overviewController.isBuildResultSelected()) {
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

    private void startPkgMetaConfigTask(String prj, String pkg) {
        PkgMetaConfigTask pkgMetaConfigTask = new PkgMetaConfigTask(prj, pkg);
        progressIndicator.setVisible(true);
        new Thread(pkgMetaConfigTask).start();

        pkgMetaConfigTask.setOnSucceeded((e) -> {
            OBSPkgMetaConfig pkgMetaConfig = pkgMetaConfigTask.getValue();
            overviewController.setPkgMetaConfig(pkgMetaConfig);
            progressIndicator.setVisible(false);
        });
        pkgMetaConfigTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(pkgMetaConfigTask.getException());
        });
    }

    public void startBuildLogTask(String prj, String repository, String arch, String pkg) {
        BuildLogTask buildLogTask = new BuildLogTask(prj, repository, arch, pkg);
        progressIndicator.setVisible(true);
        new Thread(buildLogTask).start();
        buildLogTask.setOnSucceeded((e) -> {
            overviewController.setBuildLog(buildLogTask.getValue());
            progressIndicator.setVisible(false);
        });
        buildLogTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(buildLogTask.getException());
        });
    }
        
    private void startLatestRevisionTask(String prj, String pkg) {
        RevisionsTask latestRevisionsTask = new RevisionsTask(prj, pkg, true);
        progressIndicator.setVisible(true);
        new Thread(latestRevisionsTask).start();
        latestRevisionsTask.setOnSucceeded((e) -> {
            if (!latestRevisionsTask.getValue().isEmpty()) {
                overviewController.setLatestRevision(latestRevisionsTask.getValue().get(0));
            } else {
                overviewController.clearLatestRevision();
            }
            progressIndicator.setVisible(false);
        });
        latestRevisionsTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(latestRevisionsTask.getException());
        });

    }

    public void startBuildResultsTask(String prj, String pkg) {
        BuildResultsTask buildResultsTask = new BuildResultsTask(prj, pkg);
        progressIndicator.setVisible(true);
        new Thread(buildResultsTask).start();
        buildResultsTask.setOnSucceeded((e) -> {
            List<OBSResult> buildResults = buildResultsTask.getValue();
            if (buildResults != null) {
                overviewController.setResults(buildResults);
            }
            progressIndicator.setVisible(false);
        });
        buildResultsTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(buildResultsTask.getException());
        });
    }

    public void startFilesTask(String prj, String pkg) {
        FilesTask filesTask = new FilesTask(prj, pkg);        
        progressIndicator.setVisible(true);
        new Thread(filesTask).start();        
        filesTask.setOnSucceeded((e) -> {
            List<OBSFile> files = filesTask.getValue();
            filesController.set(files);
            filesController.setPkg(pkg);
            progressIndicator.setVisible(false);
        });
        filesTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(filesTask.getException());
        });
    }

    public void startRevisionsTask(String prj, String pkg) {
        RevisionsTask revisionsTask = new RevisionsTask(prj, pkg, false);
        progressIndicator.setVisible(true);
        new Thread(revisionsTask).start();        
        revisionsTask.setOnSucceeded((e) -> {
            List<OBSRevision> revisions = revisionsTask.getValue();
            revisionsController.set(revisions);
            revisionsController.setPkg(pkg);
            progressIndicator.setVisible(false);
        });
        revisionsTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(revisionsTask.getException());
        });
    }

    public void startRequestsTask(String prj, String pkg) {
        RequestsTask requestsTask = new RequestsTask(prj, pkg);
        progressIndicator.setVisible(true);
        new Thread(requestsTask).start();
        requestsTask.setOnSucceeded((e) -> {
            List<OBSRequest> requests = requestsTask.getValue();
            requestsController.set(requests);
            requestsController.setPkg(pkg);
            progressIndicator.setVisible(false);
        });
        requestsTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(requestsTask.getException());
        });
    }

    public void startProjectsTask() {
        ProjectsTask projectsTask = new ProjectsTask();
        progressIndicator.setVisible(true);
        new Thread(projectsTask).start();
        projectsTask.setOnSucceeded((t) -> {
            projects = projectsTask.getValue();
            var autoComplete = TextFields.bindAutoCompletion(locationTextField, projects);
            autoComplete.setPrefWidth(locationTextField.getPrefWidth());
            autoComplete.setVisibleRowCount(7);
            progressIndicator.setVisible(false);
        });
        projectsTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(projectsTask.getException());
        });
    }
    
    public void startSearchTask(String pkg) {
        SearchTask searchTask = new SearchTask(pkg);
        progressIndicator.setVisible(true);
        new Thread(searchTask).start();
        
        searchTask.setOnSucceeded((t) -> {
            progressIndicator.setVisible(false);
            searchResults.clear();
            if (searchTask.getValue() != null) {
                searchResults.addAll(searchTask.getValue());
            }
        });
        
        searchTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
            showExceptionAlert(searchTask.getException());
        });
    }

    public void loadBookmarks() {
        try {
            OBSPerson person = App.getOBS().getPerson();
            bookmarksController.addBookmarks(person);
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            Logger.getLogger(BrowserController.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                + "ikonli-javafx, ikonli-icomoon-pack" + "\n"
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
        } else {
            progressIndicator.setVisible(true);
            try {
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
                    }, () -> progressIndicator.setVisible(false));
                } else {                    
                    if (apiUri.endsWith("/")) {
                        apiUri = apiUri.substring(0, apiUri.length() - 1);
                    }                    
                    App.getOBS().setApiUrl(new URL(apiUri));
                    authenticate(usernameText, passwordText);
                }
            } catch (MalformedURLException | IllegalBlockSizeException | BadPaddingException 
                    | IllegalArgumentException ex) {
                showExceptionAlert(ex);
                Logger.getLogger(BrowserController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void authenticate(String username, String password) {        
        Task<Void> authTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
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
                    startProjectsTask();
                    loadBookmarks();
                } else {
                    switch (App.getOBS().getResponseCode()) {
                        case HttpURLConnection.HTTP_UNAUTHORIZED:
                            Platform.runLater(() -> {
                                LoginDialog dialog = new LoginDialog(borderPane
                                        .getScene().getWindow(), preferences);
                                Optional<Pair<String, String>> result = dialog.showAndWait();
                                result.ifPresentOrElse((t) -> {
                                    try {
                                        preferences.put(App.USERNAME, t.getKey());
                                        preferences.put(App.PASSWORD, App
                                                .getAuthTokenEncryptor().encrypt(t.getValue()));
                                        authenticate(t.getKey(), t.getValue());
                                    } catch (IllegalBlockSizeException | BadPaddingException ex) {
                                        showExceptionAlert(ex);
                                        Logger.getLogger(BrowserController.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }, () -> progressIndicator.setVisible(false));
                            });
                            logger.info("Not authorized");
                            break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        Platform.runLater(() -> {
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
                }
                progressIndicator.setVisible(false);
            }

            @Override
            protected void failed() {
                super.failed();
                progressIndicator.setVisible(false);
                showExceptionAlert(getException());
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
            startPackagesTask(userHome);
            locationTextField.setText(userHome);
        } else {
            showNotAuthenticatedAlert();
        }
    }

    @FXML
    private void handleNavigation(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            startPackagesTask(locationTextField.getText());
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

    private void startPackagesTask(String location) {        
        if (!App.getOBS().isAuthenticated()) {
            showNotAuthenticatedAlert();
            return;
        }

        logger.log(Level.INFO, "package = {0}", location.contains("/") 
                ? location.split("/", 2)[1] : "");

        final String prj = location.contains("/") ? location.split("/", 2)[0] : location;
        final String pkg = location.contains("/") 
                ? location.split("/", 2)[1].replace("/", "") : "";
        PackagesTask packagesTask = new PackagesTask(prj);
        progressIndicator.setVisible(true);
        new Thread(packagesTask).start();

        packagesTask.setOnSucceeded((e) -> {
            logger.info("packages fetched");
            bookmarksController.setPrj(prj);
            bookmarksController.setPkg(pkg);
            List<String> packages = packagesTask.getValue();
            packagesObservableList.setAll(packages);
            progressIndicator.setVisible(false);

            logger.log(Level.INFO, "package = {0}", pkg);
            if (!pkg.isEmpty()) {
                packagesListView.getSelectionModel().select(pkg);
            }
        });
        
        packagesTask.setOnFailed((t) -> {
            progressIndicator.setVisible(false);
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
            updatePreferences(data);
            if (callHandleLogin) {
                handleLogin();
            }
        });
    }
    
    public void showExceptionAlert(Throwable throwable) {
        progressIndicator.setVisible(false);
        ExceptionAlert exceptionAlert = new ExceptionAlert(borderPane.getScene().getWindow());
        exceptionAlert.setHeader(throwable.getMessage());        
        exceptionAlert.setThrowable(throwable);
        exceptionAlert.showAndWait();
    }

    private void updatePreferences(Map<String, String> data) {
        preferences.put(App.USERNAME, data.get(App.USERNAME));
        preferences.put(App.PASSWORD, data.get(App.PASSWORD));
        preferences.put(App.API_URI, data.get(App.API_URI));
        preferences.put(App.AUTOLOGIN, data.get(App.AUTOLOGIN));
    }
    
    private void clear() {
        packagesListView.getSelectionModel().clearSelection();
        packagesObservableList.clear();
    }

}
