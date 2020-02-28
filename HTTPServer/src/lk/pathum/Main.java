package lk.pathum;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;


public class Main {

    private static HashMap<String, InetSocketAddress> users = new HashMap<>();
    private static String userMessage;

    public static void main(String[] args){
        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(8590),0);
            server.createContext("/").setHandler(Main::handleRequest);
            server.createContext("/sendMessage").setHandler(Main::incomingMessages);
            server.createContext("/connectToserver").setHandler(Main::connectToServer);
            server.createContext("/listUsers").setHandler(Main::usersList);

            server.start();

        } catch (IOException e){
            e.getStackTrace();
        }
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String response = " Hi there ";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        System.out.println(response.getBytes().toString());
        os.close();
    }


    private static void usersList(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();

        String response = users.entrySet().toString();
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        System.out.println(response.getBytes().toString());
        os.close();
    }

    private static void connectToServer(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        users.put(getRequestBody(exchange),exchange.getRemoteAddress());

        String response = String.valueOf(exchange.getRemoteAddress());

        System.out.println(response);
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());

        os.close();
    }

    private static void incomingMessages(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        sendMessage(getRequestBody(exchange));
        String response = getRequestBody(exchange);
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        System.out.println(response.toString());
        os.close();
    }

    private static String getRequestBody(HttpExchange exchange) throws IOException{
        String requestBody;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8"))) {
            requestBody = br.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        return requestBody;
    }

    private static void sendMessage(String message){
        System.out.println("sending...");
        HttpURLConnection con = null;
        var url = "http:/"+addressForUser(message.split(" ",2)[0])+"/new-message";
        System.out.println("send message meth "+url);
        var urlParameters = message.split(" ",2)[1];
        System.out.println(urlParameters);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(urlParameters))
            .build();

    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                System.out.println(response.body());
                userMessage = response.body(); return response; })
            .thenAccept(System.out::println)
            .join();

//        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
//        try {
//            var myurl = new URL(url);
//            con = (HttpURLConnection) myurl.openConnection();
//            con.setDoOutput(true);
//            con.setRequestMethod("POST");
//            con.setRequestProperty("User-Agent", "Java client");
//            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            try (var wr = new DataOutputStream(con.getOutputStream())) {
//                wr.write(postData);
//            }
////            StringBuilder content;
////            try (var br = new BufferedReader(
////                    new InputStreamReader(con.getInputStream()))) {
////                String line;
////                content = new StringBuilder();
////                while ((line = br.readLine()) != null) {
////                    content.append(line);
////                    content.append(System.lineSeparator());
////                }
////            }
////            System.out.println(content.toString());
//        } catch (ProtocolException e) {
//            e.printStackTrace();
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            con.disconnect();
//        }
        System.out.println("user sends "+userMessage);
        System.out.println("message sended.");
    }

    private static String addressForUser(String user){
        return users.get(user).getAddress()+":"+ String.valueOf(users.get(user).getPort()+2);
    }
}
