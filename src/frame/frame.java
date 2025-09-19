package frame;

import javax.swing.*;

import client.client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
@SuppressWarnings({ "serial", "unused" })

public class frame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin, btnRegister;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public frame() {
        setTitle("Movie Booking - Login");
        setSize(300, 180);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2));

        add(new JLabel("Username:"));
        txtUser = new JTextField();
        add(txtUser);

        add(new JLabel("Password:"));
        txtPass = new JPasswordField();
        add(txtPass);

        btnLogin = new JButton("Login");
        btnRegister = new JButton("Register");
        add(btnLogin);
        add(btnRegister);

        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect to server!");
            System.exit(0);
        }

        btnLogin.addActionListener(e -> login());
        btnRegister.addActionListener(e -> register());
    }

    private void login() {
        String user = txtUser.getText();
        String pass = new String(txtPass.getPassword());
        out.println("LOGIN|" + user + "|" + pass);
        try {
            String response = in.readLine();
            if (response.equals("LOGIN_SUCCESS")) {
                JOptionPane.showMessageDialog(this, "Login success!");
                new client(socket, out, in, response).setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Login failed!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void register() {
        String user = txtUser.getText();
        String pass = new String(txtPass.getPassword());
        out.println("REGISTER|" + user + "|" + pass);
        try {
            String response = in.readLine();
            if (response.equals("REGISTER_SUCCESS")) {
                JOptionPane.showMessageDialog(this, "Register success! Now login.");
            } else {
                JOptionPane.showMessageDialog(this, "Username already exists!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new frame().setVisible(true));
    }
}
