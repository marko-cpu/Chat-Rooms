package rs.raf.pds.v4.z5;

import java.util.List;

import rs.raf.pds.v4.z5.messages.ChatMessage;

public interface ChatMessages {
    void handleMessage(String message);
    void handleUserListUpdate(List<String> users, String room);
    void handleMessageUpdate(ChatMessage old,ChatMessage newr,String room);
}
