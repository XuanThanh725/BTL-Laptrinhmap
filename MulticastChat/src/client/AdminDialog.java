package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AdminDialog extends JDialog {
    private final String serverHost;
    private final int ctrlPort;
    private final String sessionId;

    private JTextArea taAnnounce;
    private JButton btnSendAnn;

    private DefaultListModel<String> lmUsers = new DefaultListModel<>();
    private JList<String> lstUsers;
    private JButton btnRefresh, btnKick;

    public AdminDialog(Frame owner, String serverHost, int ctrlPort, String sessionId) {
        super(owner, "Quản trị", true);
        this.serverHost = serverHost;
        this.ctrlPort = ctrlPort;
        this.sessionId = sessionId;
        buildUI();
        setSize(560, 420);
        setLocationRelativeTo(owner);
        refreshUsers();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();

        // --- Tab 1: Announce
        JPanel p1 = new JPanel(new BorderLayout(8,8));
        p1.setBorder(new EmptyBorder(10,10,10,10));
        taAnnounce = new JTextArea(6, 30);
        taAnnounce.setLineWrap(true);
        taAnnounce.setWrapStyleWord(true);
        btnSendAnn = new JButton("Gửi thông báo");
        btnSendAnn.addActionListener(e -> sendAnnounce());
        p1.add(new JScrollPane(taAnnounce), BorderLayout.CENTER);
        p1.add(btnSendAnn, BorderLayout.SOUTH);
        tabs.addTab("Thông báo", p1);

        // --- Tab 2: Kick / List users
        JPanel p2 = new JPanel(new BorderLayout(8,8));
        p2.setBorder(new EmptyBorder(10,10,10,10));
        lstUsers = new JList<>(lmUsers);
        p2.add(new JScrollPane(lstUsers), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRefresh = new JButton("Làm mới");
        btnKick    = new JButton("Kick người chọn");
        actions.add(btnRefresh);
        actions.add(btnKick);
        btnRefresh.addActionListener(e -> refreshUsers());
        btnKick.addActionListener(e -> kickSelected());
        p2.add(actions, BorderLayout.SOUTH);
        tabs.addTab("Người dùng", p2);

        setContentPane(tabs);
    }

    private void sendAnnounce() {
        String msg = taAnnounce.getText().trim();
        if (msg.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập nội dung thông báo.");
            return;
        }
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setSoTimeout(1500);
            String cmd = "ADMIN_ANNOUNCE|" + sessionId + "|" + msg;
            send(ds, cmd);
            String resp = recv(ds);
            if ("OK".equals(resp)) {
                JOptionPane.showMessageDialog(this, "Đã gửi thông báo.");
                taAnnounce.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Thất bại: " + resp, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshUsers() {
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setSoTimeout(1500);
            send(ds, "LIST_USERS|" + sessionId);
            List<String> rows = new ArrayList<>();
            while (true) {
                String s = recv(ds);
                if ("USERS_END".equals(s)) break;
                if (s.startsWith("USER|")) {
                    // USER|username|role|online(0/1)
                    String[] a = s.split("\\|", 5);
                    if (a.length >= 4) {
                        String line = ( "1".equals(a[3]) ? "● " : "○ " ) + a[1] + "  [" + a[2] + "]";
                        rows.add(line);
                    }
                }
            }
            lmUsers.clear();
            for (String r : rows) lmUsers.addElement(r);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi load users: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void kickSelected() {
        String sel = lstUsers.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Chọn một người trong danh sách."); return; }
        // Dòng hiển thị dạng: "● username  [role]" -> tách username
        String username = sel.replace("●","").replace("○","").trim();
        int sp = username.indexOf("  [");
        if (sp > 0) username = username.substring(0, sp).trim();

        int cf = JOptionPane.showConfirmDialog(this, "Kick '" + username + "'?", "Xác nhận", JOptionPane.OK_CANCEL_OPTION);
        if (cf != JOptionPane.OK_OPTION) return;

        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setSoTimeout(1500);
            send(ds, "ADMIN_KICK|" + sessionId + "|" + username);
            String resp = recv(ds);
            if ("OK".equals(resp)) {
                JOptionPane.showMessageDialog(this, "Đã kick " + username);
                refreshUsers();
            } else {
                JOptionPane.showMessageDialog(this, "Thất bại: " + resp, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void send(DatagramSocket ds, String s) throws Exception {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        DatagramPacket out = new DatagramPacket(data, data.length, InetAddress.getByName(serverHost), ctrlPort);
        ds.send(out);
    }
    private String recv(DatagramSocket ds) throws Exception {
        byte[] buf = new byte[1024];
        DatagramPacket r = new DatagramPacket(buf, buf.length);
        ds.receive(r);
        return new String(r.getData(), 0, r.getLength(), StandardCharsets.UTF_8);
    }
}
