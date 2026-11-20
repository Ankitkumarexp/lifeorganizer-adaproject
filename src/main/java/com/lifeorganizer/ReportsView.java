package com.lifeorganizer;
import javafx.geometry.*;import javafx.scene.control.*;import javafx.scene.layout.*;import java.sql.*;import javafx.stage.*;import java.io.*;
public class ReportsView extends VBox {
 public ReportsView(int userId){
  setSpacing(12);setPadding(new Insets(22));Label h=new Label("Reports & Activity");h.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
  TextArea area=new TextArea();area.setEditable(false);area.setPrefRowCount(22);
  Button refresh=new Button("Refresh reports"),export=new Button("Export activity CSV");
  refresh.setOnAction(e->area.setText(report(userId)));export.setOnAction(e->exportAudit(userId));
  getChildren().addAll(h,new Label("Database-driven totals and recent account activity."),new HBox(8,refresh,export),area);area.setText(report(userId));
 }
 private String report(int id){
  StringBuilder b=new StringBuilder();
  try(Connection c=Db.connect()){
   String[] queries={
    "SELECT category,COALESCE(SUM(amount),0) FROM expenses WHERE user_id=? GROUP BY category ORDER BY 2 DESC",
    "SELECT payment_status,COUNT(*),COALESCE(SUM(amount),0) FROM bills WHERE user_id=? GROUP BY payment_status",
    "SELECT status,COUNT(*) FROM tasks WHERE user_id=? GROUP BY status",
    "SELECT entity_name,action_name,details,created_at FROM audit_logs WHERE user_id=? ORDER BY created_at DESC LIMIT 30"
   };
   b.append("EXPENSES BY CATEGORY\n");try(PreparedStatement p=c.prepareStatement(queries[0])){p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())b.append("  ").append(r.getString(1)).append(": ").append(r.getBigDecimal(2)).append("\n");}}
   b.append("\nBILLS BY PAYMENT STATUS\n");try(PreparedStatement p=c.prepareStatement(queries[1])){p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())b.append("  ").append(r.getString(1)).append(": ").append(r.getInt(2)).append(" bill(s), total ").append(r.getBigDecimal(3)).append("\n");}}
   b.append("\nTASKS BY STATUS\n");try(PreparedStatement p=c.prepareStatement(queries[2])){p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())b.append("  ").append(r.getString(1)).append(": ").append(r.getInt(2)).append("\n");}}
   b.append("\nRECENT AUDIT ACTIVITY\n");try(PreparedStatement p=c.prepareStatement(queries[3])){p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())b.append("  ").append(r.getTimestamp(4)).append(" | ").append(r.getString(1)).append(" | ").append(r.getString(2)).append(" | ").append(r.getString(3)).append("\n");}}
  }catch(Exception e){b.append("Report error: ").append(e.getMessage());}
  return b.toString();
 }
 private void exportAudit(int id){
  FileChooser fc=new FileChooser();fc.setInitialFileName("life-organizer-audit.csv");File f=fc.showSaveDialog(getScene().getWindow());if(f==null)return;
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement("SELECT entity_name,record_id,action_name,details,created_at FROM audit_logs WHERE user_id=? ORDER BY created_at DESC")){
   p.setInt(1,id);try(ResultSet r=p.executeQuery();PrintWriter w=new PrintWriter(f)){w.println("entity,record_id,action,details,created_at");while(r.next())w.printf("\"%s\",%s,\"%s\",\"%s\",%s%n",r.getString(1),r.getString(2),r.getString(3),r.getString(4).replace("\"","'"),r.getTimestamp(5));}
  }catch(Exception ex){new Alert(Alert.AlertType.ERROR,"Export failed: "+ex.getMessage()).showAndWait();}
 }
}
