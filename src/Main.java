import com.sun.security.jgss.GSSUtil;
import jdk.swing.interop.SwingInterOpUtils;
import messages.*;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.DoubleToIntFunction;

public class Main {
    Socket socket;
    InputStream socketInputStream;
    OutputStream socketOutputStream;
    BufferedReader reader;
    PrintWriter writer;
    private ArrayList<String> messages= new ArrayList<>();

    private ArrayList<BCSTMessage> broadcastMailbox;
    private ArrayList<Message> otherMessages;
    Lock messagesLock = new ReentrantLock();
    Condition emptyMessages = messagesLock.newCondition();
    private ArrayList<Message> postBox;
    Lock postboxLock = new ReentrantLock();
    Condition emptyPostBox = postboxLock.newCondition();
    boolean runChat;


    public static void main(String[] args) {
        new Main().run();
    }

    public void run(){
        try {
            this.socket = new Socket("127.0.0.1", 1337);
            this.socketInputStream = this.socket.getInputStream();
            this.socketOutputStream = this.socket.getOutputStream();
            this.reader = new BufferedReader(new InputStreamReader(this.socketInputStream));
            this.writer = new PrintWriter(new PrintWriter(this.socketOutputStream));
        } catch (IOException e) {
            System.out.println("Something went wrong with connecting to the chat server.");
            e.printStackTrace();
            return;
        }

        System.out.println("Connected to chat server!");

        if (handshake()) {
            startClient();
        } else {
            terminateConnection();
        }
    }

    public void startClient(){
        System.out.println("please enter your username: ");
        Scanner scanner = new Scanner(System.in);
        String username = scanner.nextLine();

        writer.println("HELO " + username);
        writer.flush();

        Message message = null;
        try {
            message = Message.formulate(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (message instanceof OKMessage) {
            String rawConfirmation = message.getContent();
            String[] splitMessage = rawConfirmation.split(" ");

            ReaderProcess readerProcess = new ReaderProcess(reader);
            Thread readThread = new Thread(readerProcess);

            WriterProcess writerProcess = new WriterProcess(writer);
            Thread writeThread = new Thread(writerProcess);

            MessageHandler handlerProcess = new MessageHandler();
            Thread handlerThread = new Thread(handlerProcess);

            if (splitMessage[0].equals("HELO")) {
                if (splitMessage[1].equals(username)) {
                    System.out.println("Login successful.");
                    runChat = true;
                    broadcastMailbox = new ArrayList<>();
                    postBox = new ArrayList<>();
                    readThread.start();
                    writeThread.start();
                    handlerThread.start();
                } else {
                    terminateConnection();
                }
            }

            while (runChat) {
                System.out.println("    Logged in as: "+ username);
                System.out.println(" 1. Read broadcasted messages");
                System.out.println(" 2. Broadcast a message");
                System.out.println(" 3. Quit");
                System.out.println(" Select a menu option:");

                Scanner menuScanner = new Scanner(System.in);
                int menuChoice = menuScanner.nextInt();
                if (menuChoice == 1) {
                    if (broadcastMailbox.isEmpty()) {
                        System.out.println("no broadcast message received in the mailbox");
                    } else {
                        for (BCSTMessage bcstMessage: broadcastMailbox) {
                            System.out.println(bcstMessage.getContent());
                        }
                    }
                } else if(menuChoice == 2) {
                    System.out.println("Enter your broadcast message:");
                    Scanner messageScanner = new Scanner(System.in);
                    String broadcast = messageScanner.nextLine();
                    postBox.add(new BCSTMessage(broadcast));
                    emptyPostBox.signal();
                } else if(menuChoice == 3) {
                    runChat = false;
                } else {
                    System.out.println("Incorrect input.");
                }
            }

            handlerProcess.stop();
            readerProcess.stop();
            writerProcess.stop();
            terminateConnection();
        }
    }

    public boolean handshake(){
        String firstMessage = null;
        try {
            firstMessage =reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (firstMessage == null) {
            System.out.println("Handshake failed.");
            terminateConnection();
        }else {
            String[] splitMessage = firstMessage.split(" ");
            if (splitMessage[0].equals("HELO")) {
                System.out.println("Handshake Successful.");
                String welcomeMessage = "";
                for (int i = 1; i < splitMessage.length; i++) {
                    welcomeMessage += splitMessage[i];
                }
                System.out.println(welcomeMessage);
                return true;
            } else {
                System.out.println("Wrong protocol.");
                System.out.println("Handshake failed.");
                terminateConnection();
            }
        }
        return false;
    }

    public void terminateConnection(){
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Socket failed to close.");
            e.printStackTrace();
        }
        System.out.println("Socket closed, connection successfully terminated.");
    }

    class ReaderProcess implements Runnable{
        private BufferedReader reader;
        private boolean run;
        public ReaderProcess(BufferedReader reader){
            this.reader = reader;
            this.run = true;
        }
        @Override
        public void run() {
            while (run) {
                Message message = null;
                try {
                    message = Message.formulate(reader.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (message instanceof BCSTMessage) {
                    broadcastMailbox.add((BCSTMessage) message);
                } else {
                    otherMessages.add(message);
                    emptyMessages.signal();
                }
            }
        }

        public void stop(){
            this.run = false;
        }
    }

    class MessageHandler implements Runnable {
        private boolean run;
        public MessageHandler(){
            this.run=true;
        }
        @Override
        public void run() {
            while (run) {
                if (otherMessages.isEmpty()) {
                    try {
                        emptyMessages.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Message message = otherMessages.remove(0);

                if(message instanceof OKMessage){

                }else if (message instanceof DSCNMessage) {
                    runChat= false;
                    System.out.println(message.getContent());
                }else if (message instanceof PINGMessage) {
                    postBox.add(new PONGMessage());
                    emptyPostBox.signal();
                } else {
                    System.out.println(message.getContent());
                }
            }
        }
        public void stop(){
            this.run = false;
        }
    }


    class WriterProcess implements Runnable {
        private PrintWriter printWriter;
        private boolean run;

        public WriterProcess(PrintWriter writer) {
            printWriter = writer;
            this.run = true;
        }

        @Override
        public void run() {
            while (run) {
                if (postBox.isEmpty()) {
                    try {
                        emptyPostBox.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Message message = postBox.remove(0);

                writer.println(Message.createString(message));
                writer.flush();
            }
        }

        public void stop(){
            this.run = false;
        }
    }
}
