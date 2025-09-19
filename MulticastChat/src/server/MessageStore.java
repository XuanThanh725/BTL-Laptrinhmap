package server;

import java.util.List;

public interface MessageStore {
    void append(ChatMessage m) throws Exception;
    List<ChatMessage> history(String room, long sinceTs, int limit) throws Exception;
}
