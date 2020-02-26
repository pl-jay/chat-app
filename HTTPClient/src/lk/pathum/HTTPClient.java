package lk.pathum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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


    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        while(true){
            try{
                String command = input.nextLine();
                if(!isConnected){
                    connectToServer();
                }else if(command.matches("send .* -> .*") && isConnected) {
                    String[] sub = command.substring(5).split(" -> ");
                    sendMessage(sub[0],sub[1]);
                } else if(command.equals("list") && isConnected){
                    listUsers();
                }

                messageRequest();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //abc=> [/127.0.0.1:56102 /127.0.0.1:56104]  abcd=> [/127.0.0.1:56106 /127.0.0.1:56108]

    private static void messageRequest(){
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://"+serverUrl+"/connectToserver"))
                .POST(HttpRequest.BodyPublishers.ofString(username))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();
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

    private static void connectToServer(){
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
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();
        isConnected = true;
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
