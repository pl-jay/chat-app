package lk.pathum;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;


public class Main {

    private static HashMap<String, InetSocketAddress> users = new HashMap<>();

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
        printRequestInfo(exchange);
        String respone = " Hi there ";
        exchange.sendResponseHeaders(200, respone.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(respone.getBytes());
        System.out.println(respone.getBytes().toString());
        os.close();
    }


    private static void usersList(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();

        String respone = users.entrySet().toString();
        exchange.sendResponseHeaders(200, respone.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(respone.getBytes());
        System.out.println(respone.getBytes().toString());
        os.close();
    }

    private static void connectToServer(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        users.put(getRequestBody(exchange),exchange.getRemoteAddress());

        String respone = String.valueOf(exchange.getRemoteAddress().getPort());
        System.out.println(respone);
        exchange.sendResponseHeaders(200, respone.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(respone.getBytes());

        os.close();
    }

    private static void incomingMessages(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        sendMessage(getRequestBody(exchange));

        String respone = "Helo";
        exchange.sendResponseHeaders(200, respone.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(respone.getBytes());
        System.out.println(respone.getBytes().toString());
        os.close();
    }

    private static void printRequestInfo(HttpExchange exchange) {
        System.out.println("-- headers --");
        Headers requestHeaders = exchange.getRequestHeaders();
        requestHeaders.entrySet().forEach(System.out::println);

        System.out.println("-- principle --");
        HttpPrincipal principal = exchange.getPrincipal();
        System.out.println(principal);

        System.out.println("-- HTTP method --");
        String requestMethod = exchange.getRequestMethod();
        System.out.println(requestMethod);

        System.out.println("-- query --");
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        System.out.println(query);
    }

    private static String getRequestBody(HttpExchange exchange) throws IOException{
        String requestBody;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8"))) {
            requestBody = br.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        return requestBody;
    }

    private static boolean sendMessage(String message){

        HttpURLConnection con = null;
        var url = "https:/"+addressForUser(message.split(" ",2)[0]);
        System.out.println(url);
        var urlParameters = message.split(" ",2)[1];
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        try {
            var myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (var wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postData);
            }
            StringBuilder content;
            try (var br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {
                String line;
                content = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }
            System.out.println(content.toString());
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            con.disconnect();
        }
        return false;
    }

    private static String addressForUser(String user){
        return users.get(user).getAddress()+":"+users.get(user).getPort();
    }
}
