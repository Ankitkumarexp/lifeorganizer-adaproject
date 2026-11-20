package com.lifeorganizer;

import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.*;
import java.sql.*;
import java.util.*;

public class ModuleView extends BorderPane {
 public record Field(String name,String label,String type,boolean required) {}
 private final int userId; private final String table,title,idColumn; private final List<Field> fields;
 private final TableView<ObservableList<String>> grid=new TableView<>();
 private final ObservableList<ObservableList<String>> data=FXCollections.observableArrayList();
 private final TextField search=new TextField(); private final ComboBox<String> sort=new ComboBox<>();
 private final Label message=new Label();
 public ModuleView(int userId,String table,String title,String idColumn,List<Field>fields){
  this.userId=userId;this.table=table;this.title=title;this.idColumn=idColumn;this.fields=fields;
  setPadding(new Insets(20));setStyle("-fx-background-color:#f4f6f8;");
  Label h=new Label(title);h.setStyle("-fx-font-size:25px;-fx-font-weight:bold;-fx-text-fill:#17324d;");
  search.setPromptText("Search visible records...");
  Button add=button("Add record"),edit=button("Edit selected"),del=button("Delete selected"),refresh=button("Refresh"),csv=button("Export CSV");
  add.setOnAction(e->edit(null));edit.setOnAction(e->{if(grid.getSelectionModel().getSelectedItem()!=null)edit(grid.getSelectionModel().getSelectedItem());});
  del.setOnAction(e->delete());refresh.setOnAction(e->load());csv.setOnAction(e->export());
  sort.getItems().addAll("Default (newest first)","Name A to Z","Name Z to A");
  sort.getSelectionModel().selectFirst();sort.setOnAction(e->applyFilter());
  search.textProperty().addListener((o,a,b)->applyFilter());
  HBox tools=new HBox(8,add,edit,del,refresh,csv);tools.setAlignment(Pos.CENTER_LEFT);
  HBox filters=new HBox(8,new Label("Search"),search,new Label("Sort"),sort);filters.setAlignment(Pos.CENTER_LEFT);
  VBox top=new VBox(12,h,new Label("Records are restricted to the signed-in account."),tools,filters);
  setTop(top);setCenter(grid);setBottom(message);message.setPadding(new Insets(10,0,0,0));load();
 }
 private Button button(String s){Button b=new Button(s);b.setStyle("-fx-background-color:#17324d;-fx-text-fill:white;-fx-padding:9 14;");return b;}
 private void load(){
  data.clear();grid.getColumns().clear();
  String sql="SELECT "+idColumn+", "+String.join(", ",fields.stream().map(Field::name).toList())+" FROM "+table+" WHERE user_id=? ORDER BY "+idColumn+" DESC";
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement(sql)){
   p.setInt(1,userId);try(ResultSet r=p.executeQuery()){
    ResultSetMetaData md=r.getMetaData();int n=md.getColumnCount();
    for(int i=1;i<=n;i++){final int ix=i-1;TableColumn<ObservableList<String>,String> col=new TableColumn<>(md.getColumnLabel(i).replace('_',' ').toUpperCase());
     col.setCellValueFactory(v->new javafx.beans.property.SimpleStringProperty(v.getValue().get(ix)));col.setPrefWidth(155);grid.getColumns().add(col);}
    while(r.next()){ObservableList<String> row=FXCollections.observableArrayList();for(int i=1;i<=n;i++)row.add(Objects.toString(r.getString(i),""));data.add(row);}
   }
   applyFilter();message.setText(data.size()+" record(s) loaded.");
  }catch(Exception ex){message.setText("Database error: "+ex.getMessage());}
 }
 private void applyFilter(){
  String q=search.getText()==null?"":search.getText().toLowerCase(Locale.ROOT);
  List<ObservableList<String>> filtered=new ArrayList<>();
  for(ObservableList<String> row:data)if(String.join(" ",row).toLowerCase(Locale.ROOT).contains(q))filtered.add(row);
  String s=sort.getValue();
  if(s!=null&&s.startsWith("Name")&&filtered.size()>0){
   Comparator<ObservableList<String>> cmp=Comparator.comparing(r->r.size()>1?r.get(1).toLowerCase(Locale.ROOT):"");
   if(s.endsWith("Z to A"))cmp=cmp.reversed();filtered.sort(cmp);
  }
  grid.setItems(FXCollections.observableArrayList(filtered));
 }
 private void edit(ObservableList<String> selected){
  Dialog<Map<String,String>> d=new Dialog<>();d.setTitle((selected==null?"Add ":"Edit ")+title);
  ButtonType save=new ButtonType("Save",ButtonBar.ButtonData.OK_DONE);d.getDialogPane().getButtonTypes().addAll(save,ButtonType.CANCEL);
  GridPane form=new GridPane();form.setHgap(12);form.setVgap(10);form.setPadding(new Insets(16));
  Map<String,TextField> inputs=new LinkedHashMap<>();
  for(int i=0;i<fields.size();i++){Field f=fields.get(i);TextField t=new TextField();
   if(selected!=null&&i+1<selected.size())t.setText(selected.get(i+1));
   if(f.type().equals("date"))t.setPromptText("YYYY-MM-DD");else if(f.type().equals("time"))t.setPromptText("HH:MM:SS");
   form.addRow(i,new Label(f.label()+(f.required()?" *":"")),t);inputs.put(f.name(),t);
  }
  d.getDialogPane().setContent(form);
  d.setResultConverter(bt->{if(bt!=save)return null;Map<String,String>v=new LinkedHashMap<>();inputs.forEach((k,t)->v.put(k,t.getText().trim()));return v;});
  d.showAndWait().ifPresent(v->save(v,selected));
 }
 private void save(Map<String,String>v,ObservableList<String>selected){
  for(Field f:fields){String x=v.get(f.name());if(f.required()&&(x==null||x.isBlank())){message.setText(f.label()+" is required.");return;}
   try{if(!x.isBlank()&&f.type().equals("number"))new java.math.BigDecimal(x);
    if(!x.isBlank()&&f.type().equals("integer")){int n=Integer.parseInt(x);if(f.name().equals("priority")&&(n<1||n>5))throw new IllegalArgumentException("Priority must be 1 to 5.");}
    if(!x.isBlank()&&f.type().equals("date"))java.time.LocalDate.parse(x);
    if(!x.isBlank()&&f.type().equals("time"))java.time.LocalTime.parse(x);
   }catch(Exception ex){message.setText("Invalid "+f.label()+". Check its format.");return;}
  }
  try(Connection c=Db.connect()){
   Db.setUser(c,userId);
   if(selected==null){
    String cols=String.join(",",fields.stream().map(Field::name).toList());String marks=String.join(",",Collections.nCopies(fields.size(),"?"));
    try(PreparedStatement p=c.prepareStatement("INSERT INTO "+table+"(user_id,"+cols+") VALUES(?,"+marks+")",Statement.RETURN_GENERATED_KEYS)){
     p.setInt(1,userId);int i=2;for(Field f:fields)p.setString(i++,v.get(f.name()));p.executeUpdate();
     try(ResultSet keys=p.getGeneratedKeys()){long id=keys.next()?keys.getLong(1):0;Audit.log(userId,table,id,"CREATE","Created "+title+" record");}
    }
   }else{
    String set=String.join(",",fields.stream().map(f->f.name()+"=?").toList());
    try(PreparedStatement p=c.prepareStatement("UPDATE "+table+" SET "+set+" WHERE "+idColumn+"=? AND user_id=?")){
     int i=1;for(Field f:fields)p.setString(i++,v.get(f.name()));p.setString(i++,selected.get(0));p.setInt(i,userId);p.executeUpdate();
     Audit.log(userId,table,Long.parseLong(selected.get(0)),"UPDATE","Updated "+title+" record");
    }
   }
   message.setText("Saved successfully.");load();
  }catch(Exception ex){message.setText("Save failed: "+ex.getMessage());}
 }
 private void delete(){
  ObservableList<String> row=grid.getSelectionModel().getSelectedItem();if(row==null){message.setText("Select a record first.");return;}
  if(new Alert(Alert.AlertType.CONFIRMATION,"Delete the selected record?",ButtonType.YES,ButtonType.NO).showAndWait().orElse(ButtonType.NO)!=ButtonType.YES)return;
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement("DELETE FROM "+table+" WHERE "+idColumn+"=? AND user_id=?")){
   p.setString(1,row.get(0));p.setInt(2,userId);p.executeUpdate();Audit.log(userId,table,Long.parseLong(row.get(0)),"DELETE","Deleted "+title+" record");load();
  }catch(Exception ex){message.setText("Delete failed: "+ex.getMessage());}
 }
 private void export(){
  FileChooser fc=new FileChooser();fc.setTitle("Export "+title);fc.setInitialFileName(table+".csv");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files","*.csv"));
  File f=fc.showSaveDialog(getScene().getWindow());if(f==null)return;
  try(PrintWriter w=new PrintWriter(new OutputStreamWriter(new FileOutputStream(f),java.nio.charset.StandardCharsets.UTF_8))){
   w.println(String.join(",",grid.getColumns().stream().map(TableColumn::getText).toList()));
   for(ObservableList<String> r:grid.getItems())w.println(r.stream().map(x->"\""+x.replace("\"","\"\"")+"\"").reduce((a,b)->a+","+b).orElse(""));
   message.setText("Exported to "+f.getAbsolutePath());
  }catch(Exception ex){message.setText("Export failed: "+ex.getMessage());}
 }
}
