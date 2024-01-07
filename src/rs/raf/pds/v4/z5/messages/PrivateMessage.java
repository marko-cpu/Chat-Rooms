package rs.raf.pds.v4.z5.messages;


public class PrivateMessage {
	    private String sender;
	    private String content;
	    private String recipient;
	   

	    public PrivateMessage() {
	        super();
	    }

	    public PrivateMessage(String sender, String content, String recipient) {
	        
	        this.sender = sender;
	        this.content = content ;
	        this.recipient = recipient;
	    }

	    public String getRecipient() {
	        return recipient;
	    }

	    public void setRecipient(String recipient) {
	        this.recipient = recipient;
	    }

	    public String getSender() {
	        return sender;
	    }

	    public String getContent() {
	        return content;
	    }

	  
}
