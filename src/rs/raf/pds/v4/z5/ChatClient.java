package rs.raf.pds.v4.z5;

import java.io.BufferedReader;



import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import javafx.application.Platform;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.PrivateMessage;
import rs.raf.pds.v4.z5.messages.WhoRequest;

public class ChatClient implements Runnable{

	public static int DEFAULT_CLIENT_READ_BUFFER_SIZE = 1000000;
	public static int DEFAULT_CLIENT_WRITE_BUFFER_SIZE = 1000000;
	
	private volatile Thread thread = null;
	
	volatile boolean running = false;
	
	private String activeRoom = "MAIN-CHAT";
	final Client client;
	final String hostName;
	final int portNumber;
	final String userName;
	private final ChatMessages chatMessages;

	
	public ChatClient(String hostName, int portNumber, String userName, ChatMessages chatMessages) {
		this.client = new Client(DEFAULT_CLIENT_WRITE_BUFFER_SIZE, DEFAULT_CLIENT_READ_BUFFER_SIZE);
		
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.userName = userName;
		this.chatMessages = chatMessages;
	

		KryoUtil.registerKryoClasses(client.getKryo());
		registerListener();
	}
	
	
	private void registerListener() {
		client.addListener(new Listener() {
			public void connected (Connection connection) {
				Login loginMessage = new Login(userName);
				client.sendTCP(loginMessage);
			}
			
			public void received (Connection connection, Object object) {
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;
					showChatMessage(chatMessage);
					return;
				}
				 if (object instanceof PrivateMessage) {
	                    PrivateMessage privateMessage = (PrivateMessage) object;
	                    showPrivateChatMessage(privateMessage);
	                    return;
	                }

				if (object instanceof ListUsers) {
					ListUsers listUsers = (ListUsers) object;
                    if(!listUsers.getChecker()) {
                    	showOnlineUsers(listUsers.getUsers());
                    return;
                    }
                    else {
                    	showUsersInChatRoom(listUsers.getUsers());
                    }
				}
				if(object instanceof List) {
                	handleChatMessages((List<ChatMessage>) object);
                	
                }
				
				 if (object instanceof InfoMessage) {
	                    InfoMessage message = (InfoMessage) object;
	                    if(message.getTxt().startsWith("Active room set to: ")) {
	                    	String[] parts = message.getTxt().split(": ");
	                    	String room = parts[1].trim();
	                    	setActiveRoom(room);
	                       
	                    	return;
	                    }
	                    
	                    showMessage("Server: " + message.getTxt());
	                    return;
	                }
				
          
			}
		
		});
	}
	
	private void handleChatMessages(List<ChatMessage> chatMessagess) {
        if (chatMessages != null) {
        	ChatMessage old = chatMessagess.get(0);
        	ChatMessage newer = chatMessagess.get(1);
        	chatMessages.handleMessageUpdate(old, newer,this.activeRoom);
        }
    }
	
    
	private void showChatMessage(ChatMessage chatMessage) {
	     printToGUI("[" + activeRoom + "] " + chatMessage.getUser() + ": " + chatMessage.getTxt());
	}
	private void showPrivateChatMessage(PrivateMessage privateMessage) {
        printToGUI("Private message from " + privateMessage.getSender() + ": " + privateMessage.getContent());
    }
	
	private void showOnlineUsers(String[] users) {
	    StringJoiner joiner = new StringJoiner(", ");
	    for (String user : users) {
	        joiner.add(user);
	    }
	    String message = "Server: Active users: " + joiner.toString();
	    printToGUI(message);
	}

	
	 private void showMessage(String txt) {
	        printToGUI(txt);
	    }

	 private void printToGUI(String message) {
		 	Platform.runLater(() -> chatMessages.handleMessage(message));
	    }
	    
	 private void showUsersInChatRoom(String[] users) {
			Platform.runLater(() -> chatMessages.handleUserListUpdate(Arrays.asList(users),activeRoom));
	    }
 
	    public void handleUserListUpdate(List<String> users, String room) {
	        if (chatMessages != null) {
	            chatMessages.handleUserListUpdate(users, room);
	        }
	    }
	    
	    public void handleMessageUpdate(ChatMessage old,ChatMessage newer,String room) {
	        if (chatMessages != null) {
	            chatMessages.handleMessageUpdate(old, newer, room);
	        }
	    }
	    
	public void start() throws IOException {
		client.start();
		connect();
		
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
	
	public void connect() throws IOException {
		client.connect(1000, hostName, portNumber);
	}
	
	  public void setActiveRoom(String roomName) {
	        activeRoom = roomName;
	    }
	  
	  
	  
	  public void userInput(String userInput, String activeRoom) {
	        if (userInput == null || "BYE".equalsIgnoreCase(userInput)) {
	            running = false;
	        } else if ("WHO".equalsIgnoreCase(userInput)) {
	            client.sendTCP(new WhoRequest());
	        } else if (userInput.toUpperCase().startsWith("/PRIVATE")) {
	        	String[] parts = userInput.split(" ");
	        	String recipient = parts[1];
	        	
	        	StringJoiner sj = new StringJoiner(" ");
	        	for (int i = 2; i < parts.length; i++) {
	        		sj.add(parts[i]);
	        	}
	        	String messageText = sj.toString();
	            if (recipient == null || messageText == null) {
	                System.out.println("Use /PRIVATE <Recipient> <Message>");
	                return;
	            }
	            sendPrivateMessage(recipient, messageText);
	        } else if (userInput.toUpperCase().startsWith("/CREATE")) {
	            client.sendTCP(userInput.toUpperCase());
	        } else if (userInput.toUpperCase().startsWith("/LISTROOMS")) {
	            client.sendTCP(userInput.toUpperCase());
	        } else if (userInput.toUpperCase().startsWith("/JOIN")) {
	            client.sendTCP(userInput.toUpperCase());
	        }else if (userInput.toUpperCase().startsWith("/EDIT#")) {
	            client.sendTCP(userInput);
	        }
	        else if (userInput.toUpperCase().startsWith("/INVITE")) {
	            String[] parts = userInput.split(" ");
	            if (parts.length == 3) {
	                client.sendTCP(parts[0].toUpperCase() + " " + parts[1] + " " + parts[2].toUpperCase());
	            } else {
	                System.out.println(" Use /INVITE <User> <Room>");
	            }
	        } else if(userInput.toUpperCase().startsWith("/GETMOREMESSAGES")) {
	        	String[] parts = userInput.split(" ");
	        	String command = parts[0].toUpperCase();

	        	client.sendTCP(command.toUpperCase() + " "+ activeRoom + " " + "50");
	        }
	        else {
	            ChatMessage message = new ChatMessage(userName, userInput, activeRoom);
	            client.sendTCP(message);
	        }

	        if (!client.isConnected() && running) {
	            try {
	                connect();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	public void run() {
		
		try (
				BufferedReader stdIn = new BufferedReader(
	                    new InputStreamReader(System.in))	
	        ) {
					            
				String userInput;
				running = true;
				
	            while (running) {
	            	userInput = stdIn.readLine();
	            	if (userInput == null || "BYE".equalsIgnoreCase(userInput)) 
	            	{
	            		running = false;
	            	}
	            	else if ("WHO".equalsIgnoreCase(userInput)){
	            		client.sendTCP(new WhoRequest());
	            	}	 else if ("PRIVATE".equalsIgnoreCase(userInput)) {
	                    System.out.print("Enter recipient: ");
	                    String recipient = stdIn.readLine();
	                    System.out.print("Enter message: ");
	                    String messageText = stdIn.readLine();
	                    sendPrivateMessage(recipient, messageText);
	            	}  else if (userInput.toUpperCase().startsWith("/CREATE")) {
	                    client.sendTCP(userInput.toUpperCase());
	                } else if (userInput.toUpperCase().startsWith("/LISTROOMS")) {
	                    client.sendTCP(userInput.toUpperCase());
	                } else if (userInput.toUpperCase().startsWith("/JOIN")) {
	                    client.sendTCP(userInput.toUpperCase());
	                } else if (userInput.toUpperCase().startsWith("/INVITE")) {
	                    String[] parts = userInput.split(" ");
	                    if (parts.length == 3) {
	                        client.sendTCP(parts[0].toUpperCase() + " " + parts[1] + " " + parts[2].toUpperCase());
	                    } else {
	                        System.out.println("Use /INVITE <User> <Room>");
	                    }
	                }
	            	else {
	            		ChatMessage message = new ChatMessage(userName, userInput, activeRoom);
	            		client.sendTCP(message);
	            	}
	            	
	            	if (!client.isConnected() && running)
	            		connect();
	            	
	           }
	            
	    } catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			running = false;
			System.out.println("CLIENT DISCONNECT");
			client.close();;
		}
	}
	
	public String getUserName() {
	    	return userName;
	    }
	 public String getActiveRoom() {
	    	return activeRoom;
	    }
	

    public void sendPrivateMessage(String recipient, String messageText) {
        PrivateMessage privateMessage = new PrivateMessage(userName, messageText, recipient);
        client.sendTCP(privateMessage);
    }

	/*public static void main(String[] args) {
		if (args.length != 3) {
		
            System.err.println(
                "Usage: java -jar chatClient.jar <host name> <port number> <username>");
            System.out.println("Recommended port number is 54555");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String userName = args[2];
        
        try{
        	ChatClient chatClient = new ChatClient(hostName, portNumber, userName);
        	chatClient.start();
        }catch(IOException e) {
        	e.printStackTrace();
        	System.err.println("Error:"+e.getMessage());
        	System.exit(-1);
        }
	}*/
}
