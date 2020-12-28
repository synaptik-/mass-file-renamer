package com.synaptik.massfilerenamer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Controller implements Initializable {
    @FXML
    public Button closeButton;

    @FXML
    public Button resetButton;

    @FXML
    public Button runButton;

    @FXML
    public TextField folderField;

    @FXML
    public TextField regexField;

    @FXML
    public TextField replaceField;

    @FXML
    public TableView<TransformHolder> tableView;

    @FXML
    public TableColumn<TransformHolder, String> beforeColumn;

    @FXML
    public TableColumn<TransformHolder, String> afterColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        beforeColumn.setCellValueFactory(new PropertyValueFactory("before"));
        afterColumn.setCellValueFactory(new PropertyValueFactory("after"));
        tableView.setRowFactory(row -> new TableRow<TransformHolder>() {
            @Override
            protected void updateItem(TransformHolder item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null) {
                    return;
                }

                if (item.getBefore().equals(item.getAfter())) {
                    setStyle("");
                } else {
                    setStyle("-fx-background-color: yellow");
                }
            }
        });

        // We don't want certain characters to be typed in since they are not allowed within filenames.
        // See: https://stackoverflow.com/questions/1976007/what-characters-are-forbidden-in-windows-and-linux-directory-names
        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (change.getText().length() == 0 || change.getText().matches("[^\\\\/<>:\"|?*]")) {
                return change;
            }
            return null;
        };
        replaceField.setTextFormatter(new TextFormatter<String>(filter));
    }

    public void onQuitClicked(final ActionEvent e) {
        Stage stage = (Stage)closeButton.getScene().getWindow();
        stage.close();
    }

    public void onResetClicked(final ActionEvent e) {
        regexField.setText("");
        replaceField.setText("");

        refreshAfterColumn();
        updateButtonStates();
    }

    public void onRunClicked(final ActionEvent e) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to run this operation?", ButtonType.YES, ButtonType.NO);
        dialog.setHeaderText("");
        dialog.showAndWait();

        if (dialog.getResult() == ButtonType.YES) {
            String rootFolder = folderField.getText();
            for (TransformHolder holder : tableView.getItems()) {
                if (!holder.getBefore().equals(holder.getAfter())) {
                    File beforeFile = new File(rootFolder + File.separator + holder.getBefore());
                    File afterFile = new File(rootFolder + File.separator + holder.getAfter());

                    if (beforeFile.renameTo(afterFile)) {
                        holder.setBefore(holder.getAfter());
                    }
                }
            }

            tableView.refresh();
        }
    }

    public void onFolderClicked(final MouseEvent e) {
        Window window = folderField.getScene().getWindow();
        DirectoryChooser chooser = new DirectoryChooser();

        File selected = chooser.showDialog(window);
        if (selected != null) {
            folderField.setText(selected.getAbsolutePath());
            resetGrid();
            updateButtonStates();
        }
    }

    public void onFindKeyReleased(final KeyEvent e) {
        updateButtonStates();
        refreshAfterColumn();
    }

    public void onReplaceKeyReleased(final KeyEvent e) {
        updateButtonStates();
        refreshAfterColumn();
    }

    private void updateButtonStates() {
        resetButton.setDisable(regexField.getText().length() == 0 && replaceField.getText().length() == 0);
        runButton.setDisable(regexField.getText().length() == 0 || replaceField.getText().length() == 0 || folderField.getText().length() == 0);
    }

    private void resetGrid() {
        File selectedFolder = new File(folderField.getText());
        File[] files = selectedFolder.listFiles();
        if (files == null) {
            return;
        }

        tableView.getItems().clear();

        for (File file : files) {
            TransformHolder holder = new TransformHolder();
            holder.setBefore(file.getName());
            holder.setAfter(getTransform(file.getName()));

            tableView.getItems().add(holder);
        }
    }

    private void refreshAfterColumn() {
        regexField.setStyle("");
        try {
            Pattern.compile(regexField.getText());
        } catch (PatternSyntaxException ex) {
            // Safe to ignore. The user is likely in the middle of typing a regex
            regexField.setStyle("-fx-text-fill: red;");
            return;
        }

        try {
            replaceField.setStyle("");
            for (TransformHolder holder : tableView.getItems()) {
                holder.setAfter(getTransform(holder.getBefore()));
            }
        } catch (IllegalArgumentException ex) {
            // Safe to ignore. Likely the replaceField is invalid (e.g. missing group index)
            replaceField.setStyle("-fx-text-fill: red;");
        }

        tableView.refresh();
    }

    private String getTransform(String input) {
        String regex = regexField.getText();
        String replace = replaceField.getText();
        String result = input.replaceAll(regex, replace);

        return result.trim();
    }
}
