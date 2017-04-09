package todolist.ui;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import todolist.MainApp;
import todolist.commons.core.ComponentManager;
import todolist.commons.core.Config;
import todolist.commons.core.LogsCenter;
import todolist.commons.events.storage.DataSavingExceptionEvent;
import todolist.commons.events.ui.ClearAllSelectionsEvent;
import todolist.commons.events.ui.JumpToListRequestEvent;
import todolist.commons.events.ui.SelectMultipleTargetEvent;
import todolist.commons.events.ui.ShowHelpRequestEvent;
import todolist.commons.events.ui.TaskPanelSelectionChangedEvent;
import todolist.commons.util.StringUtil;
import todolist.logic.Logic;
import todolist.model.UserPrefs;
/**
 * The manager of the UI component.
 */
public class UiManager extends ComponentManager implements Ui {
    private static final Logger logger = LogsCenter.getLogger(UiManager.class);
    private static final String ICON_APPLICATION = "/images/address_book_32.png";
    public static final String ALERT_DIALOG_PANE_FIELD_ID = "alertDialogPane";

    private Logic logic;
    private Config config;
    private UserPrefs prefs;
    private MainWindow mainWindow;
    private Boolean firstTime;

    public UiManager(Logic logic, Config config, UserPrefs prefs) {
        super();
        this.logic = logic;
        this.config = config;
        this.prefs = prefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting UI...");
        primaryStage.setTitle(config.getAppTitle());

        // Set the application icon.
        primaryStage.getIcons().add(getImage(ICON_APPLICATION));

        try {
            mainWindow = new MainWindow(primaryStage, config, prefs, logic);
            // Create the keystroke listeners
            //initiateGlobalKeyListener(mainWindow);

            // Create the tray icon.
            //initializeTray(primaryStage);
            mainWindow.show(); // This should be called before creating other UI
                               // parts
            mainWindow.fillInnerParts();

        } catch (Throwable e) {
            logger.severe(StringUtil.getDetails(e));
            showFatalErrorDialogAndShutdown("Fatal error during initializing", e);
        }
    }

