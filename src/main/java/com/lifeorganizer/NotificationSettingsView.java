package com.lifeorganizer;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationSettingsView extends BorderPane {
    private static final Map<String,String> MODULES = new LinkedHashMap<>();
    static {
        MODULES.put("medicines", "Medicines"); MODULES.put("calendar", "Calendar");
        MODULES.put("tasks", "Tasks"); MODULES.put("bills", "Bills");
        MODULES.put("expenses", "Expenses"); MODULES.put("groceries", "Groceries");
        MODULES.put("documents", "Documents");
    }
    public NotificationSettingsView(int userId) {
        setPadding(new Insets(22)); setStyle("-fx-background-color:#f4f6f8;");
        Label title = new Label("NOTIFICATION SETTINGS"); title.setStyle("-fx-font-size:25px;-fx-font-weight:bold;-fx-text-fill:#17324d;");
        Label info = new Label("Choose popup and sound independently for each module. Preferences are saved on this Windows user account.");
        GridPane grid = new GridPane(); grid.setHgap(24); grid.setVgap(13); grid.setPadding(new Insets(18));
        grid.addRow(0, new Label("MODULE"), new Label("POPUP"), new Label("SOUND"));
        int row=1;
        for (var entry : MODULES.entrySet()) {
            CheckBox popup = new CheckBox("Show popup"), sound = new CheckBox("Play sound");
            popup.setSelected(NotificationSettings.popup(userId, entry.getKey())); sound.setSelected(NotificationSettings.sound(userId, entry.getKey()));
            Runnable save = () -> NotificationSettings.set(userId, entry.getKey(), popup.isSelected(), sound.isSelected());
            popup.setOnAction(e -> save.run()); sound.setOnAction(e -> save.run());
            grid.addRow(row++, new Label(entry.getValue()), popup, sound);
        }
        Button test = new Button("Test Sound");

test.setOnAction(e -> {
    try {
        String soundFile = "C:\\Windows\\Media\\Windows Notify System Generic.wav";
        javax.sound.sampled.AudioInputStream audio =
            javax.sound.sampled.AudioSystem.getAudioInputStream(
                new java.io.File(soundFile)
            );

        javax.sound.sampled.Clip clip =
            javax.sound.sampled.AudioSystem.getClip();

        clip.open(audio);
        clip.start();

    } catch (Exception ex) {
        new Alert(Alert.AlertType.ERROR,
            "Sound error: " + ex.getMessage()).showAndWait();
    }
});
        VBox box = new VBox(12,title,info,grid,test); setTop(box);
    }
}
