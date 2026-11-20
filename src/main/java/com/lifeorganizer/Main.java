package com.lifeorganizer;
import javafx.application.*;import javafx.geometry.*;import javafx.scene.*;import javafx.scene.control.*;import javafx.scene.layout.*;import javafx.stage.*;import javafx.animation.*;import javafx.util.Duration;import java.sql.*;import java.time.*;import java.util.*;
public class Main extends Application {
 private Stage stage;private Timeline reminderTimer;private int currentUser;private String lastReminderKey="";
 @Override public void start(Stage s){
  stage=s;stage.setMinWidth(1000);stage.setMinHeight(650);
  try{
   String[] startup=resolveStartupUser();
   if(startup==null){showStartupError("No account was found in the life_organizer database.");return;}
   launchApp(Integer.parseInt(startup[0]),startup[1]);
  }catch(SQLException ex){
   showStartupError("Could not connect to MySQL. Set LIFEORG_DB_USER and LIFEORG_DB_PASSWORD to your existing MySQL credentials, then restart the app.\n\n"+ex.getMessage());
  }
 }
 private String[] resolveStartupUser() throws SQLException {
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement("SELECT user_id,name,email FROM users ORDER BY user_id LIMIT 1");ResultSet r=p.executeQuery()){
   if(r.next())return new String[]{String.valueOf(r.getInt(1)),r.getString(3)};
   return null;
  }
 }
 private void showStartupError(String message){
  Alert alert=new Alert(Alert.AlertType.ERROR);
  alert.setTitle("Database connection required");
  alert.setHeaderText("Life Organizer could not start");
  alert.setContentText(message);
  alert.showAndWait();
  Platform.exit();
 }
 private void launchApp(int id,String email){
  currentUser=id;
  BorderPane root=new BorderPane();root.setStyle("-fx-background-color:#f4f6f8;");
  VBox nav=new VBox(7);nav.setPadding(new Insets(22,14,18,14));nav.setPrefWidth(225);nav.setStyle("-fx-background-color:#17324d;");
  Label brand=new Label("LIFE ORGANIZER");brand.setStyle("-fx-text-fill:white;-fx-font-size:18px;-fx-font-weight:bold;");
  Label signed=new Label(email);signed.setStyle("-fx-text-fill:#cbd6df;");nav.getChildren().addAll(brand,signed,new Separator());
  StackPane content=new StackPane();content.setPadding(new Insets(12));
  Runnable[] showDashboard=new Runnable[1];
  Runnable tasks=()->showModule(content,"tasks","TASKS","task_id",List.of(f("task_name","Task name","text",true),f("deadline","Deadline (YYYY-MM-DD)","date",true),f("status","Status: Pending / In Progress / Completed","text",true),f("priority","Priority (1 highest, 5 lowest)","integer",true)));
  Runnable bills=()->showModule(content,"bills","BILLS","bill_id",List.of(f("bill_name","Bill name","text",true),f("amount","Amount","number",true),f("due_date","Due date (YYYY-MM-DD)","date",true),f("payment_status","Payment status: Unpaid / Paid","text",true)));
  showDashboard[0]=()->content.getChildren().setAll(new DashboardView(id,tasks,bills));
  navButton(nav,"Dashboard",showDashboard[0]);
  navButton(nav,"Grocery",()->showModule(content,"groceries","GROCERY","grocery_id",List.of(f("item_name","Item name","text",true),f("category","Category","text",true),f("quantity","Quantity","text",true),f("purchased_status","Purchased status: Pending / Purchased","text",true))));
  navButton(nav,"Bills",bills);
  navButton(nav,"Expenses",()->content.getChildren().setAll(new ExpenseAnalyticsView(id)));
  navButton(nav,"Expense Records",()->showModule(content,"expenses","EXPENSES","expense_id",List.of(f("category","Category","text",true),f("amount","Amount","number",true),f("expense_date","Expense date (YYYY-MM-DD)","date",true),f("notes","Notes","text",false))));
  navButton(nav,"Documents",()->showModule(content,"documents","DOCUMENTS","document_id",List.of(f("document_name","Document name","text",true),f("category","Category","text",true),f("file_path","File path (optional)","text",false),f("notes","Notes","text",false))));
  navButton(nav,"Medicines",()->showModule(content,"medicines","MEDICINES","medicine_id",List.of(f("medicine_name","Medicine name","text",true),f("dosage","Dosage","text",false),f("reminder_time","Reminder time (HH:MM:SS)","time",true),f("notes","Notes","text",false))));
  navButton(nav,"Tasks",tasks);
  navButton(nav,"Calendar",()->content.getChildren().setAll(new CalendarView(id)));
  navButton(nav,"Global Search",()->content.getChildren().setAll(new SearchView(id)));
  navButton(nav,"Reminders & Snooze",()->content.getChildren().setAll(new ReminderCenterView(id)));
  navButton(nav,"Backup & Restore",()->content.getChildren().setAll(new BackupRestoreView(id)));
  navButton(nav,"Notification Settings",()->content.getChildren().setAll(new NotificationSettingsView(id)));
  navButton(nav,"ADA Algorithm Lab",()->content.getChildren().setAll(new AlgorithmView(id)));
  navButton(nav,"Reports & Audit",()->content.getChildren().setAll(new ReportsView(id)));
  navButton(nav,"Medicine reminders",()->showReminders());
  Region spacer=new Region();VBox.setVgrow(spacer,Priority.ALWAYS);nav.getChildren().add(spacer);
  root.setLeft(nav);root.setCenter(content);showDashboard[0].run();stage.setScene(new Scene(root,1280,800));stage.setTitle("Life Organizer | "+email);startReminderChecks();
 }
 private ModuleView.Field f(String n,String l,String t,boolean r){return new ModuleView.Field(n,l,t,r);}
 private void showModule(StackPane c,String table,String title,String id,List<ModuleView.Field>fields){c.getChildren().setAll(new ModuleView(currentUser,table,title,id,fields));}
 private void navButton(VBox nav,String label,Runnable action){Button b=new Button(label);b.setMaxWidth(Double.MAX_VALUE);b.setAlignment(Pos.CENTER_LEFT);b.setStyle("-fx-background-color:transparent;-fx-text-fill:white;-fx-font-size:14px;-fx-padding:10 12;");b.setOnAction(e->action.run());nav.getChildren().add(b);}
 private void startReminderChecks(){
  if(reminderTimer!=null)reminderTimer.stop();
  reminderTimer=new Timeline(new KeyFrame(Duration.seconds(30),e->checkReminders()));reminderTimer.setCycleCount(Animation.INDEFINITE);reminderTimer.play();checkReminders();
 }
 private void checkReminders(){
  LocalDateTime now=LocalDateTime.now().withSecond(0).withNano(0);processExpiredSnoozes(now);String keyTime=now.toString();if(keyTime.equals(lastReminderKey))return;
  System.out.println("REMINDER CHECK RUNNING: " + now);
  try(Connection c=Db.connect()){
    System.out.println("LOGGED-IN USER ID: " + currentUser);
    System.out.println("CHECKING TIME: " + now.toLocalTime());
   try(PreparedStatement p=c.prepareStatement("SELECT medicine_id,medicine_name,dosage,reminder_time FROM medicines WHERE user_id=? AND TIME_FORMAT(reminder_time,'%H:%i')=?")){
    p.setInt(1,currentUser);
    p.setString(2,now.toLocalTime().toString());

    try(ResultSet r=p.executeQuery()){
        while(r.next()){
            System.out.println("MEDICINE FOUND: " + r.getString(2));

            String key="medicine:"+r.getInt(1)+":"+now.toLocalDate();

            notifyIfDue("medicines",key,
                "Medicine: "+r.getString(2)+" | Dosage: "+
                Objects.toString(r.getString(3),"Not specified"));
        }
    }
}
   try(PreparedStatement p=c.prepareStatement("SELECT event_id,event_title,event_date,event_time,reminder_minutes FROM calendar_events WHERE user_id=? AND event_date>=CURDATE() AND event_date<=DATE_ADD(CURDATE(),INTERVAL 30 DAY) AND event_time IS NOT NULL AND reminder_minutes>0")){p.setInt(1,currentUser);try(ResultSet r=p.executeQuery()){while(r.next()){LocalDate eventDate=r.getDate(3).toLocalDate();LocalTime eventTime=r.getTime(4).toLocalTime();LocalDateTime trigger=LocalDateTime.of(eventDate,eventTime).minusMinutes(r.getInt(5));if(trigger.equals(now))notifyIfDue("calendar","event:"+r.getInt(1)+":"+eventDate,"Event: "+r.getString(2));}}}
   if(now.getHour()==9&&now.getMinute()==0){
    try(PreparedStatement p=c.prepareStatement("SELECT task_id,task_name FROM tasks WHERE user_id=? AND deadline=CURDATE() AND status<>'Completed'")){p.setInt(1,currentUser);try(ResultSet r=p.executeQuery()){while(r.next())notifyIfDue("tasks","task:"+r.getInt(1)+":"+now.toLocalDate(),"Task due today: "+r.getString(2));}}
    try(PreparedStatement p=c.prepareStatement("SELECT bill_id,bill_name,amount FROM bills WHERE user_id=? AND due_date=CURDATE() AND payment_status='Unpaid'")){p.setInt(1,currentUser);try(ResultSet r=p.executeQuery()){while(r.next())notifyIfDue("bills","bill:"+r.getInt(1)+":"+now.toLocalDate(),"Bill due today: "+r.getString(2)+" | Amount: ₹"+r.getBigDecimal(3));}}
   }
   lastReminderKey=keyTime;
  }catch(Exception ex){System.err.println("Reminder check failed: "+ex.getMessage());}
 }

 private void processExpiredSnoozes(LocalDateTime now){
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement("SELECT reminder_key FROM reminder_actions WHERE user_id=? AND action_type='SNOOZE' AND snooze_until<=?")){p.setInt(1,currentUser);p.setTimestamp(2,Timestamp.valueOf(now));try(ResultSet r=p.executeQuery()){List<String> keys=new ArrayList<>();while(r.next())keys.add(r.getString(1));for(String key:keys){String module=key.startsWith("medicine:")?"medicines":key.startsWith("event:")?"calendar":key.startsWith("task:")?"tasks":"bills";try(PreparedStatement d=c.prepareStatement("DELETE FROM reminder_actions WHERE user_id=? AND reminder_key=?")){d.setInt(1,currentUser);d.setString(2,key);d.executeUpdate();}notifyIfDue(module,key,"Snoozed reminder: "+key);}}}catch(Exception ex){System.err.println("Snooze check failed: "+ex.getMessage());}
 }
 private void notifyIfDue(String module,String reminderKey,String message){
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement("SELECT action_type,snooze_until FROM reminder_actions WHERE user_id=? AND reminder_key=?")){p.setInt(1,currentUser);p.setString(2,reminderKey);try(ResultSet r=p.executeQuery()){if(r.next()){String action=r.getString(1);Timestamp snooze=r.getTimestamp(2);if("COMPLETED".equals(action)||(snooze!=null&&snooze.toLocalDateTime().isAfter(LocalDateTime.now())))return;}}}catch(Exception ignored){}
  if(!NotificationSettings.popup(currentUser,module)&&!NotificationSettings.sound(currentUser,module))return;
  if(NotificationSettings.sound(currentUser,module)){
    playReminderSound();
}
 if (NotificationSettings.popup(currentUser, module)) {
    System.out.println("POPUP ENABLED: " + NotificationSettings.popup(currentUser, module));
System.out.println("SOUND ENABLED: " + NotificationSettings.sound(currentUser, module));
    Platform.runLater(() -> {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Life Organizer Reminder");
        a.setHeaderText("Medicine Reminder");
        a.setContentText(message);

        ButtonType snooze = new ButtonType("Snooze 10 min");
        ButtonType complete = new ButtonType("Mark completed");

        a.getButtonTypes().setAll(snooze, complete, ButtonType.CLOSE);

        a.showAndWait().ifPresent(bt -> {
            if (bt == snooze) {
                saveReminderAction(reminderKey, "SNOOZE",
                    LocalDateTime.now().plusMinutes(10));
            } else if (bt == complete) {
                saveReminderAction(reminderKey, "COMPLETED", null);
            }
        });
    });
}
}
 private void playReminderSound() {
    try {
        String soundFile =
            "C:\\Windows\\Media\\Windows Notify System Generic.wav";

        javax.sound.sampled.AudioInputStream audio =
            javax.sound.sampled.AudioSystem.getAudioInputStream(
                new java.io.File(soundFile)
            );

        javax.sound.sampled.Clip clip =
            javax.sound.sampled.AudioSystem.getClip();

        clip.open(audio);
        audio.close();

        clip.addLineListener(event -> {
            if (event.getType() ==
                javax.sound.sampled.LineEvent.Type.STOP) {
                clip.close();
            }
        });

        clip.start();

    } catch (Exception ex) {
        System.err.println("Reminder sound error: " + ex.getMessage());
    }
}
 private void saveReminderAction(String key,String action,LocalDateTime until){try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement("INSERT INTO reminder_actions(user_id,reminder_key,action_type,snooze_until) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE action_type=VALUES(action_type),snooze_until=VALUES(snooze_until)")){p.setInt(1,currentUser);p.setString(2,key);p.setString(3,action);if(until==null)p.setNull(4,Types.TIMESTAMP);else p.setTimestamp(4,Timestamp.valueOf(until));p.executeUpdate();}catch(Exception ex){new Alert(Alert.AlertType.ERROR,"Could not save reminder action: "+ex.getMessage()).showAndWait();}}
 private void showReminders(){
  StringBuilder b=new StringBuilder("TODAY'S MEDICINE REMINDERS\n\n");
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement("SELECT medicine_name,dosage,reminder_time FROM medicines WHERE user_id=? ORDER BY reminder_time")){p.setInt(1,currentUser);try(ResultSet r=p.executeQuery()){while(r.next())b.append(r.getTime(3)).append("  ").append(r.getString(1)).append("  ").append(Objects.toString(r.getString(2),"")).append("\n");}}
  catch(Exception e){b.append(e.getMessage());}
  TextArea a=new TextArea(b.toString());a.setEditable(false);a.setPrefRowCount(18);new Alert(Alert.AlertType.INFORMATION).getDialogPane().setContent(a);
  Alert alert=new Alert(Alert.AlertType.INFORMATION);alert.setTitle("Medicine reminders");alert.getDialogPane().setContent(a);alert.showAndWait();
 }
 public static void main(String[]args){launch(args);}
}
