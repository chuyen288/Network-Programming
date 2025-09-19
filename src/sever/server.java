package sever;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class server {
    private static final int PORT = 12345;
    private static Map<String, List<Boolean>> movieSeats = new HashMap<>(); // Trạng thái ghế cho từng phim: false = trống, true = đã đặt
    private static Map<String, List<String>> userOrders = new HashMap<>(); // Lịch sử đơn hàng cho từng user
    private static List<String> movies = Arrays.asList("Làm Giàu Với Ma", "Mưa Đỏ", "Tử Chiến Trên Không", "Cuộc Chiến Hột Xoàn");

    public static void main(String[] args) {
        // Khởi tạo trạng thái ghế cho từng phim (30 ghế, ban đầu tất cả trống)
        for (String movie : movies) {
            List<Boolean> seats = new ArrayList<>(Collections.nCopies(30, false));
            movieSeats.put(movie, seats);
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Tạo thread riêng cho từng client
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Nhận username từ client (sau khi đăng nhập)
                username = in.readLine();
                if (!userOrders.containsKey(username)) {
                    userOrders.put(username, new ArrayList<>());
                }

                String request;
                while ((request = in.readLine()) != null) {
                    String[] parts = request.split("\\|");
                    String command = parts[0];

                    switch (command) {
                        case "LIST_MOVIES":
                            out.println(String.join(",", movies));
                            break;
                        case "GET_SEATS":
                            String movie = parts[1];
                            List<Boolean> seats = movieSeats.get(movie);
                            StringBuilder seatStatus = new StringBuilder();
                            for (boolean booked : seats) {
                                seatStatus.append(booked ? "1" : "0").append(",");
                            }
                            out.println(seatStatus.toString());
                            break;
                        case "BOOK_SEATS":
                            movie = parts[1];
                            String[] seatIndices = parts[2].split(",");
                            List<Boolean> seatsToBook = movieSeats.get(movie);
                            StringBuilder selectedSeats = new StringBuilder();
                            boolean success = true;
                            for (String idxStr : seatIndices) {
                                int idx = Integer.parseInt(idxStr);
                                if (!seatsToBook.get(idx)) {
                                    seatsToBook.set(idx, true);
                                    selectedSeats.append("Ghế ").append(idx + 1).append(" ");
                                } else {
                                    success = false;
                                    break;
                                }
                            }
                            if (success) {
                                String ticketId = "V" + UUID.randomUUID().toString().substring(0, 8);
                                String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                                String order = "🎟 Vé [" + movie + "] | " + selectedSeats + "| Mã: " + ticketId + " | Thời gian: " + dateTime;
                                userOrders.get(username).add(order);
                                out.println("SUCCESS|" + ticketId + "|" + dateTime + "|" + selectedSeats);
                            } else {
                                out.println("FAIL|Ghế đã được đặt!");
                            }
                            break;
                        case "ORDER_SNACKS":
                            movie = parts[1];
                            String snackDetails = parts[2];
                            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                            String order = "🥤 Đồ ăn/uống [" + movie + "]: " + snackDetails + " | " + dateTime;
                            userOrders.get(username).add(order);
                            out.println("SUCCESS|" + dateTime + "|" + snackDetails);
                            break;
                        case "GET_ORDERS":
                            List<String> orders = userOrders.get(username);
                            out.println(String.join(";", orders));
                            break;
                        case "REFRESH_SEATS":
                            // Không cần làm gì vì trạng thái ghế đã trên server, client sẽ yêu cầu GET_SEATS lại
                            break;
                        default:
                            out.println("Invalid command");
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client " + username + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket");
                }
            }
        }
    }
}