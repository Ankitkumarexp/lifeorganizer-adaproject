package com.lifeorganizer;
import java.sql.*;
import java.util.Properties;
public final class Db {
 private Db(){}
 public static Connection connect() throws SQLException {
  Properties p=new Properties();
  p.setProperty("user",System.getenv().getOrDefault("LIFEORG_DB_USER","root"));
  p.setProperty("password",System.getenv().getOrDefault("LIFEORG_DB_PASSWORD",""));
  p.setProperty("useSSL","false"); p.setProperty("allowPublicKeyRetrieval","true");
  p.setProperty("serverTimezone","UTC");
  String url=System.getenv().getOrDefault("LIFEORG_DB_URL","jdbc:mysql://localhost:3306/life_organizer");
  return DriverManager.getConnection(url,p);
 }
 public static void setUser(Connection c,int userId) throws SQLException {
  try(Statement s=c.createStatement()){s.execute("SET @lifeorganizer_user_id="+userId);}
 }
}