    /*
     * Load the icon image and then call for tray setup.
     */
    private void initializeTray(Stage primaryStage) {
        BufferedImage trayIconImage = null;
        try {
            trayIconImage = getBufferedImage(ICON_APPLICATION);
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
        if (SystemTray.isSupported()) {
            createTrayIcon(primaryStage, trayIconImage);
        }
    }

    /*
     * Setup the tray and all the tray properties.
     */
    private void createTrayIcon(final Stage stage, BufferedImage trayIconImage) {
        SystemTray tray = SystemTray.getSystemTray();

        // create listener for clicks on tray icon
        final TrayIconListener trayIconListener = new TrayIconListener(mainWindow);

        // create a popup menu
        PopupMenu popup = new PopupMenu();

        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(trayIconListener);
        popup.add(showItem);

        MenuItem hideItem = new MenuItem("Hide");
        hideItem.addActionListener(trayIconListener);
        popup.add(hideItem);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(trayIconListener);
        popup.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(trayIconImage, "ToDoList", popup);
        trayIcon.addActionListener(trayIconListener);

        // add the tray image
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println(e);
        }

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                stage.hide();
                if (firstTime) {
                    trayIcon.displayMessage("ToDoList running in background.", "Press hotkeys to open.",
                            TrayIcon.MessageType.INFO);
                    firstTime = false;
                }
            }
        });
    }

    @Override
    public void stop() {
        prefs.updateLastUsedGuiSetting(mainWindow.getCurrentGuiSetting());
        mainWindow.hide();
        // mainWindow.releaseResources();
    }

    private void showFileOperationAlertAndWait(String description, String details, Throwable cause) {
        final String content = details + ":\n" + cause.toString();
        showAlertDialogAndWait(AlertType.ERROR, "File Op Error", description, content);
    }

    private Image getImage(String imagePath) {
        return new Image(MainApp.class.getResourceAsStream(imagePath));
    }

    private BufferedImage getBufferedImage(String imagePath) throws IOException {
        return ImageIO.read((MainApp.class.getResourceAsStream(imagePath)));
    }

    void showAlertDialogAndWait(Alert.AlertType type, String title, String headerText, String contentText) {
        showAlertDialogAndWait(mainWindow.getPrimaryStage(), type, title, headerText, contentText);
    }

    private static void showAlertDialogAndWait(Stage owner, AlertType type, String title, String headerText,
            String contentText) {
        final Alert alert = new Alert(type);
        alert.getDialogPane().getStylesheets().add("view/DarkTheme.css");
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getDialogPane().setId(ALERT_DIALOG_PANE_FIELD_ID);
        alert.showAndWait();
    }

    private void showFatalErrorDialogAndShutdown(String title, Throwable e) {
        logger.severe(title + " " + e.getMessage() + StringUtil.getDetails(e));
        showAlertDialogAndWait(Alert.AlertType.ERROR, title, e.getMessage(), e.toString());
        Platform.exit();
        System.exit(1);
    }

    // ==================== Event Handling Code
    // ===============================================================

    @Subscribe
    private void handleDataSavingExceptionEvent(DataSavingExceptionEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        showFileOperationAlertAndWait("Could not save data", "Could not save data to file", event.exception);
    }

    @Subscribe
    private void handleShowHelpEvent(ShowHelpRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        mainWindow.handleHelp();
    }

    @Subscribe
    private void handleJumpToListRequestEvent(JumpToListRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        char listType = event.targetIndex.getTaskChar();
        if (listType == 'e' || listType == 'E') {
            mainWindow.getEventListPanel().scrollTo(event.targetIndex.getTaskNumber());
            mainWindow.getFloatListPanel().clearSelection();
            mainWindow.getTaskListPanel().clearSelection();
            mainWindow.getCompleteListPanel().clearSelection();
        } else if (listType == 'f' || listType == 'F') {
            mainWindow.getFloatListPanel().scrollTo(event.targetIndex.getTaskNumber());
            mainWindow.getEventListPanel().clearSelection();
            mainWindow.getTaskListPanel().clearSelection();
            mainWindow.getCompleteListPanel().clearSelection();
        } else if (listType == 'd' || listType == 'D') {
            mainWindow.getTaskListPanel().scrollTo(event.targetIndex.getTaskNumber());
            mainWindow.getFloatListPanel().clearSelection();
            mainWindow.getEventListPanel().clearSelection();
            mainWindow.getCompleteListPanel().clearSelection();
        } else if (listType == 'c' || listType == 'C') {
            mainWindow.getCompleteListPanel().scrollTo(event.targetIndex.getTaskNumber());
            mainWindow.getFloatListPanel().clearSelection();
            mainWindow.getTaskListPanel().clearSelection();
            mainWindow.getEventListPanel().clearSelection();
        }
    }

    @Subscribe
    private void handleTaskPanelSelectionChangedEvent(TaskPanelSelectionChangedEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        // mainWindow.loadTaskPage(event.getNewSelection());
    }

  //@@author A0143648Y
    @Subscribe
    private void handleSelectMultipleTargetEvent(SelectMultipleTargetEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        for (int count = event.targetIndexes.size() - 1; count >= 0; count--) {
            char listType = event.targetIndexes.get(count).getTaskChar();
            if (count == event.targetIndexes.size() - 1) {
                mainWindow.getEventListPanel().clearSelection();
                mainWindow.getFloatListPanel().clearSelection();
                mainWindow.getTaskListPanel().clearSelection();
                mainWindow.getCompleteListPanel().clearSelection();
            }
            if (listType == 'e' || listType == 'E') {
                mainWindow.getEventListPanel().selectTheTarget(event.targetIndexes.get(count).getTaskNumber() - 1);
            } else if (listType == 'f' || listType == 'F') {
                mainWindow.getFloatListPanel().selectTheTarget(event.targetIndexes.get(count).getTaskNumber() - 1);
            } else if (listType == 'd' || listType == 'D') {
                mainWindow.getTaskListPanel().selectTheTarget(event.targetIndexes.get(count).getTaskNumber() - 1);
            } else if (listType == 'c' || listType == 'C') {
                mainWindow.getCompleteListPanel().selectTheTarget(event.targetIndexes.get(count).getTaskNumber() - 1);
            }
        }
    }

    @Subscribe
    private void handleClearAllSelectionsEvent(ClearAllSelectionsEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        mainWindow.getEventListPanel().clearSelection();
        mainWindow.getFloatListPanel().clearSelection();
        mainWindow.getTaskListPanel().clearSelection();
        mainWindow.getCompleteListPanel().clearSelection();
    }
}
