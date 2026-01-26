package com.ecommerce.util;

import java.sql.*;

public class DataMigrationTool {
    
    private static final String H2_URL = "jdbc:h2:file:./data/ecommerce_db";
    private static final String H2_USER = "SA";
    private static final String H2_PASSWORD = "password";  // Use your actual H2 password
    
    private static final String PG_URL = "jdbc:postgresql://ep-jolly-fire-ahjrykdg-pooler.c-3.us-east-1.aws.neon.tech:5432/neondb?sslmode=require";
    private static final String PG_USER = "neondb_owner";
    private static final String PG_PASSWORD = "npg_CkrEz2Ug7XGw";
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("🔄 Starting Data Migration from H2 to PostgreSQL");
        System.out.println("========================================");
        
        try {
            Class.forName("org.h2.Driver");
            Class.forName("org.postgresql.Driver");
            
            Connection h2Conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
            Connection pgConn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASSWORD);
            
            System.out.println("✅ Connected to both databases");
            
            pgConn.setAutoCommit(false);
            
            // CLEAR EXISTING DATA FIRST
            clearPostgreSQLTables(pgConn);
            
            // Migrate tables
            migrateTableSafe(h2Conn, pgConn, "users");
            migrateTableSafe(h2Conn, pgConn, "products");
            migrateTableSafe(h2Conn, pgConn, "shipping_config");
            migrateTableSafe(h2Conn, pgConn, "orders");
            migrateTableSafe(h2Conn, pgConn, "order_items");
            
            pgConn.commit();
            
            h2Conn.close();
            pgConn.close();
            
            System.out.println("========================================");
            System.out.println("✅ Migration Complete!");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("❌ Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void clearPostgreSQLTables(Connection pg) throws SQLException {
        System.out.println("========================================");
        System.out.println("🗑️ Clearing existing PostgreSQL data...");
        System.out.println("========================================");
        
        Statement stmt = pg.createStatement();
        
        try {
            stmt.execute("TRUNCATE TABLE order_items CASCADE;");
            System.out.println("✅ Cleared order_items");
        } catch (SQLException e) {
            System.out.println("ℹ️ order_items table doesn't exist yet");
        }
        
        try {
            stmt.execute("TRUNCATE TABLE orders CASCADE;");
            System.out.println("✅ Cleared orders");
        } catch (SQLException e) {
            System.out.println("ℹ️ orders table doesn't exist yet");
        }
        
        try {
            stmt.execute("TRUNCATE TABLE products CASCADE;");
            System.out.println("✅ Cleared products");
        } catch (SQLException e) {
            System.out.println("ℹ️ products table doesn't exist yet");
        }
        
        try {
            stmt.execute("TRUNCATE TABLE shipping_config CASCADE;");
            System.out.println("✅ Cleared shipping_config");
        } catch (SQLException e) {
            System.out.println("ℹ️ shipping_config table doesn't exist yet");
        }
        
        try {
            stmt.execute("TRUNCATE TABLE users CASCADE;");
            System.out.println("✅ Cleared users");
        } catch (SQLException e) {
            System.out.println("ℹ️ users table doesn't exist yet");
        }
        
        System.out.println("========================================");
        
        stmt.close();
    }
    
    private static void migrateTableSafe(Connection h2, Connection pg, String tableName) {
        System.out.println("📊 Migrating " + tableName + "...");
        
        try {
            Statement h2Stmt = h2.createStatement();
            ResultSet rs = h2Stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            
            // Build column names
            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    columns.append(", ");
                    placeholders.append(", ");
                }
                columns.append(meta.getColumnName(i));
                placeholders.append("?");
            }
            
            String insertSQL = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
            PreparedStatement pgStmt = pg.prepareStatement(insertSQL);
            
            int count = 0;
            int skipped = 0;
            
            while (rs.next()) {
                try {
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        
                        if (value == null) {
                            pgStmt.setNull(i, meta.getColumnType(i));
                        } else {
                            pgStmt.setObject(i, value);
                        }
                    }
                    
                    pgStmt.executeUpdate();
                    count++;
                    
                } catch (SQLException e) {
                    System.err.println("⚠️ Skipped row in " + tableName + ": " + e.getMessage());
                    skipped++;
                }
            }
            
            System.out.println("✅ Migrated " + count + " rows from " + tableName + (skipped > 0 ? " (skipped " + skipped + ")" : ""));
            
            rs.close();
            h2Stmt.close();
            pgStmt.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error migrating " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}