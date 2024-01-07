package rs.raf.pds.v4.z5;

import java.util.Arrays;

import java.util.List;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import rs.raf.pds.v4.z5.messages.ChatMessage;

public class ChatApp extends Application implements ChatMessages {
	private ChatClient chatClient;
    private String lastSelectedMessage = null;
    private String lastSelectedUser = null;
    private ListView<String> messageListView;
    private TextField inputField;
    private String activeRoom = "MAIN-CHAT";
    private ListView<String> userListView;
   

    
    private static final String SCENE_BACKGROUND_COLOR = "-fx-background-color: #87CEEB";
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
    	 primaryStage.setTitle("Chat-App");

         VBox loginPane = new VBox(10);
         loginPane.setPadding(new Insets(20));
         loginPane.setAlignment(Pos.CENTER);

        
         Image logoImage = new Image("/rs/raf/pds/v4/z5/chat.png");  
         ImageView imageView = new ImageView(logoImage);
         imageView.setFitHeight(150);  
         imageView.setPreserveRatio(true);

         Label usernameLabel = new Label("Name:");
         TextField usernameField = new TextField();
         Button loginButton = new Button("Login");
         loginButton.setStyle(activeRoom);
         loginButton.setStyle("-fx-background-color: #1e81b0; -fx-text-fill: white; -fx-font-size: 14px;");

         loginButton.setOnAction(e -> login(usernameField.getText(), primaryStage));

         loginPane.getChildren().addAll(imageView, usernameLabel, usernameField, loginButton);

         Scene loginScene = new Scene(loginPane, 350, 350);

