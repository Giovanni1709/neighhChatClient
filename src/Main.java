import com.sun.security.jgss.GSSUtil;
import jdk.swing.interop.SwingInterOpUtils;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    private ArrayList<String> messages= new ArrayList<>();
    public static void main(String[] args) {
        new Main().run();
    }

    public void run(){
        try {
            Socket socket = new Socket("127.0.0.1", 1337);
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Thread readThread = new Thread(new ReaderThread(reader));
            readThread.start();
            PrintWriter writer = new PrintWriter(outputStream);
            Thread writeThread = new Thread(new WriterThread(writer));
            writeThread.start();
            while (true){
                if (messages.size() > 0) {
                    String message = messages.remove(0);
                    System.out.println("removed from mailbox: "+ message);
                    if (message.equals("PING")){
                        System.out.println("ping received");
                        writer.println("PONG");
                        writer.flush();
                        System.out.println("wrote PONG");
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        } catch (IOException e) {
            System.out.println("something went wrong with getting Streams");
            e.printStackTrace();
        }
    }


    class ReaderThread implements Runnable{
        private BufferedReader reader;
        public ReaderThread(BufferedReader reader){
            this.reader = reader;
        }
        @Override
        public void run() {
            while (true) {
                String line = null;
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                messages.add(line);
                System.out.println("added to mailbox: " + line);
            }
        }
    }



    class WriterThread implements Runnable {
        private PrintWriter printWriter;

        public WriterThread(PrintWriter writer) {
            printWriter = writer;
        }

        @Override
        public void run() {
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String message = scanner.nextLine();
                printWriter.println(message);
                printWriter.flush();
            }
        }
    }
}
