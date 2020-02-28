package lk.pathum;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class HTTPClient {

    private static boolean isConnected = false;
    private static String username;
    private static String serverUrl;
    private static HttpURLConnection con;
    private static String userAddress;

    public static void main(String[] args) throws IOException {

        Scanner input = new Scanner(System.in);



        while(true){
            try {
                String command = input.nextLine();
                if (!isConnected) {
                    connectToServer();
                } else if (command.matches("send .* -> .*") && isConnected) {
                    String[] sub = command.substring(5).split(" -> ");
                    sendMessage(sub[0], sub[1]);
                } else if (command.equals("list") && isConnected) {
                    listUsers();
                }
            } finally {

            }
        }

    }

    private static void messageListner() throws IOException {

        int port = Integer.parseInt(userAddress.split(":")[1])+2;
        HttpServer server = HttpServer.create(new InetSocketAddress(port),0);
        server.createContext("/new-message").setHandler(HTTPClient::messageRequester);
        server.start();
        System.out.println(server.getAddress());
    }

    private static void messageRequester(HttpExchange exchange) throws IOException {
            URI requestURI = exchange.getRequestURI();
            System.out.println(exchange.getRequestBody());
            String respone = " Hi there user ";
            exchange.sendResponseHeaders(200, respone.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(respone.getBytes());
            System.out.println(respone.getBytes().toString());
            os.close();

    }

    private static void listUsers(){

        var url = "http://"+serverUrl+"/listUsers";

        try {

            var myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();
            con.setRequestMethod("GET");
            List<String> content = new ArrayList<>();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    content.add(line.toString());
                }
            }
            content.forEach(System.out::print);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            con.disconnect();
        }
    }

    private static void connectToServer() throws IOException {
        Scanner inputS = new Scanner(System.in);
        System.out.println("Type 'connect <host address>:<port> as <username>' ");
        String hostPc = inputS.nextLine();
        String[] split = hostPc.split(" ", 4);
        serverUrl = split[1];
        username = split[3];
        //inputS.close();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://"+serverUrl+"/connectToserver"))
                .POST(HttpRequest.BodyPublishers.ofString(username))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> { userAddress = response.body(); return response; })
                .thenAccept(System.out::println)
                .join();
        isConnected = true;
        System.out.println("User Address: "+userAddress);
        if(isConnected) messageListner();
    }

    private static void sendMessage(String message,String to){

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://"+serverUrl+"/sendMessage"))
                .POST(HttpRequest.BodyPublishers.ofString(to+" "+"["+username+"]"+":"+message))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();
    }
}
