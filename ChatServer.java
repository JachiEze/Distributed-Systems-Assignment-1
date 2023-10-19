import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 3000;
    private static Map<String, PrintWriter> onlineClients = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> chatRooms = new ConcurrentHashMap<>();
    private static Map<String, BufferedWriter> chatRoomFiles = new ConcurrentHashMap<>();
    private static Map<String, String> clientChatRooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat Server is running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // User Registration
                username = in.readLine();
                onlineClients.put(username, out);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);

                    if (message.equals("/exit")) {
                        processUserExit(username);
                        break;
                    } else if (message.startsWith("/create ")) {
                        // Create a chat room
                        processCreateChatRoom(username, message);
                    } else if (message.startsWith("/join ")) {
                        // Join a chat room
                        processJoinChatRoom(username, message);
                    } else if (message.startsWith("/leave")) {
                        // Leave the current chat room
                        processLeaveChatRoom(username);
                    } else if (message.startsWith("/list")) {
                        // List available chat rooms
                        processListChatRooms();
                    } else {
                        // Broadcast the message to the current chat room
                        broadcastToChatRoom(username, message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (username != null) {
                    onlineClients.remove(username);
                    String currentChatRoom = clientChatRooms.get(username);
                    if (currentChatRoom != null) {
                        chatRooms.get(currentChatRoom).remove(username);
                        closeChatRoomFile(currentChatRoom);
                    }
                    clientChatRooms.remove(username);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processUserExit(String sender) {
            out.println(sender + " has left the chat.");
            String currentChatRoom = clientChatRooms.get(sender);
            if (currentChatRoom != null) {
                chatRooms.get(currentChatRoom).remove(sender);
                closeChatRoomFile(currentChatRoom);
                clientChatRooms.remove(sender);
            }
        }

        private void processCreateChatRoom(String sender, String message) {
            String roomName = message.substring(8);
            chatRooms.computeIfAbsent(roomName, k -> new HashSet<>());
            chatRooms.get(roomName).add(sender);
            clientChatRooms.put(sender, roomName);
            out.println("You have created and joined the chat room: " + roomName);
            openChatRoomFile(roomName);
        }

        private void openChatRoomFile(String roomName) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(roomName + ".txt", true));
                chatRoomFiles.put(roomName, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void closeChatRoomFile(String roomName) {
            BufferedWriter writer = chatRoomFiles.get(roomName);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processJoinChatRoom(String sender, String message) {
            String roomName = message.substring(6);
            if (chatRooms.containsKey(roomName)) {
                String currentChatRoom = clientChatRooms.get(sender);
                if (currentChatRoom != null) {
                    chatRooms.get(currentChatRoom).remove(sender);
                    closeChatRoomFile(currentChatRoom);
                }
                chatRooms.get(roomName).add(sender);
                clientChatRooms.put(sender, roomName);
                out.println("You have joined the chat room: " + roomName);
                openChatRoomFile(roomName);
            } else {
                out.println("Chat room " + roomName + " does not exist.");
            }
        }

        private void processLeaveChatRoom(String sender) {
            String currentChatRoom = clientChatRooms.get(sender);
            if (currentChatRoom != null) {
                chatRooms.get(currentChatRoom).remove(sender);
                out.println("You have left the chat room: " + currentChatRoom);
                closeChatRoomFile(currentChatRoom);
                clientChatRooms.remove(sender);
            }
        }

        private void processListChatRooms() {
            if (chatRooms.isEmpty()) {
                out.println("No chat rooms available.");
            } else {
                out.println("Available chat rooms: " + String.join(", ", chatRooms.keySet()));
            }
        }

        private void broadcastToChatRoom(String sender, String message) {
            String currentChatRoom = clientChatRooms.get(sender);
            if (currentChatRoom != null) {
                Set<String> roomMembers = chatRooms.get(currentChatRoom);
                for (String member : roomMembers) {
                    PrintWriter memberOut = onlineClients.get(member);
                    if (memberOut != null && !member.equals(sender)) {
                        memberOut.println(username + " (" + currentChatRoom + "): " + message);
                    }
                }

                // Store the message in the chat room's text file
                BufferedWriter writer = chatRoomFiles.get(currentChatRoom);
                if (writer != null) {
                    try {
                        writer.write(username + " (" + currentChatRoom + "): " + message);
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                out.println("You are not in a chat room. Use /create or /join to enter a chat room.");
            }
        }
    }
}


