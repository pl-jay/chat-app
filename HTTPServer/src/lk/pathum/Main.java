package lk.pathum;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;


public class Main {

    public static void main(String[] args){
        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(8590),0);
            server.createContext("/").setHandler(Main::handleRequest);
            server.createContext("/recieve-messages").setHandler(Main::incomingMessages);


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

    private static void incomingMessages(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        printRequestInfo(exchange);
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
}
