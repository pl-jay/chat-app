package lk.pathum;



import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * A multithreaded chat room server. When a client connects the server requests
 * a screen name by sending the client the text "SUBMITNAME", and keeps
 * requesting a name until a unique one is received. After a client submits a
 * unique name, the server acknowledges with "NAMEACCEPTED". Then all messages
 * from that client will be broadcast to all other clients that have submitted a
 * unique screen name. The broadcast messages are prefixed with "MESSAGE".
 *
 * This is just a teaching example so it can be enhanced in many ways, e.g.,
 * better logging. Another is to accept a lot of fun commands, like Slack.
 */
public class ChatServer {

    // All client names, so we can check for duplicates upon registration.
    private static Set<String> names = new HashSet<>();

    private static HashMap<String, Socket> users = new HashMap<>();

    // The set of all the print writers for all the clients, used for broadcast.
    private static Set<PrintWriter> writers = new HashSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running...");
        var pool = Executors.newFixedThreadPool(500);
        try (var listener = new ServerSocket(59001)) {
            while (true) {
                Socket clientSocket = listener.accept();
                System.out.println("Recieved connection from " + clientSocket.getInetAddress() + " on port "
                        + clientSocket.getPort());
                pool.execute(new Handler(clientSocket));
            }
        }
    }

    private static class Handler implements Runnable {
        private String name;
        private String message;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;


        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void clientSocket(String ipaddress,int port) throws UnknownHostException, IOException {

            Socket cSocket;
            try{

                cSocket = new Socket(ipaddress, port);

                in = new Scanner(cSocket.getInputStream());
                out = new PrintWriter(cSocket.getOutputStream(), true);


                out.println("SENDMSG");

                String input = in.nextLine();

                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + ": " + input);
                }

            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (out != null) {
                    writers.remove(out);
                }
                if (name != null) {
                    System.out.println(name + " is leaving");
                    names.remove(name);
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + " has left");
                    }
                }
                // try {
                //    // cSocket.close();
                // } catch ( IOException e) {
                // }
            }
        }

        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!name.isBlank() && !names.contains(name)) {
                            names.add(name);
                            users.put(name, socket);
                            break;
                        }
                    }
                }

                out.println("NAMEACCEPTED " + name);
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + " has joined");
                }
                //writers.add(out);

                String message = in.nextLine();

                Socket socket2 = users.get(message.split("(->)")[1]);

                sendMessageTo(in.nextLine(),socket2);

//                while(true){
//                    message = in.nextLine();
//                    for (PrintWriter writer : writers) {
//                        writer.println("MESSAGE " + name + ": " + message);
//                        System.out.println(message);
//                    }
//                    sendMessageTo(message);
//                }
            }

            catch (Exception e) {
                System.out.println(e);
            } finally {
                if (out != null) {
                    writers.remove(out);
                }
                if (name != null) {
                    System.out.println(name + " is leaving");
                    names.remove(name);
                    for (final PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + " has left");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        public void sendMessageTo(String msgString, Socket socket) throws IOException {
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(msgString);
        }


    }
}
