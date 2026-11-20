package com.lifeorganizer;
import java.sql.*;
public final class Audit {
 private Audit(){}
 public static void log(int userId,String entity,long recordId,String action,String details){
  try(Connection c=Db.connect();PreparedStatement p=c.prepareStatement(
   "INSERT INTO audit_logs(user_id,entity_name,record_id,action_name,details) VALUES(?,?,?,?,?)")){
   p.setInt(1,userId);p.setString(2,entity);p.setLong(3,recordId);p.setString(4,action);p.setString(5,details);p.executeUpdate();
  }catch(SQLException ignored){/* Audit failure must not crash ordinary user operations. */}
 }
}
