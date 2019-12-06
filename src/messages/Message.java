package messages;

public abstract class Message {
    public static Message formulate(String rawMessage){
        String[] splittedMessage = rawMessage.split(" ");
        Message message = null;
        String content = "";
        for (int i = 1; i < splittedMessage.length; i++) {
            content += splittedMessage[i]+ " ";
        }
        switch (splittedMessage[0]) {
            case "+OK":{
                message = new OKMessage(content);
                break;
            }
            case "PING":{
                message = new PINGMessage();
                break;
            }
            case "PONG":{
                message = new PONGMessage();
                break;
            }
            case "ERR":{
                message = new ERRMessage(content);
                break;
            }
            case "BCST":{
                message = new BCSTMessage(content);
                break;
            }
            case "DSCN":{
                message = new DSCNMessage(content);
                break;
            }
        }
        return message;
    }

    public static String createString(Message message) {
        if (message instanceof BCSTMessage) {
            return "BCST " + message.getContent();
        } else if (message instanceof PONGMessage) {
            return "PONG" + message.getContent();
        } else if (message instanceof QUITMessage) {
            return "QUIT";
        } else {
            return null;
        }
    }

    private String content;
    public Message(String content){
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
