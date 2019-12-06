package messages;

public class OKMessage extends Message {
    Message message;

    public OKMessage(String content) {
        super(content);
        this.message = Message.formulate(content);
    }

    public Message getMessage() {
        return message;
    }
}
