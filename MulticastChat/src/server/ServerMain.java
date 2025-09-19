package server;

import java.nio.file.Paths;

public class ServerMain {
    public static void main(String[] args)throws Exception{
        MessageStore store=new DbMessageStore(Paths.get("data"));
        new MulticastLogger("239.1.1.1",5000,store).start();
        new ControlServer(6000,"239.1.1.1",5000,store).start();
        System.out.println("Server started");
        Thread.currentThread().join();
    }
}