         primaryStage.setScene(loginScene);
         primaryStage.show();
    }

    private void login(String username, Stage primaryStage) {
        if (username.trim().isEmpty()) {
            showErrorDialog("Enter a valid name.");
            return;
        }
        chatClient = new ChatClient("localhost", 4555, username, this);

        try {
            chatClient.start();
            primaryStage.close();
            launchChatApp(primaryStage);
        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorDialog("Error connecting to the server.");
        }
    }

    private void launchChatApp(Stage primaryStage) {
    	BorderPane mainBorderPane = new BorderPane();
    	mainBorderPane.setPadding(new Insets(10));
    	mainBorderPane.setStyle(SCENE_BACKGROUND_COLOR);
    

    	messageListView = new ListView<>();
    	messageListView.setCellFactory(param -> new MessageCell());
    	
    	messageListView.setPrefSize(500, 400);
    	
    	
       

      
    	messageListView.setOnMouseClicked(event -> {
    	    String selectedMessage = messageListView.getSelectionModel().getSelectedItem();
    	    userListView.getSelectionModel().clearSelection();
    	 
    	    lastSelectedUser = null;
    	    if (selectedMessage != null) {
    	        if(selectedMessage.startsWith("Server:")) {
    	            messageListView.getSelectionModel().clearSelection();
    	            lastSelectedMessage = null;
    	            return;
    	        } else if (selectedMessage.equals(lastSelectedMessage)) {
    	            messageListView.getSelectionModel().clearSelection();
    	            lastSelectedMessage = null;
    	        } else {
    	            lastSelectedMessage = selectedMessage;
    	        }
    	    }
    	});

    	inputField = new TextField();
    	inputField.setPrefWidth(400); 
    	inputField.setPrefHeight(20); 
    	
    

    	userListView = new ListView<>();
    	userListView.setPrefHeight(600);
    	userListView.setMaxWidth(240);

    	

    	
    
   
    	VBox userListVBox = new VBox(userListView);
    	
    	userListVBox.setAlignment(Pos.CENTER);
    	userListView.setCellFactory(param -> new UserListCell());

    	Button enterButton = new Button("Enter");
    	enterButton.setPrefWidth(220); 
    	enterButton.setPrefHeight(20);
    	enterButton.setAlignment(Pos.CENTER);
    	enterButton.setStyle("-fx-background-color: #154c79; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

    	enterButton.setOnMouseEntered(e -> {
    	    enterButton.setStyle("-fx-background-color: #131b62; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
    	   
    	});

    	enterButton.setOnMouseExited(e -> {
    	    enterButton.setStyle("-fx-background-color: #154c79; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
    	    
    	});
    	
    	enterButton.setOnAction(e -> processUserInput());

    	HBox inputBox = new HBox(10);
    	inputBox.getChildren().addAll(inputField, new Region(), enterButton);
    	
    	inputBox.setPadding(new Insets(10, 0, 0, 260)); 
    
    	mainBorderPane.setLeft(userListVBox);
    	mainBorderPane.setCenter(messageListView);
    	mainBorderPane.setBottom(inputBox);

    	
    	BorderPane.setMargin(userListVBox, new Insets(0, 20, 0, 0)); 
    	
    	
    	Scene chatScene = new Scene(mainBorderPane, 800, 700);

    	primaryStage.setScene(chatScene);
    	primaryStage.setTitle("Chat-App - " + chatClient.getUserName());
    	primaryStage.show();

    }

    @Override
    public void handleMessage(String message) {
        Platform.runLater(() -> {
            messageListView.getItems().add(message);

            if (message.startsWith("Your chat rooms:")) {
                List<String> rooms = Arrays.asList(message.split(":")[1].trim().split("\n"));
            }
        });
    }

    private class MessageCell extends ListCell<String> {
    	private final Button editButton;
    	
        public MessageCell() {
            editButton = new Button("Edit");
            
            editButton.setStyle("-fx-background-color: #154c79; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5px;");

            editButton.setOnMouseEntered(e -> {
            	editButton.setStyle("-fx-background-color: #131b62; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5px;");
        	   
        	});

            editButton.setOnMouseExited(e -> {
            	editButton.setStyle("-fx-background-color: #154c79; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5px;");
        	    
        	});
         

        }
    	
    	
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null); 
            } else {
                Text messageText = new Text(item);
               // setColorForMessage(messageText);
                messageText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                String[] parts = item.split(":")[0].split("\\)", 2);
                String messageUsername = (parts.length > 1) ? parts[1].trim() : "";
                boolean isClientMessage = messageUsername.equalsIgnoreCase(chatClient.getUserName());
                if (isClientMessage) {
                	Region spacer = new Region();
                	HBox messageBox = new HBox(messageText, spacer, editButton);
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    setStyle("-fx-background-color: #d6d6cd; -fx-text-fill: white;");
                    
                    messageBox.setOnMouseClicked(e -> {
                    	setStyle("-fx-background-color: #a7a7a0; -fx-text-fill: white;");
                	   
                	});
                    setGraphic(messageBox);
                } else {
                	
                    setGraphic(new HBox(messageText));
                }
            }
        }
        
     /*  private void setColorForMessage(Text text) {
        	text.setFill(javafx.scene.paint.Color.web("#10435b"));
        	
       }*/

     /*   private void setColorForMessageClient(Text text) {
        	text.setFill(javafx.scene.paint.Color.web("#10435b"));
        }*/
      
     
    
        }
       
    
    
    public class UserListCell extends ListCell<String> {

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
            } else {
                setText("-> " + item);
                setStyle(getUserStyle());

                setOnMouseEntered(event -> setHoverStyle());
                setOnMouseExited(event -> setExitStyle());
            }
        }

        private String getUserStyle() {
            return "-fx-background-color: #e1ecf4; " +
                   "-fx-text-fill: #10435b; " +
                   "-fx-padding: 5 10; " +
                   "-fx-font-size: 14px; " +
                   "-fx-border-width: 1; " +
                   "-fx-border-color: #c4d7ed;";
        }

        private void setHoverStyle() {
            setStyle("-fx-background-color: #92cbdf; " +
                     "-fx-text-fill: white; " +
                     "-fx-padding: 5 10; " +
                     "-fx-font-size: 14px; " +
                     "-fx-border-width: 1; " +
                     "-fx-border-color: #c4d7ed;");
        }

        private void setExitStyle() {
            setStyle(getUserStyle());
        }
    }


    @Override
    public void handleUserListUpdate(List<String> users, String room) {
        Platform.runLater(() -> {
            if (userListView.getItems().isEmpty()) {
                userListView.getItems().addAll(users);
            } else {
                userListView.getItems().clear();
                userListView.getItems().addAll(users);
            }

            activeRoom = room;
            
        });
    }

    private void processUserInput() {
    	
        String userInput = inputField.getText().trim();
        if (!userInput.isEmpty()) {
        	if(lastSelectedMessage == null) {
        		if(lastSelectedUser != null) {
        			chatClient.sendPrivateMessage(lastSelectedUser, userInput);
        		}
        		else {
        		chatClient.processUserInput(userInput, activeRoom);
        		}
        	}
        	else {
        		if(lastSelectedMessage.startsWith("Private message from")) {
        			String[] parts = lastSelectedMessage.split(":",2)[0].split(" ");
        			String from = parts[parts.length-1];
        			
        			chatClient.sendPrivateMessage(from, "replied to message:\n("+lastSelectedMessage+")\n"+userInput);
        		}
        		else {
        		chatClient.processUserInput("replied to message:\n("+lastSelectedMessage+")\n"+userInput, activeRoom);
        		}
        		}
        	messageListView.getSelectionModel().clearSelection();
            lastSelectedMessage = null;
        }
        inputField.clear();
    }
    
    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public void handleMessageUpdate(ChatMessage oldMessage, ChatMessage message, String room) {
        Platform.runLater(() -> {
        	
        	String messageText = message.format().trim();
        	if(message.getReply()) {
            	handleReplyUpdate(oldMessage, messageText);
            	return;
            }
            else {
            	for (int i = 0; i < messageListView.getItems().size(); i++) {
                    String existingMessage = messageListView.getItems().get(i);
                    	if(existingMessage.trim().equalsIgnoreCase(oldMessage.format().trim())) {
                        messageListView.getItems().set(i, messageText);
                        
                    	}
                    	else if(existingMessage.trim().contains(oldMessage.format().trim())) {
                    		
                    		messageListView.getItems().set(i, replaceBetweenMarkers(existingMessage.trim(), "(("+chatClient.getActiveRoom()+") "+message.getUser()+": ", ")", message.getTxt()));
                    	}
                    }
            }
            
        });
    }
    
    
    private String replaceBetweenMarkers(String input, String startMarker, String endMarker, String replacement) {
        String escapedStartMarker = Pattern.quote(startMarker);
        String escapedEndMarker = Pattern.quote(endMarker);

        String[] parts = input.split(escapedStartMarker, 2);
        if (parts.length > 1) {
            String[] secondParts = parts[1].split(escapedEndMarker, 2);
            if (secondParts.length > 1) {
                return parts[0] + startMarker + replacement + endMarker + secondParts[1];
            }
        }

        return input;
    }

    private void handleReplyUpdate(ChatMessage oldMessage, String updatedMessageText) {
        Platform.runLater(() -> {
            ObservableList<String> items = messageListView.getItems();
            for (int i = 0; i < items.size(); i++) {
                String existingMessage = items.get(i);
                if (existingMessage.trim().equalsIgnoreCase(oldMessage.format().trim())) {
                    String pattern = ")\n";
                    int lastIndex = oldMessage.format().lastIndexOf(pattern);
                    if (lastIndex != -1) {
                        String editedMsg = updatedMessageText;
                        String updatedFormat = oldMessage.format().substring(0, lastIndex + 4) + updatedMessageText;
                        items.set(i, updatedFormat);
                    } else {
                        items.set(i, updatedMessageText);
                    }
                    break;
                }
            }
        });
    }


  
}