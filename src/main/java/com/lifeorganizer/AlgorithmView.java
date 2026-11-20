package com.lifeorganizer;
import javafx.geometry.*;import javafx.scene.control.*;import javafx.scene.layout.*;import java.util.*;import java.sql.*;
public class AlgorithmView extends VBox {
 public AlgorithmView(int userId){
  setSpacing(12);setPadding(new Insets(22));Label h=new Label("ADA Algorithm Lab");h.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#17324d;");
  Label intro=new Label("Run searching, sorting, priority queue and task scheduling algorithms. Use comma-separated integers or load task priorities from your database.");
  TextField input=new TextField("8, 3, 6, 1, 9, 2");ComboBox<String> choice=new ComboBox<>();
  choice.getItems().addAll("Linear Search","Binary Search","Bubble Sort","Selection Sort","Insertion Sort","Merge Sort","Quick Sort","Priority Queue","Task Scheduling","0/1 Knapsack (Dynamic Programming)","Fractional Knapsack (Greedy)","Activity Selection (Greedy)");
  choice.getSelectionModel().select("Merge Sort");TextField target=new TextField("6");target.setPromptText("Search target");
  Button loadTasks=new Button("Load my task priorities");Button run=new Button("Run");TextArea out=new TextArea();out.setEditable(false);out.setPrefRowCount(18);
  loadTasks.setOnAction(e->{try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement("SELECT priority FROM tasks WHERE user_id=? AND status<>'Completed' ORDER BY priority,deadline")){p.setInt(1,userId);StringJoiner j=new StringJoiner(",");p.setInt(1,userId);try(ResultSet r=p.executeQuery()){while(r.next())j.add(r.getString(1));}input.setText(j.toString());out.setText("Loaded pending task priorities from your account.");}catch(Exception ex){out.setText("Could not load tasks: "+ex.getMessage());}});
  run.setOnAction(e->execute(input.getText(),choice.getValue(),target.getText(),out));
  HBox controls=new HBox(8,choice,target,run,loadTasks);getChildren().addAll(h,intro,input,controls,out);
 }
 private void execute(String raw,String name,String target,TextArea out){
  try{
   List<Integer>a=new ArrayList<>();for(String s:raw.split(","))if(!s.isBlank())a.add(Integer.parseInt(s.trim()));
   if(a.isEmpty())throw new IllegalArgumentException();String before=a.toString();long start=System.nanoTime(),comparisons=0;String result="",complexity="";
   switch(name){
    case "Linear Search"->{int ix=Algorithms.linearSearch(a,Integer.parseInt(target));result="Found index: "+ix;complexity="Best O(1), average/worst O(n); space O(1).";}
    case "Binary Search"->{Algorithms.mergeSort(a);int ix=Algorithms.binarySearch(a,Integer.parseInt(target));result="Sorted data: "+a+"\nFound index: "+ix;complexity="Best O(1), average/worst O(log n); space O(1). Input is sorted before search.";}
    case "Bubble Sort"->{comparisons=Algorithms.bubbleSort(a);result=a.toString();complexity="Best O(n) with early exit; average/worst O(n²); space O(1).";}
    case "Selection Sort"->{comparisons=Algorithms.selectionSort(a);result=a.toString();complexity="Best/average/worst O(n²); space O(1).";}
    case "Insertion Sort"->{comparisons=Algorithms.insertionSort(a);result=a.toString();complexity="Best O(n); average/worst O(n²); space O(1).";}
    case "Merge Sort"->{comparisons=Algorithms.mergeSort(a);result=a.toString();complexity="Best/average/worst O(n log n); extra space O(n).";}
    case "Quick Sort"->{comparisons=Algorithms.quickSort(a);result=a.toString();complexity="Average O(n log n), worst O(n²); recursion space average O(log n).";}
    case "Priority Queue"->{PriorityQueue<Integer>q=new PriorityQueue<>();q.addAll(a);List<Integer>o=new ArrayList<>();while(!q.isEmpty())o.add(q.poll());result="Ascending priority removal order: "+o;complexity="Insert/remove O(log n), peek O(1); storage O(n).";}
    case "Task Scheduling"->{List<Integer>idx=new ArrayList<>();for(int i=0;i<a.size();i++)idx.add(i);idx.sort(Comparator.comparingInt(a::get));result="Task order by priority (1 is highest): "+idx.stream().map(i->"task#"+(i+1)+"[priority "+a.get(i)+"]").toList();complexity="Sort-based scheduling O(n log n); output space O(n).";}
    case "0/1 Knapsack (Dynamic Programming)"->{int[] wt={2,3,4,5},val={3,4,5,8};int cap=8;int[][]dp=new int[wt.length+1][cap+1];for(int i=1;i<=wt.length;i++)for(int w=0;w<=cap;w++){dp[i][w]=dp[i-1][w];if(wt[i-1]<=w)dp[i][w]=Math.max(dp[i][w],val[i-1]+dp[i-1][w-wt[i-1]]);}result="Sample weights="+Arrays.toString(wt)+", values="+Arrays.toString(val)+", capacity="+cap+"\nDP maximum value="+dp[wt.length][cap]+"\nDP table:\n"+Arrays.deepToString(dp);complexity="Time O(nW), space O(nW); pseudo-polynomial in capacity W.";}
    case "Fractional Knapsack (Greedy)"->{double[]wt={10,20,30},val={60,100,120};double cap=50,total=0;StringBuilder chosen=new StringBuilder();Integer[]ix={0,1,2};Arrays.sort(ix,(x,y)->Double.compare(val[y]/wt[y],val[x]/wt[x]));for(int i:ix){double take=Math.min(1,cap/wt[i]);if(take<=0)break;total+=take*val[i];cap-=take*wt[i];chosen.append("item ").append(i+1).append(" fraction ").append(take).append("; ");}result="Selected: "+chosen+"\nMaximum value="+total;complexity="Time O(n log n) due to ratio sorting; space O(n). Greedy is optimal for fractional, not 0/1, knapsack.";}
    case "Activity Selection (Greedy)"->{int[]st={1,3,0,5,8,5},en={2,4,6,7,9,9};Integer[]ix={0,1,2,3,4,5};Arrays.sort(ix,Comparator.comparingInt(i->en[i]));List<Integer>pick=new ArrayList<>();int last=-1;for(int i:ix)if(st[i]>=last){pick.add(i+1);last=en[i];}result="Activities (start,end): "+pick.stream().map(i->"A"+i+"("+st[i-1]+","+en[i-1]+")").toList();complexity="Time O(n log n) with sorting; O(n) if already sorted by finish time; space O(n).";}
   }
   long elapsed=System.nanoTime()-start;
   out.setText("ALGORITHM: "+name+"\nINPUT: "+before+"\nOUTPUT: "+result+"\nCOMPARISONS (where counted): "+comparisons+"\nEXECUTION TIME: "+elapsed+" ns\n\nCOMPLEXITY: "+complexity+"\n\nNote: timings are machine- and input-dependent. Use larger datasets for meaningful performance comparisons.");
  }catch(Exception ex){out.setText("Invalid input. Enter comma-separated whole numbers. For search, enter a valid target.");}
 }
}
