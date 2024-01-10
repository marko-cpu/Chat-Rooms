package rs.raf.pds.v4.z5;

import java.util.Arrays;



import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import javafx.scene.layout.StackPane;
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
    	

        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
               userInput();
            }
        });
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
    	
    	enterButton.setOnAction(e -> userInput());

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
            
            editButton.setStyle("-fx-background-color: #154c79; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 5px;");

            editButton.setOnMouseEntered(e -> {
            	editButton.setStyle("-fx-background-color: #131b62; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 5px;");
        	   
        	});

            editButton.setOnMouseExited(e -> {
            	editButton.setStyle("-fx-background-color: #154c79; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 5px;");
        	    
        	});
         
            editButton.setOnAction(event -> handleEditButtonClick());
        }
    	
    	
        public void handleEditButtonClick() {
        	  System.out.println("Edit button clicked");
        	  
        	  String selectedMessage = getItem();
        	 
              String userInput = inputField.getText().trim();
        	  if (!userInput.isEmpty()) {
                  chatClient.userInput("/EDIT# "+userInput+"#"+selectedMessage,activeRoom);
                  inputField.clear();
		}
        }


		@Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                
            } else {
                Text messageText = new Text(item);
                Font.loadFont(getClass().getResourceAsStream("/fonts/Salsa-Regular.ttf"), 14);
                messageText.setFont(Font.font("Salsa", 17));
                String[] parts = item.split(":")[0].split("\\)", 2);
                String messageUsername = (parts.length > 1) ? parts[1].trim() : "";
                boolean isClientMessage = messageUsername.equalsIgnoreCase(chatClient.getUserName());
               
                if (item.startsWith("Server:")) {
                	
                	 Font.loadFont(getClass().getResourceAsStream("/fonts/PlayfairDisplay-VariableFont_wght.ttf"), 14);
                	 messageText.setFont(Font.font("PlayfairDisplay", FontWeight.BOLD, 14));
                	  editButton.setVisible(false);
                } else if(item.startsWith("Private message from")){
                	editButton.setVisible(false);
                }
                else {
                    editButton.setVisible(true); 
                }
                
                if (isClientMessage) {
                	Region spacer = new Region();
                	HBox messageBox = new HBox(messageText, spacer, editButton );
                	 messageBox.setAlignment(Pos.CENTER);
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    setGraphic(messageBox);
                } else {
                	
                	  setStyle("-fx-background-color: #eeeee4; -fx-text-fill: white; ");
                	  
                	  setOnMouseClicked(event -> {
                          setStyle("-fx-background-color: #bebeb6; -fx-text-fill: white; "); 
                      });
                	  Region spacer = new Region();
                	  HBox.setHgrow(spacer, Priority.ALWAYS);
                	  HBox messageBox = new HBox(messageText, spacer, editButton);
                      messageBox.setAlignment(Pos.CENTER);
                      messageBox.setStyle("-fx-padding: 5px 0;");

                      setGraphic(messageBox);
                    
                }
            }		
        }

        }
  
    public class UserListCell extends ListCell<String> {
        private static final String USER_STYLE_NORMAL = "-fx-background-color: #e1ecf4; -fx-text-fill: #10435b; -fx-padding: 5 10; -fx-font-size: 14px; -fx-border-width: 1; -fx-border-color: #c4d7ed;";
        private static final String USER_STYLE_HOVER = "-fx-background-color: #92cbdf; -fx-text-fill: white; -fx-padding: 5 10; -fx-font-size: 14px; -fx-border-width: 1; -fx-border-color: #c4d7ed;";
        private static final String USER_PRESSED = "-fx-background-color: #e1ecf4; -fx-text-fill: #10435b; -fx-padding: 5 10; -fx-font-size: 14px; -fx-border-width: 1; -fx-border-color: #c4d7ed;";

        private static final int CIRCLE_LABEL_MARGIN_RIGHT = 80;

     

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
            } else {
                HBox container = new HBox();

                Label nameLabel = new Label("-> " + item);
                nameLabel.setMinWidth(120); 
                nameLabel.setMaxWidth(120);
                nameLabel.setStyle("-fx-text-fill: #10435b; -fx-font-size: 14px;");
                nameLabel.setStyle("-fx-font-weight: bold;");

                StackPane stackPane = new StackPane();
                Label circleLabel = new Label("●");
                if (item != null && item.startsWith("Server:") && item.contains("has disconnected!")) {
                    String disconnectedUser = item.substring(item.indexOf("[") + 1, item.indexOf("]"));
                    boolean isDisconnectedUser = disconnectedUser.equals(item.substring(item.indexOf("["), item.indexOf("]")));
                    if (isDisconnectedUser) {
                        circleLabel.setStyle("-fx-text-fill: #FF0000;");
                    } else {
                        circleLabel.setStyle("-fx-text-fill: #00FF00;");
                    }
                } else {
                    
                    circleLabel.setStyle("-fx-text-fill: #00FF00;");
                }


                stackPane.getChildren().add(circleLabel);
               // stackPane.setStyle("-fx-alignment: CENTER_RIGHT;");

                StackPane.setMargin(circleLabel, new Insets(0, 0, 0, CIRCLE_LABEL_MARGIN_RIGHT));

                container.getChildren().addAll(nameLabel, stackPane);

                setGraphic(container);
                setStyle(USER_STYLE_NORMAL);
                setOnMouseEntered(event -> setStyle(USER_STYLE_HOVER));
                setOnMouseExited(event -> setStyle(USER_STYLE_NORMAL));
                setOnMousePressed(event -> setStyle(USER_PRESSED));
            }
        }
    }

    
    @Override
    public void handleMessageUpdate(ChatMessage oldMessage, ChatMessage message, String room) {
        Platform.runLater(() -> {
            System.out.println("Old Message Format: " + oldMessage.toString().trim());
            System.out.println("Message Format: " + message.toString().trim());

            String messageText = message.toString().trim();

           
                ObservableList<String> items = messageListView.getItems();

                for (int i = 0; i < items.size(); i++) {
                    String existingMessage = items.get(i).trim();

                    if (existingMessage.equalsIgnoreCase(oldMessage.toString().trim())) {
                        items.remove(i);  
                        items.add(i, messageText);  
                        break;  
                    }
                }
            
        });
    }

    @Override
    public void handleUserListUpdate(List<String> users, String room) {
        Platform.runLater(() -> {
            ObservableList<String> userList = userListView.getItems();

           
            userList.removeIf(user -> !users.contains(user));

            
            users.stream()
                 .filter(user -> !userList.contains(user))
                 .forEach(userList::add);

            activeRoom = room;
        });
    }

    private void userInput() {
        String userInput = inputField.getText().trim();

        if (!userInput.isEmpty()) {
            if (lastSelectedMessage == null) {
                if (lastSelectedUser != null) {
                    chatClient.sendPrivateMessage(lastSelectedUser, userInput);
                } else {
                    chatClient.userInput(userInput, activeRoom);
                }
            } else {
                if (lastSelectedMessage.startsWith("Private message from")) {
                    String[] parts = lastSelectedMessage.split(":", 2)[0].split(" ");
                    String from = parts[parts.length - 1];

                    String replyMessage = String.format("replied to message:\n(%s)\n%s", lastSelectedMessage, userInput);
                    chatClient.sendPrivateMessage(from, replyMessage);
                } else {
                    String replyMessage = String.format("replied to message:\n(%s)\n%s", lastSelectedMessage, userInput);
                    chatClient.userInput(replyMessage, activeRoom);
                }

                messageListView.getSelectionModel().clearSelection();
                lastSelectedMessage = null;
            }

            inputField.clear();
        }
    }

    
    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
  
}