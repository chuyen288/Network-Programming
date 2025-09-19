package client;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@SuppressWarnings("unused")
public class client {
    private JFrame frame;
    private List<String> orderHistory = new ArrayList<>(); // Lịch sử đơn hàng (lấy từ server)
    private List<JCheckBox> seatList = new ArrayList<>(); // Danh sách ghế
    private String selectedMovie = "";
    private JSpinner combo1Spinner, combo2Spinner, colaSpinner, bapSpinner;
    private JLabel lblTicketId;

    // Socket để giao tiếp với server
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(client::new);
    }

    public client() {
        connectToServer(); // Kết nối đến server trước
        showLogin();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Không kết nối được đến server: " + e.getMessage());
            System.exit(1);
        }
    }

    // ---------------- LOGIN ----------------
    private void showLogin() {
        JFrame loginFrame = new JFrame("🎬 Cinema Booking - Login");
        loginFrame.setSize(400, 300);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(230, 240, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Đăng nhập hệ thống");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setForeground(new Color(30, 30, 150));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);

        JTextField userField = new JTextField();
        gbc.gridx = 1;
        panel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);

        JPasswordField passField = new JPasswordField();
        gbc.gridx = 1;
        panel.add(passField, gbc);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        loginBtn.setBackground(new Color(100, 180, 255));
        registerBtn.setBackground(new Color(180, 220, 180));

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(loginBtn, gbc);
        gbc.gridx = 1;
        panel.add(registerBtn, gbc);

        loginFrame.add(panel, BorderLayout.CENTER);

        // Sự kiện
        loginBtn.addActionListener(e -> {
            username = userField.getText();
            // Gửi username đến server (đăng nhập giả lập)
            out.println(username);
            loginFrame.dispose();
            showMovieSelection(username);
        });

        registerBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(loginFrame, "Đăng ký thành công!");
        });

        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    // ---------------- MOVIE SELECTION ----------------
    private void showMovieSelection(String username) {
        JFrame movieFrame = new JFrame("🎥 Chọn phim");
        movieFrame.setSize(400, 200);
        movieFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        movieFrame.setLayout(new BorderLayout());

        // Lấy danh sách phim từ server
        out.println("LIST_MOVIES");
        String movieListStr;
        try {
            movieListStr = in.readLine();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Lỗi lấy danh sách phim: " + e.getMessage());
            return;
        }
        String[] movies = movieListStr.split(",");

        JComboBox<String> movieBox = new JComboBox<>(movies);

        JButton continueBtn = new JButton("Tiếp tục");
        continueBtn.setBackground(new Color(100, 200, 120));

        movieFrame.add(new JLabel("Chọn phim:", SwingConstants.CENTER), BorderLayout.NORTH);
        movieFrame.add(movieBox, BorderLayout.CENTER);
        movieFrame.add(continueBtn, BorderLayout.SOUTH);

        continueBtn.addActionListener(e -> {
            selectedMovie = (String) movieBox.getSelectedItem();
            movieFrame.dispose();
            showMainUI(username);
        });

        movieFrame.setLocationRelativeTo(null);
        movieFrame.setVisible(true);
    }

    // ---------------- MAIN UI ----------------
    private void showMainUI(String username) {
        frame = new JFrame("🍿 Cinema Booking - Xin chào " + username + " | Phim: " + selectedMovie);
        frame.setSize(1000, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Left: Snacks
        frame.add(createSnackPanel(), BorderLayout.WEST);

        // Center: Seats
        frame.add(createSeatPanel(), BorderLayout.CENTER);

        // Right: Toolbar
        frame.add(createToolbarPanel(username), BorderLayout.EAST);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---------------- SEAT PANEL ----------------
    private JPanel createSeatPanel() {
        seatList.clear(); // reset danh sách ghế mới
        JPanel seatPanel = new JPanel(new GridLayout(5, 6, 5, 5));
        seatPanel.setBorder(BorderFactory.createTitledBorder("Chọn ghế"));

        // Lấy trạng thái ghế từ server
        out.println("GET_SEATS|" + selectedMovie);
        String seatStatusStr;
        try {
            seatStatusStr = in.readLine();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Lỗi lấy trạng thái ghế: " + e.getMessage());
            return seatPanel;
        }
        String[] seatStatuses = seatStatusStr.split(",");

        for (int i = 1; i <= 30; i++) {
            JCheckBox seat = new JCheckBox("Ghế " + i);
            boolean isBooked = seatStatuses[i-1].equals("1");
            if (isBooked) {
                seat.setBackground(Color.RED);
                seat.setEnabled(false);
            } else {
                if (i <= 10) seat.setBackground(Color.GREEN); // hạng thường
                else if (i <= 20) seat.setBackground(Color.ORANGE); // hạng vip
                else seat.setBackground(Color.CYAN); // hạng đôi
                seat.setEnabled(true);
            }
            seat.setOpaque(true);
            seatList.add(seat);
            seatPanel.add(seat);
        }

        return seatPanel;
    }

    // ---------------- SNACK PANEL ----------------
    private JPanel createSnackPanel() {
        JPanel snackPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        snackPanel.setBorder(BorderFactory.createTitledBorder("Order Snack & Drinks"));

        combo1Spinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        combo2Spinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        colaSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        bapSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));

        snackPanel.add(new JLabel("Combo1 (100k):"));
        snackPanel.add(combo1Spinner);
        snackPanel.add(new JLabel("Combo2 (150k):"));
        snackPanel.add(combo2Spinner);
        snackPanel.add(new JLabel("Cola (50k):"));
        snackPanel.add(colaSpinner);
        snackPanel.add(new JLabel("Bắp rang (70k):"));
        snackPanel.add(bapSpinner);

        JButton btnOrderSnack = new JButton("Đặt đồ ăn/uống");
        btnOrderSnack.setBackground(new Color(240, 200, 100));
        snackPanel.add(new JLabel());
        snackPanel.add(btnOrderSnack);

        btnOrderSnack.addActionListener(e -> orderSnacks());

        return snackPanel;
    }

    // ---------------- TOOLBAR PANEL ----------------
    private JPanel createToolbarPanel(String username) {
        JPanel toolbar = new JPanel(new GridLayout(6, 1, 5, 5)); // tăng thêm 1 dòng
        toolbar.setBorder(BorderFactory.createTitledBorder("Tiện ích"));

        lblTicketId = new JLabel("Ticket ID: Chưa có");
        toolbar.add(lblTicketId);

        JButton btnRefresh = new JButton("Refresh Seats");
        JButton btnBookSeat = new JButton("Đặt vé");
        JButton btnMyOrders = new JButton("Đơn của tôi");
        JButton btnChangeMovie = new JButton("Chọn phim khác");
        JButton btnLogout = new JButton("Logout");

        toolbar.add(btnRefresh);
        toolbar.add(btnBookSeat);
        toolbar.add(btnMyOrders);
        toolbar.add(btnChangeMovie);
        toolbar.add(btnLogout);

        // Refresh seats
        btnRefresh.addActionListener(e -> {
            frame.remove(frame.getContentPane().getComponent(1)); // Xóa seat panel cũ
            frame.add(createSeatPanel(), BorderLayout.CENTER); // Tạo mới từ server
            frame.revalidate();
            frame.repaint();
        });

        // Book seats
        btnBookSeat.addActionListener(e -> bookSeats());

        // My Orders
        btnMyOrders.addActionListener(e -> showOrders());

        // Change Movie
        btnChangeMovie.addActionListener(e -> {
            frame.dispose();
            showMovieSelection(username);
        });

        // Logout
        btnLogout.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(frame, "Bạn muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Error closing socket");
                }
                frame.dispose();
                new client(); // Khởi động lại client mới
            }
        });

        return toolbar;
    }

    // ---------------- ORDER LOGIC ----------------
    private void bookSeats() {
        StringBuilder seatIndices = new StringBuilder();
        for (int i = 0; i < seatList.size(); i++) {
            JCheckBox seat = seatList.get(i);
            if (seat.isSelected() && seat.isEnabled()) {
                seatIndices.append(i).append(",");
            }
        }

        if (seatIndices.length() > 0) {
            seatIndices.deleteCharAt(seatIndices.length() - 1); // Xóa dấu , cuối
            out.println("BOOK_SEATS|" + selectedMovie + "|" + seatIndices);
            try {
                String response = in.readLine();
                String[] parts = response.split("\\|");
                if (parts[0].equals("SUCCESS")) {
                    String ticketId = parts[1];
                    String dateTime = parts[2];
                    String selectedSeats = parts[3];
                    lblTicketId.setText("Ticket ID: " + ticketId);

                    JOptionPane.showMessageDialog(frame,
                            "Bạn đã đặt vé!\nPhim: " + selectedMovie +
                                    "\nGhế: " + selectedSeats +
                                    "\nMã vé: " + ticketId +
                                    "\nNgày giờ: " + dateTime);

                    // Cập nhật giao diện ghế sau khi đặt thành công
                    frame.remove(frame.getContentPane().getComponent(1));
                    frame.add(createSeatPanel(), BorderLayout.CENTER);
                    frame.revalidate();
                    frame.repaint();
                } else {
                    JOptionPane.showMessageDialog(frame, parts[1]);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Lỗi đặt vé: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Chưa chọn ghế!");
        }
    }

    private void orderSnacks() {
        StringBuilder order = new StringBuilder();
        int c1 = (Integer) combo1Spinner.getValue();
        int c2 = (Integer) combo2Spinner.getValue();
        int c3 = (Integer) colaSpinner.getValue();
        int c4 = (Integer) bapSpinner.getValue();

        if (c1 > 0) order.append("Combo1 x").append(c1).append(" ");
        if (c2 > 0) order.append("Combo2 x").append(c2).append(" ");
        if (c3 > 0) order.append("Cola x").append(c3).append(" ");
        if (c4 > 0) order.append("Bắp rang x").append(c4).append(" ");

        if (order.length() > 0) {
            out.println("ORDER_SNACKS|" + selectedMovie + "|" + order);
            try {
                String response = in.readLine();
                String[] parts = response.split("\\|");
                if (parts[0].equals("SUCCESS")) {
                    String dateTime = parts[1];
                    String snackDetails = parts[2];
                    JOptionPane.showMessageDialog(frame, "Bạn đã đặt đồ ăn/uống: " + snackDetails + "\nThời gian: " + dateTime);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Lỗi đặt đồ ăn: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Chưa chọn gì!");
        }
    }

    private void showOrders() {
        out.println("GET_ORDERS");
        try {
            String ordersStr = in.readLine();
            if (ordersStr.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Bạn chưa có đơn nào!");
            } else {
                StringBuilder sb = new StringBuilder("📜 Danh sách đơn hàng:\n");
                String[] orders = ordersStr.split(";");
                for (String order : orders) {
                    sb.append("- ").append(order).append("\n");
                }
                JOptionPane.showMessageDialog(frame, sb.toString());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Lỗi lấy đơn hàng: " + e.getMessage());
        }
    }
}