package rs.raf.pds.v4.z5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.PrivateMessage;
import rs.raf.pds.v4.z5.messages.WhoRequest;


public class ChatServer implements Runnable{

	private volatile Thread thread = null;
	
	volatile boolean running = false;
	final Server server;
	final int portNumber;
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();
	private final List<PrivateMessage> privateMessagesList = new CopyOnWriteArrayList<>();
	private final ConcurrentMap<String, List<Connection>> chatRooms = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, List<ChatMessage>> chatRoomsMessages = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> userActiveRoomsMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, List<String>> welcomedUsersMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<Connection, String> connectionActiveRoomMap = new ConcurrentHashMap<>();

	
	public ChatServer(int portNumber) {
		this.server = new Server();
		
		this.portNumber = portNumber;
		KryoUtil.registerKryoClasses(server.getKryo());
		registerListener();
	}
	
	
	private void registerListener() {
		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Login) {
					 Login login = (Login) object;
	                 newUserLogged(login, connection);
	                 updateUserChatRooms(connection);
	                 return;
				}
				
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage) object;
                    System.out.println(chatMessage.getUser() + ": " + chatMessage.getTxt());
                    connection.sendTCP(chatMessage);
                    broadcastChatMessage(chatMessage, connection);
                    addMessageToChatRoom(chatMessage);
                    return;
				}

				if (object instanceof WhoRequest) {
					ListUsers listUsers = new ListUsers(getAllUsers());
					connection.sendTCP(listUsers);
					updateUserChatRooms(connection);
					return;
				}
				 if (object instanceof PrivateMessage) {
	                    PrivateMessage privateMessage = (PrivateMessage) object;
	                    String recipient = privateMessage.getRecipient();
	                    Connection recipientConnection = userConnectionMap.get(recipient);
	                    if (recipientConnection != null && recipientConnection.isConnected()) {
	                        recipientConnection.sendTCP(privateMessage);
	                        System.out.println("Private message from " + privateMessage.getSender() + " to " + recipient +": " + privateMessage.getContent());
	                        connection.sendTCP(new InfoMessage("Private message sent to " + recipient));
	                        privateMessagesList.add(privateMessage);
	                    } else {
	                        connection.sendTCP(new InfoMessage("User " + recipient + " is not online."));
	                    }
	                    return;
	                }
				 if (object instanceof InfoMessage) {
	                    InfoMessage message = (InfoMessage) object;
	                    showTextToAll("Server: " + message.getTxt(), connection);
	                    return;
	                }

	                if (object instanceof String) {
	                    String command = (String) object;
	                    String[] parts = command.split(" ");
	                    String action = parts[0].toUpperCase();

	                    switch (action) {
	                        case "/CREATE":
	                            if (parts.length >= 2) {
	                                String roomName = parts[1];
	                                createChatRoom(roomName, connection);
	                            }
	                            break;
	                            
	                        case "/JOIN":
	                            if (parts.length >= 2) {
	                                String roomName = parts[1];
	                                joinChatRoom(roomName, connection);
	                            }
	                            break;
	                        case "/LISTROOMS":
	                            listChatRooms(connection);
	                            break;
	                       
	                        case "/ROOM":
	                            if (parts.length >= 2) {
	                                String roomName = parts[1];
	                                String userName = connectionUserMap.get(connection);
	                                
	                                if (userName != null) {
	                                    setActiveRoom(connection, roomName);
	                                } else {
	                                    connection.sendTCP(new InfoMessage("You haven't joined yet."));
	                                }
	                            }
	                            break;

	                        case "/GETMOREMESSAGES":
	                            if (parts.length >= 2) {
	                                String roomName = parts[1];
	                                int num =Integer.parseInt(parts[2]);
	                                getMoreMessages(roomName, connection, num);
	                            }
	                            break;

	                        case "/INVITE":
	                            if (parts.length >= 3) {
	                                String invitedUser = parts[1];
	                                String roomName = parts[2];
	                                inviteUserToRoom(invitedUser, roomName, connection);
	                            }
	                            break;
	                            
	                   
	                    }
	                }
	            
			}
			
			private void addMessageToChatRoom(ChatMessage chatMessage) {
		        String roomName = chatMessage.getChatRoom();
		        chatRoomsMessages.computeIfAbsent(roomName, k -> new ArrayList<>()).add(chatMessage);
		    }
			private void getMoreMessages(String roomName, Connection connection, int num) {
			    if (num < 0) {
			        throw new IllegalArgumentException("Number of messages cannot be less than zero.");
			    }

			    List<ChatMessage> messages = getChatRoomMessages(roomName);
			    if (messages != null) {
			        int maxMessages = num;
			        int startIndex = Math.max(0, messages.size() - maxMessages);
			        List<ChatMessage> lastMessages = messages.subList(startIndex, messages.size());

			        for (ChatMessage message : lastMessages) {
			            connection.sendTCP(message);
			        }
			    }
			}
			

			private void setActiveRoom(Connection connection, String roomName) {
			    String userName = connectionUserMap.get(connection);

			    if (userName != null) {
			        String previousActiveRoom = userActiveRoomsMap.getOrDefault(userName, "MAIN-CHAT");

			        if (chatRooms.containsKey(roomName) && chatRooms.get(roomName).contains(connection)) {
			            userActiveRoomsMap.put(userName, roomName);
			            connectionActiveRoomMap.put(connection, roomName);
			            updateUserChatRooms(connection);

			            List<String> usersInActiveRoom = getUsersInActiveRoom(roomName);
			            connection.sendTCP(new ListUsers(usersInActiveRoom.toArray(new String[0])));

			            if (!roomName.equals(previousActiveRoom)) {
			                connection.sendTCP(new InfoMessage("Welcome to the room: " + roomName));
			                broadcastUserListUpdate(roomName);
			            }

			            connection.sendTCP(new InfoMessage("Active room set to: " + roomName));
			            broadcastUserListUpdate(previousActiveRoom);
			            broadcastUserListUpdate(roomName);
			        } else {
			            connection.sendTCP(new InfoMessage("You are not a member of the room '" + roomName + "'. Use /JOIN " + roomName + " to join."));
			        }
			    } else {
			        connection.sendTCP(new InfoMessage("Failed to set active room. User not found."));
			    }
			}

	            
	         
	            
	            private List<ChatMessage> getChatRoomMessages(String chatRoom) {
	                return chatRoomsMessages.get(chatRoom);
	            }

	            private void inviteUserToRoom(String invitedUser, String roomName, Connection inviterConnection) {
	                Connection invitedConnection = userConnectionMap.get(invitedUser);

	                if (invitedConnection != null && invitedConnection.isConnected()) {
	                    invitedConnection.sendTCP(new InfoMessage("User " + connectionUserMap.get(inviterConnection) +
	                            " invited you to room '" + roomName + "'. [Type JOIN " + roomName + "]" + " to join, and [/ROOM "+ roomName + "]" +" to set active room."));
	                    inviterConnection.sendTCP(new InfoMessage("Invitation sent to " + invitedUser + " for room '" + roomName + "'."));
	                } else {
	                    inviterConnection.sendTCP(new InfoMessage("User " + invitedUser + " is not online."));
	                }
	            }

	            private void createChatRoom(String roomName, Connection connection) {
	                chatRooms.putIfAbsent(roomName, new CopyOnWriteArrayList<>());
	                connection.sendTCP(new InfoMessage("Chat room '" + roomName + "' created."));
	            }

	            private void listChatRooms(Connection connection) {
	                connection.sendTCP(new InfoMessage("Available chat rooms: " + chatRooms.keySet()));
	            }

	            private void joinChatRoom(String roomName, Connection connection) {
	                if (chatRooms.containsKey(roomName)) {
	                    List<Connection> userConnections = chatRooms.get(roomName);
	                    userConnections.add(connection);
	                    connection.sendTCP(new InfoMessage("Joined chat room '" + roomName + "'."));
	                    updateUserChatRooms(connection);
	                } else {
	                    connection.sendTCP(new InfoMessage("Chat room '" + roomName + "' does not exist."));
	                }
	            }

	            private void updateUserChatRooms(Connection connection) {
	                String userName = connectionUserMap.get(connection);

	                if (userName != null) {
	                    String activeRoom = userActiveRoomsMap.getOrDefault(userName, null);
	                    if (activeRoom != null) {
	                        connection.sendTCP(new InfoMessage("Active room set to: " + activeRoom));
	                    }
	                }

	                List<String> userRooms = chatRooms.entrySet().stream()
	                        .filter(entry -> entry.getValue().contains(connection))
	                        .map(Map.Entry::getKey)
	                        .collect(Collectors.toList());
	            }
			
			public void disconnected(Connection connection) {
				String user = connectionUserMap.get(connection);
				connectionUserMap.remove(connection);
				userConnectionMap.remove(user);
				showTextToAll(user+" has disconnected!", connection);
			}
		});
	}
	
	private void broadcastUserListUpdate(String roomName) {
        List<Connection> connections = chatRooms.get(roomName);
        List<String> usersInActiveRoom = getUsersInActiveRoom(roomName);
        for (String username : usersInActiveRoom) {
        }
        for (String username : usersInActiveRoom) {
        	Connection connection = userConnectionMap.get(username);
            if (connection.isConnected()) {
            	ListUsers lsu = new ListUsers(usersInActiveRoom.toArray(new String[0]));
            	lsu.setChecker(true);
                connection.sendTCP(lsu);
            }
            
        }
    }
    
    
    private List<String> getUsersInActiveRoom(String roomName) {
        List<String> usersInActiveRoom = userActiveRoomsMap.entrySet().stream()
                .filter(entry -> roomName.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return usersInActiveRoom;
    }
	  
    String[] getAllUsers() {
    	String[] usersWithRooms = userConnectionMap.keySet().stream()
                .map(user -> user + " [" + userActiveRoomsMap.get(user) + "] ")
                .toArray(String[]::new);
        return usersWithRooms;
    }
    
 
    

    private void newUserLogged(Login loginMessage, Connection conn) {
        String userName = loginMessage.getUserName();

        Connection connection = userConnectionMap.get(userName);

        if (connection != null) {
           
            if (connection.isConnected()) {
                connection.close();
            }
         
            chatRooms.forEach((roomName, connections) -> {
                int index = connections.indexOf(connection);
                if (index != -1) {
                    connections.set(index, conn);
                }
            });
           
            String activeRoom = userActiveRoomsMap.getOrDefault(userName, "MAIN-CHAT");
            userActiveRoomsMap.put(userName, activeRoom);
        } else {
           
            chatRooms.computeIfAbsent("MAIN-CHAT", k -> new CopyOnWriteArrayList<>()).add(conn);
        }

       
        userConnectionMap.put(userName, conn);
        connectionUserMap.put(conn, userName);

        String activeRoom = userActiveRoomsMap.computeIfAbsent(userName, k -> "MAIN-CHAT");

       
        if (welcomedUsersMap.computeIfAbsent(activeRoom, k -> new ArrayList<>()).contains(userName)) {
            
            System.out.println("User " + userName + " has connected again!");
        } else {
            
            conn.sendTCP(new InfoMessage("Welcome to the room: " + activeRoom));
         
            welcomedUsersMap.computeIfAbsent(activeRoom, k -> new ArrayList<>()).add(userName);
        }

       
        showTextToAll("User " + userName + " has connected!", conn);
    
        broadcastUserListUpdate(activeRoom);
    }

    private void broadcastChatRoomMessage(String roomName, ChatMessage message, Connection exception) {
        for (Map.Entry<String, String> entry : userActiveRoomsMap.entrySet()) {
            String userName = entry.getKey();
            String activeRoom = entry.getValue();

            if (roomName.equals(activeRoom)) {
                Connection conn = userConnectionMap.get(userName);
                if (conn != null && conn.isConnected() && conn != exception) {
                    conn.sendTCP(message);
                }
            }
        }
    }

    private void broadcastChatMessage(ChatMessage message, Connection exception) {
        String activeRoom = userActiveRoomsMap.getOrDefault(connectionUserMap.get(exception), null);
        
        for (Map.Entry<String, String> entry : userActiveRoomsMap.entrySet()) {
            System.out.println("User: " + entry.getKey() + ", Active Room: " + entry.getValue());
        }
        if (activeRoom != null) {
            broadcastChatRoomMessage(activeRoom, message, exception);
        } else {
            for (Connection conn : userConnectionMap.values()) {
                if (conn.isConnected() && conn != exception) {
                    conn.sendTCP(message);
                }
            }
        }
    }

    private void showTextToAll(String txt, Connection exception) {
        System.out.println(txt);
        for (Connection conn : userConnectionMap.values()) {
            if (conn.isConnected() && conn != exception)
                conn.sendTCP(new InfoMessage(txt));
        }
    }

    
    
    
  

    public void start() throws IOException {
        server.start();
        server.bind(portNumber);

        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        Thread stopThread = thread;
        thread = null;
        running = false;
        if (stopThread != null)
            stopThread.interrupt();
    }

    @Override
    public void run() {
        running = true;

        while (running) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    

	
	
	public static void main(String[] args) {
		
		if (args.length != 1) {
	        System.err.println("Usage: java -jar chatServer.jar <port number>");
	        System.out.println("Recommended port number is 54555");
	        System.exit(1);
	   }
	    
	   int portNumber = Integer.parseInt(args[0]);
	   try { 
		   ChatServer chatServer = new ChatServer(portNumber);
	   	   chatServer.start();
	   
			chatServer.thread.join();
	   } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	   } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	   }
	}
}