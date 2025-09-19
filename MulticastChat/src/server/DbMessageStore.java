package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

public class DbMessageStore implements MessageStore {

    // Cho ControlServer lấy url này dùng chung DB users
    final String url; // ví dụ: jdbc:sqlite:D:\...\data\chat.db

    public DbMessageStore(Path dataDir) throws Exception {
        if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
        this.url = "jdbc:sqlite:" + dataDir.resolve("chat.db").toAbsolutePath();
        init();
    }

    private void init() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection c = DriverManager.getConnection(url);
             Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS messages (" +
                " id   INTEGER PRIMARY KEY AUTOINCREMENT," +
                " room TEXT    NOT NULL," +
                " user TEXT    NOT NULL," +
                " text TEXT    NOT NULL," +
                " ts   INTEGER NOT NULL)"
            );
            st.execute("CREATE INDEX IF NOT EXISTS idx_msg_room_ts ON messages(room, ts)");
        }
    }

    @Override
    public synchronized void append(ChatMessage m) throws Exception {
        final String sql = "INSERT INTO messages(room, user, text, ts) VALUES (?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.room);
            ps.setString(2, m.user);
            ps.setString(3, m.text);
            ps.setLong  (4, m.ts);
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized List<ChatMessage> history(String room, long sinceTs, int limit) throws Exception {
        List<ChatMessage> out = new ArrayList<>();
        final String sql =
            "SELECT room, user, text, ts FROM messages " +
            "WHERE room=? AND ts>? ORDER BY ts ASC LIMIT ?";
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, room);
            ps.setLong  (2, sinceTs);
            ps.setInt   (3, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ChatMessage(
                        rs.getString(1), rs.getString(2),
                        rs.getString(3), rs.getLong(4)
                    ));
                }
            }
        }
        return out;
    }
}
