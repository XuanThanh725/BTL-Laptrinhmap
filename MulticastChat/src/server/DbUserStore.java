package server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public class DbUserStore {
    private final String url;

    public DbUserStore(String sqliteUrl) throws Exception {
        this.url = sqliteUrl;
        init();
    }

    private void init() throws Exception {
        try (Connection c = DriverManager.getConnection(url);
             Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  username TEXT UNIQUE NOT NULL,
                  pass_hash TEXT NOT NULL,
                  salt TEXT NOT NULL,
                  role TEXT NOT NULL DEFAULT 'user',
                  created_at INTEGER NOT NULL,
                  last_seen  INTEGER NOT NULL)""");
        }
    }

    public synchronized boolean createUser(String u,String p,String role) throws Exception {
        String salt = Long.toHexString(Double.doubleToLongBits(Math.random()));
        String hash = hash(p, salt);
        long now = Instant.now().getEpochSecond();
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users(username,pass_hash,salt,role,created_at,last_seen) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, u); ps.setString(2, hash); ps.setString(3, salt);
            ps.setString(4, role==null?"user":role); ps.setLong(5, now); ps.setLong(6, now);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public synchronized Optional<String> verifyLogin(String u,String p) throws Exception {
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement("SELECT pass_hash,salt,role FROM users WHERE username=?")) {
            ps.setString(1,u);
            try(ResultSet rs=ps.executeQuery()){
                if(!rs.next()) return Optional.empty();
                String hash=rs.getString(1), salt=rs.getString(2), role=rs.getString(3);
                if(hash.equals(hash(p,salt))) return Optional.of(role);
                return Optional.empty();
            }
        }
    }

    private static String hash(String p,String salt)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] out=md.digest(p.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb=new StringBuilder(); for(byte b:out) sb.append(String.format("%02x",b));
        return sb.toString();
    }
}
