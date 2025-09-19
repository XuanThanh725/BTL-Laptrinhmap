package server;

public class ChatMessage {
    public final String room;
    public final String user;
    public final String text;
    public final long ts;

    public ChatMessage(String room, String user, String text, long ts) {
        this.room = room;
        this.user = user;
        this.text = text;
        this.ts   = ts;
    }
}
