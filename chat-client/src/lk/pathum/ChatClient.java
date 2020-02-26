package lk.pathum;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    String serverAddress;
    int port;
    Scanner in,uScanner;
    PrintWriter out;

    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    public ChatClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    private String getName() {
        return in.nextLine();
    }

    private void run() throws IOException {
        try {
            var socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {

                var line = in.nextLine();

                if (line.startsWith("SUBMITNAME")) {
                    System.out.print("submit name: ");
                    out.println(new Scanner(System.in).nextLine());

                } else if (line.startsWith("NAMEACCEPTED")) {
                    System.out.append("> ");
                    String msg = in.nextLine();
                    if(msg.startsWith("send")){
                        out.println(msg.substring(msg.indexOf("send")+5));
                    }
                }

                else if (line.startsWith("MESSAGE")) {
                    System.out.append("> "+line.substring(0) + "\n");
                }
            }
        } finally {

        }
    }





    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }

        var client = new ChatClient(args[0]);
        client.run();
    }
}