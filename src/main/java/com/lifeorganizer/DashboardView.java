package com.lifeorganizer;
import javafx.geometry.*;import javafx.scene.control.*;import javafx.scene.layout.*;import java.sql.*;
public class DashboardView extends VBox {
 public DashboardView(int userId,Runnable openTasks,Runnable openBills){
  setSpacing(18);setPadding(new Insets(22));setStyle("-fx-background-color:#f4f6f8;");
  Label h=new Label("Overview");h.setStyle("-fx-font-size:27px;-fx-font-weight:bold;-fx-text-fill:#17324d;");
  Label subtitle=new Label("A clear view of your personal organization.");
  GridPane cards=new GridPane();cards.setHgap(14);cards.setVgap(14);
  String[] labels={"Grocery items","Purchased groceries","Unpaid bills","Total expenses","Active tasks","High-priority tasks","Overdue tasks","Upcoming events"};
  String[] vals=metrics(userId);
  for(int i=0;i<labels.length;i++){VBox card=new VBox(8,new Label(labels[i]),new Label(vals[i]));card.setPadding(new Insets(18));card.setPrefWidth(210);card.setStyle("-fx-background-color:white;-fx-border-color:#dce3e9;-fx-border-radius:5;");
   ((Label)card.getChildren().get(0)).setStyle("-fx-text-fill:#536273;");((Label)card.getChildren().get(1)).setStyle("-fx-font-size:25px;-fx-font-weight:bold;-fx-text-fill:#17324d;");cards.add(card,i%4,i/4);}
  Button tasks=new Button("View tasks");tasks.setOnAction(e->openTasks.run());Button bills=new Button("View bills");bills.setOnAction(e->openBills.run());
  HBox actions=new HBox(10,tasks,bills);
  Label note=new Label("Dashboard values are calculated from your saved database records.");
  getChildren().addAll(h,subtitle,cards,actions,note);
 }
 private String[] metrics(int id){
  String[] v={"0","0","0","0.00","0","0","0","0"};
  String sql="SELECT (SELECT COUNT(*) FROM groceries WHERE user_id=?),(SELECT COUNT(*) FROM groceries WHERE user_id=? AND purchased_status='Purchased'),(SELECT COUNT(*) FROM bills WHERE user_id=? AND payment_status='Unpaid'),(SELECT COALESCE(SUM(amount),0) FROM expenses WHERE user_id=?),(SELECT COUNT(*) FROM tasks WHERE user_id=? AND status<>'Completed'),(SELECT COUNT(*) FROM tasks WHERE user_id=? AND priority<=2 AND status<>'Completed'),(SELECT COUNT(*) FROM tasks WHERE user_id=? AND status<>'Completed' AND deadline<CURDATE()),(SELECT COUNT(*) FROM calendar_events WHERE user_id=? AND event_date>=CURDATE())";
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement(sql)){for(int i=1;i<=8;i++)p.setInt(i,id);try(ResultSet r=p.executeQuery()){if(r.next())for(int i=0;i<8;i++)v[i]=r.getString(i+1);}}catch(Exception e){v[0]="DB error";}
  return v;
 }
}
