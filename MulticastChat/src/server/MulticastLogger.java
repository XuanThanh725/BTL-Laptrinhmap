package server;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class MulticastLogger {
    private final String group; private final int mcastPort; private final MessageStore store;

    public MulticastLogger(String group,int mcastPort,MessageStore store){
        this.group=group; this.mcastPort=mcastPort; this.store=store;
    }

    public void start(){
        Thread t=new Thread(()->{
            try(MulticastSocket ms=new MulticastSocket(mcastPort)){
                InetAddress g=InetAddress.getByName(group);
                ms.joinGroup(g);
                byte[] buf=new byte[4096];
                while(true){
                    DatagramPacket in=new DatagramPacket(buf,buf.length);
                    ms.receive(in);
                    String s=new String(in.getData(),0,in.getLength(),StandardCharsets.UTF_8);
                    String[] p=s.split("\\|",4);
                    String room=p[0],user=p[1],text=p[2];
                    long ts=(p.length>=4)?Long.parseLong(p[3]):System.currentTimeMillis()/1000;
                    store.append(new ChatMessage(room,user,text,ts));
                }
            }catch(Exception e){ e.printStackTrace(); }
        },"mcast-logger");
        t.setDaemon(true); t.start();
    }
}
