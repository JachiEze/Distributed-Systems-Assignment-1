import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 3000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the Chat Server.");
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            out.println(username);

            Thread receiver = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.equals("/exit")) {
                            System.out.println("Server: The chat has ended.");
                            break;
                        }
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            receiver.start();

            String input;
            while (true) {
                input = scanner.nextLine();
                if (input.equals("/exit")) {
                    out.println("/exit");
                    break; // Exit the loop and close the client
                } else {
                    out.println(input);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}