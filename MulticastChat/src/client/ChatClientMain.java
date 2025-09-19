package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ChatClientMain extends JFrame {

    // ==== cấu hình ====
    private static final String DEFAULT_GROUP      = "239.1.1.1";
    private static final int    DEFAULT_MCAST_PORT = 5000;
    private static final int    DEFAULT_CTRL_PORT  = 6000;
    private static final String SYS_ROOM           = "__sys__"; // presence + admin

    // ==== UI ====
    private JTextField tfUser, tfRoom, tfServer;
    private JButton btnChoose, btnConnect, btnHistory;
    private DefaultListModel<String> lmUsers = new DefaultListModel<>();
    private JList<String> lstUsers;
    private JTextArea taChat;       
    private JTextField tfInput;     
    private JButton btnSend;
    private JLabel lbStatus;

    // ==== Network ====
    private MulticastSocket ms;
    private InetAddress groupAddr;
    private int mcastPort = DEFAULT_MCAST_PORT;
    private DatagramSocket ctrlSocket;
    private String serverHost = "127.0.0.1";
    private int    ctrlPort   = DEFAULT_CTRL_PORT;
    private Thread recvThread;
    private volatile boolean connected = false;

    // ==== Auth ====
    private String sessionId = null;
    private String role = "user";

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Set<String> onlineUsers = Collections.synchronizedSet(new HashSet<>());

    public ChatClientMain() {
        super("Multicast Chat – có đăng nhập + admin");
        applyLookAndFeel();
        buildUI();
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        tfUser.setText("user" + (int)(Math.random()*1000));
        tfRoom.setText("general");
        tfServer.setText("127.0.0.1");
    }

    private void buildUI() {
        setFontAll(this, new Font("Segoe UI", Font.PLAIN, 14));

        // --- thanh trên
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx=0; c.gridy=0; top.add(new JLabel("User:"), c);
        c.gridx=1; c.weightx=0.18; tfUser = new JTextField(10); top.add(tfUser, c);

        c.gridx=2; c.weightx=0; top.add(new JLabel("Room:"), c);
        c.gridx=3; c.weightx=0.18; tfRoom = new JTextField("general", 10); top.add(tfRoom, c);

        c.gridx=4; c.weightx=0; top.add(new JLabel("Server:"), c);
        c.gridx=5; c.weightx=0.32; tfServer = new JTextField(16); top.add(tfServer, c);

        c.gridx=6; c.weightx=0; btnChoose = new JButton("Chọn…");   top.add(btnChoose, c);
        c.gridx=7; btnConnect = new JButton("Kết nối");             top.add(btnConnect, c);
        c.gridx=8; btnHistory = new JButton("Lịch sử");             top.add(btnHistory, c);

        // --- trái: users
        lstUsers = new JList<>(lmUsers);
        JScrollPane spUsers = new JScrollPane(lstUsers);
        spUsers.setBorder(BorderFactory.createTitledBorder("Người online"));

        // --- phải: chat (gộp)
        taChat = new JTextArea();
        taChat.setEditable(false);
        taChat.setLineWrap(true);
        JScrollPane spChat = new JScrollPane(taChat);

        tfInput = new JTextField();
        btnSend = new JButton("Gửi");
        JPanel inputBar = new JPanel(new BorderLayout(6,6));
        inputBar.add(tfInput, BorderLayout.CENTER);
        inputBar.add(btnSend, BorderLayout.EAST);

        JPanel chatPanel = new JPanel(new BorderLayout(6,6));
        chatPanel.setBorder(BorderFactory.createTitledBorder("PHÒNG CHAT"));
        chatPanel.add(spChat, BorderLayout.CENTER);
        chatPanel.add(inputBar, BorderLayout.SOUTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spUsers, chatPanel);
        mainSplit.setResizeWeight(0.22);
        mainSplit.setBorder(null);

        lbStatus = new JLabel("Chưa kết nối");
        lbStatus.setBorder(new EmptyBorder(6,10,8,10));

        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(new EmptyBorder(8,8,0,8));
        root.add(top, BorderLayout.NORTH);
        root.add(mainSplit, BorderLayout.CENTER);

        setContentPane(new JPanel(new BorderLayout()));
        getContentPane().add(root, BorderLayout.CENTER);
        getContentPane().add(lbStatus, BorderLayout.SOUTH);

        btnConnect.addActionListener(e -> onConnect());
        btnSend.addActionListener(e -> onSend());
        tfInput.addActionListener(e -> onSend());
        btnHistory.addActionListener(e -> onHistory());
        btnChoose.addActionListener(e -> onChooseServer());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                try { sendPresence("LEAVE"); } catch (Exception ignored) {}
                safeClose();
            }
        });
    }

    // === CONNECT ===
    private void onConnect() {
        if (connected) {
            try { sendPresence("LEAVE"); } catch (Exception ignored) {}
            safeClose();
            return;
        }
        try {
            parseServerField();

            // === đăng nhập trước ===
            LoginDialog dlg = new LoginDialog(this, serverHost, ctrlPort);
            dlg.setVisible(true);
            LoginDialog.Result lr = dlg.getResult();
            if (lr == null) return;
            this.sessionId = lr.session;
            this.role = lr.role;
            tfUser.setText(lr.username);
            tfUser.setEditable(false);

            // REGISTER để server đánh dấu online
            ctrlSocket = new DatagramSocket();
            ctrlSocket.setSoTimeout(1200);
            sendCtrl("REGISTER|" + tfUser.getText().trim());

            // join multicast
            groupAddr = InetAddress.getByName(DEFAULT_GROUP);
            ms = new MulticastSocket(mcastPort);
            ms.setReuseAddress(true);
            ms.joinGroup(groupAddr);

            connected = true;
            btnConnect.setText("Ngắt kết nối");
            lbStatus.setText("Đã kết nối: " + DEFAULT_GROUP + ":" + mcastPort + " • Control " + serverHost + ":" + ctrlPort);

            recvThread = new Thread(this::recvLoop, "mcast-recv");
            recvThread.setDaemon(true);
            recvThread.start();

            lmUsers.clear(); onlineUsers.clear();
            sendPresence("JOIN");
            appendSystem("Bạn đã vào phòng " + tfRoom.getText().trim());
            tfInput.requestFocusInWindow();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            safeClose();
        }
    }

    private void safeClose() {
        connected = false;
        try { if (ms != null) { try { ms.leaveGroup(groupAddr); } catch (Exception ignored) {} ms.close(); } } catch (Exception ignored) {}
        try { if (ctrlSocket != null) ctrlSocket.close(); } catch (Exception ignored) {}
        btnConnect.setText("Kết nối");
        lbStatus.setText("Chưa kết nối");
    }

    // === SEND ===
    private void onSend() {
        if (!connected) { JOptionPane.showMessageDialog(this, "Bạn chưa kết nối!"); return; }
        String text = tfInput.getText().trim();
        if (text.isEmpty()) return;
        try {
            String room = tfRoom.getText().trim();
            String user = tfUser.getText().trim();
            long ts = nowEpoch();
            String payload = room + "|" + user + "|" + sanitize(text) + "|" + ts;
            byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            DatagramPacket out = new DatagramPacket(data, data.length, groupAddr, mcastPort);
            ms.send(out);
            tfInput.setText("");
            addOnlineUser(user);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gửi lỗi: " + ex.getMessage());
        }
    }

    // === RECEIVE LOOP ===
    private void recvLoop() {
        byte[] buf = new byte[4096];
        while (connected && ms != null && !ms.isClosed()) {
            try {
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                ms.receive(in);
                String s = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8);

                String[] p = s.split("\\|", 4);
                if (p.length >= 3) {
                    String room = p[0], user = p[1], text = p[2];
                    long ts = (p.length >= 4) ? safeLong(p[3], nowEpoch()) : nowEpoch();

                    if (SYS_ROOM.equals(room)) {
                        if (text.startsWith("ANNOUNCE:")) {
                            appendSystem("[THÔNG BÁO] " + text.substring(9));
                        } else if (text.startsWith("KICK|")) {
                            String victim = text.substring(5);
                            if (victim.equals(tfUser.getText().trim())) {
                                appendSystem("Bạn bị admin kick. Ứng dụng sẽ tự ngắt.");
                                SwingUtilities.invokeLater(this::safeClose);
                            } else {
                                appendSystem("Admin đã kick " + victim);
                            }
                        } else {
                            handlePresence(user, text);
                        }
                        continue;
                    }
                    if (room.equals(tfRoom.getText().trim())) {
                        addOnlineUser(user);
                        appendChat(user, text, ts);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    // === HISTORY ===
    private void onHistory() {
        if (!connected) { JOptionPane.showMessageDialog(this, "Hãy kết nối trước."); return; }
        try {
            String room = tfRoom.getText().trim();
            String cmd = "HISTORY|" + room + "|0|50";
            ctrlSocket.setSoTimeout(1500);
            sendCtrl(cmd);

            int appended=0;
            while (true) {
                DatagramPacket r = recvCtrl(2500);
                String s = new String(r.getData(), 0, r.getLength(), StandardCharsets.UTF_8);
                if (s.startsWith("HIST_END|")) break;
                if (s.startsWith("HIST|")) {
                    String[] a = s.split("\\|", 5);
                    if (a.length>=5 && a[1].equals(room)) {
                        appendChat(a[2], a[3], safeLong(a[4], nowEpoch()));
                        appended++;
                    }
                }
            }
            appendSystem("Đã nạp " + appended + " dòng lịch sử.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi history: " + ex.getMessage());
        }
    }

    // === DISCOVER ===
    private void onChooseServer() {
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setBroadcast(true); ds.setSoTimeout(800);
            byte[] q = ("DISCOVER|" + ThreadLocalRandom.current().nextInt()).getBytes(StandardCharsets.UTF_8);
            ds.send(new DatagramPacket(q, q.length, InetAddress.getByName("255.255.255.255"), DEFAULT_CTRL_PORT));
            Thread.sleep(120);
            List<String> items=new ArrayList<>();
            byte[] buf=new byte[1024]; long until=System.currentTimeMillis()+1200;
            while(System.currentTimeMillis()<until){
                try{ DatagramPacket r=new DatagramPacket(buf,buf.length); ds.receive(r);
                     String s=new String(r.getData(),0,r.getLength(),StandardCharsets.UTF_8);
                     if(s.startsWith("DISCOVER_OK|")) items.add(s); }
                catch(Exception ignored){}
            }
            if(items.isEmpty()){ JOptionPane.showMessageDialog(this,"Không tìm thấy server"); return; }
            String sel=(String)JOptionPane.showInputDialog(this,"Chọn server:","Server",JOptionPane.QUESTION_MESSAGE,null,items.toArray(),items.get(0));
            if(sel!=null){ tfServer.setText(sel.split("\\|")[1]); }
        }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Lỗi discover:"+ex.getMessage());}
    }

    // === Helpers ===
    private void sendPresence(String action) throws Exception {
        if (!connected||ms==null||ms.isClosed()) return;
        String user=tfUser.getText().trim();
        String payload=SYS_ROOM+"|"+user+"|"+action+"|"+nowEpoch();
        byte[] data=payload.getBytes(StandardCharsets.UTF_8);
        ms.send(new DatagramPacket(data,data.length,groupAddr,mcastPort));
    }
    private void handlePresence(String user,String action){
        if("JOIN".equalsIgnoreCase(action)){ addOnlineUser(user); appendSystem(user+" đã tham gia");}
        else if("LEAVE".equalsIgnoreCase(action)){ removeOnlineUser(user); appendSystem(user+" đã rời phòng");}
    }
    private void addOnlineUser(String user){ if(user==null||user.isEmpty())return; if(onlineUsers.add(user)) SwingUtilities.invokeLater(()->lmUsers.addElement(user)); }
    private void removeOnlineUser(String user){ if(user==null||user.isEmpty())return; if(onlineUsers.remove(user)) SwingUtilities.invokeLater(()->lmUsers.removeElement(user)); }

    private void sendCtrl(String s)throws Exception{ byte[]data=s.getBytes(StandardCharsets.UTF_8); ctrlSocket.send(new DatagramPacket(data,data.length,InetAddress.getByName(serverHost),ctrlPort)); }
    private DatagramPacket recvCtrl(int timeout)throws Exception{ byte[]buf=new byte[2048]; ctrlSocket.setSoTimeout(timeout); DatagramPacket r=new DatagramPacket(buf,buf.length); ctrlSocket.receive(r); return r; }
    private void parseServerField(){ String[]hp=tfServer.getText().trim().split(":"); serverHost=hp[0]; ctrlPort=(hp.length>=2)?safeInt(hp[1],DEFAULT_CTRL_PORT):DEFAULT_CTRL_PORT; }

    private void appendSystem(String s){ SwingUtilities.invokeLater(()->{ taChat.append("[SYS "+nowStr()+"] "+s+"\n"); taChat.setCaretPosition(taChat.getDocument().getLength());}); }
    private void appendChat(String u,String t,long ts){ SwingUtilities.invokeLater(()->{ taChat.append(u+" - "+fmt(ts)+": "+t+"\n"); taChat.setCaretPosition(taChat.getDocument().getLength());}); }

    private static String fmt(long epochSec){ return TS_FMT.format(LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSec),ZoneId.systemDefault())); }
    private static long nowEpoch(){ return System.currentTimeMillis()/1000; }
    private static String nowStr(){ return TS_FMT.format(LocalDateTime.now()); }
    private static long safeLong(String s,long d){ try{return Long.parseLong(s.trim());}catch(Exception e){return d;} }
    private static int safeInt(String s,int d){ try{return Integer.parseInt(s.trim());}catch(Exception e){return d;} }
    private static String sanitize(String s){ return s.replace('\n',' ').replace('\r',' ').replace('|','/'); }

    private static void applyLookAndFeel(){ try{ for(UIManager.LookAndFeelInfo info:UIManager.getInstalledLookAndFeels()){ if("Nimbus".equals(info.getName())){ UIManager.setLookAndFeel(info.getClassName()); break; } } }catch(Exception ignored){} }
    private static void setFontAll(Component comp, Font f){ comp.setFont(f); if(comp instanceof Container) for(Component c:((Container)comp).getComponents()) setFontAll(c,f); }

    public static void main(String[] args){ SwingUtilities.invokeLater(()->new ChatClientMain().setVisible(true)); }
}
