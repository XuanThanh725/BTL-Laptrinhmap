package server;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ControlServer {
    private final int port; private final String group; private final int mcastPort;
    private final MessageStore store; private final DbUserStore userStore;
    private final Map<String,String> sessions=new ConcurrentHashMap<>();
    private final Map<String,String> roles=new ConcurrentHashMap<>();

    public ControlServer(int port,String group,int mcastPort,MessageStore store)throws Exception{
        this.port=port; this.group=group; this.mcastPort=mcastPort; this.store=store;
        String sqliteUrl=((DbMessageStore)store).url;
        this.userStore=new DbUserStore(sqliteUrl);
    }

    public void start(){
        Thread t=new Thread(()->{
            try(DatagramSocket ds=new DatagramSocket(port)){
                byte[] buf=new byte[4096];
                while(true){
                    DatagramPacket in=new DatagramPacket(buf,buf.length); ds.receive(in);
                    String s=new String(in.getData(),0,in.getLength(),StandardCharsets.UTF_8);
                    InetAddress addr=in.getAddress(); int sport=in.getPort();
                    String[] p=s.split("\\|"); String cmd=p[0];
                    switch(cmd){
                        case "REGISTER"->send(ds,"OK",addr,sport);
                        case "HISTORY"->{
                            var list=store.history(p[1],0,50);
                            for(ChatMessage m:list) send(ds,"HIST|"+m.room+"|"+m.user+"|"+m.text+"|"+m.ts,addr,sport);
                            send(ds,"HIST_END",addr,sport);
                        }
                        case "DISCOVER"->{
                            String host=InetAddress.getLocalHost().getHostAddress();
                            send(ds,"DISCOVER_OK|"+host+"|"+port+"|"+group+"|"+mcastPort,addr,sport);
                        }
                        case "SIGNUP"->{
                            boolean ok=userStore.createUser(p[1],p[2],"user");
                            send(ds,ok?"OK":"ERR|EXISTS",addr,sport);
                        }
                        case "LOGIN"->{
                            var role=userStore.verifyLogin(p[1],p[2]);
                            if(role.isPresent()){
                                String sid=UUID.randomUUID().toString();
                                sessions.put(sid,p[1]); roles.put(p[1],role.get());
                                send(ds,"LOGIN_OK|"+sid+"|"+role.get(),addr,sport);
                            } else send(ds,"ERR|LOGIN",addr,sport);
                        }
                        case "ADMIN_ANNOUNCE"->{
                            if(isAdmin(p[1])) multicast("__sys__|server|ANNOUNCE:"+p[2]+"|"+Instant.now().getEpochSecond());
                        }
                        case "ADMIN_KICK"->{
                            if(isAdmin(p[1])) multicast("__sys__|server|KICK|"+p[2]+"|"+Instant.now().getEpochSecond());
                        }
                        case "LIST_USERS"->{
                            if(isAdmin(p[1])){
                                for(var e:roles.entrySet()) send(ds,"USER|"+e.getKey()+"|"+e.getValue()+"|1",addr,sport);
                                send(ds,"USERS_END",addr,sport);
                            }
                        }
                    }
                }
            }catch(Exception e){e.printStackTrace();}
        });
        t.setDaemon(true); t.start();
    }

    private boolean isAdmin(String sid){ String u=sessions.get(sid); return u!=null && "admin".equalsIgnoreCase(roles.get(u)); }
    private void send(DatagramSocket ds,String s,InetAddress a,int p)throws Exception{ ds.send(new DatagramPacket(s.getBytes(),s.length(),a,p)); }
    private void multicast(String s)throws Exception{ try(MulticastSocket ms=new MulticastSocket()){ ms.send(new DatagramPacket(s.getBytes(),s.length(),InetAddress.getByName(group),mcastPort)); } }
}
