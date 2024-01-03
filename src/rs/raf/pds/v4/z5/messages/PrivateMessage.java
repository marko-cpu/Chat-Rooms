package rs.raf.pds.v4.z5.messages;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PrivateMessage {
	    private String sender;
	    private String content;
	    private String recipient;
	    private String sentDate;

	    public PrivateMessage() {
	        super();
	    }

	    public PrivateMessage(String sender, String content, String recipient) {
	        Date date = Calendar.getInstance().getTime();
	        DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
	        sentDate = dateFormat.format(date);
	        this.sender = sender;
	        this.content =  " [" + sentDate + "] " + content ;
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

	    public String getSentDate() {
	        return sentDate;
	    }
}
