package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class LoginDialog extends JDialog {
    private JTextField tfUser;
    private JPasswordField pfPass;
    private JButton btnLogin, btnSignup, btnCancel;

    private final String serverHost;
    private final int ctrlPort;

    public static class Result {
        public final String username;
        public final String session;
        public final String role;
        public Result(String u, String s, String r) { this.username=u; this.session=s; this.role=r; }
    }

    private Result result;

    public LoginDialog(Frame owner, String serverHost, int ctrlPort) {
        super(owner, "Đăng nhập", true);
        this.serverHost = serverHost;
        this.ctrlPort = ctrlPort;
        buildUI();
        setSize(380, 220);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(12,12,8,12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx=0; c.gridy=0; p.add(new JLabel("Tài khoản:"), c);
        c.gridx=1; c.weightx=1; tfUser = new JTextField(); p.add(tfUser, c);

        c.gridx=0; c.gridy=1; c.weightx=0; p.add(new JLabel("Mật khẩu:"), c);
        c.gridx=1; c.weightx=1; pfPass = new JPasswordField(); p.add(pfPass, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnLogin  = new JButton("Đăng nhập");
        btnSignup = new JButton("Đăng ký");
        btnCancel = new JButton("Hủy");
        actions.add(btnSignup);
        actions.add(btnLogin);
        actions.add(btnCancel);

        c.gridx=0; c.gridy=2; c.gridwidth=2; p.add(actions, c);
        setContentPane(p);

        btnLogin.addActionListener(e -> doLogin());
        btnSignup.addActionListener(e -> doSignup());
        btnCancel.addActionListener(e -> { result=null; dispose(); });

        getRootPane().setDefaultButton(btnLogin);
    }

    private void doSignup() {
        String u = tfUser.getText().trim();
        String p = new String(pfPass.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập tài khoản & mật khẩu.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setSoTimeout(1500);
            send(ds, "SIGNUP|" + u + "|" + p);
            String s = recv(ds);
            if ("OK".equals(s)) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công. Đăng nhập lại nhé!");
            } else if (s.startsWith("ERR|EXISTS")) {
                JOptionPane.showMessageDialog(this, "Tên tài khoản đã tồn tại.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Đăng ký thất bại: " + s, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doLogin() {
        String u = tfUser.getText().trim();
        String p = new String(pfPass.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập tài khoản & mật khẩu.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setSoTimeout(1500);
            send(ds, "LOGIN|" + u + "|" + p);
            String s = recv(ds);
            if (s.startsWith("LOGIN_OK|")) {
                String[] a = s.split("\\|", 4);
                String session = a[1];
                String role    = a[2];
                result = new Result(u, session, role);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu.", "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
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

    public Result getResult() { return result; }
}
