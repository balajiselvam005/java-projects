import java.net.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.logging.*;
import java.nio.file.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final String AES_KEY = "1234567890123456";  // Secret key for encryption/decryption

    public static void main(String args[]) throws Exception {
        ServerSocket serverSocket = null;
        Socket clientSocket;

        // Setup logger
        FileHandler fileHandler = new FileHandler("server.log", true);
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);  // Avoid console logs
        logger.info("Server started.");

        try {
            serverSocket = new ServerSocket(4000);
            logger.info("Waiting for clients...");

            while (true) {
                clientSocket = serverSocket.accept();
                logger.info("Client connected.");
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            logger.severe("Error: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    // ClientHandler to handle multi-client support
    static class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream in = clientSocket.getInputStream();
                DataInputStream dis = new DataInputStream(in);

                // Read metadata
                String fileName = dis.readUTF();
                int width = dis.readInt();
                int height = dis.readInt();
                logger.info("Received image: " + fileName + " (" + width + "x" + height + ")");

                // Read encrypted image size and data
                int len = dis.readInt();
                byte[] encryptedData = new byte[len];
                dis.readFully(encryptedData);
                dis.close();
                in.close();

                // Decrypt image
                byte[] imageData = decrypt(encryptedData);
                InputStream ian = new ByteArrayInputStream(imageData);
                BufferedImage bImage = ImageIO.read(ian);

                // Save the image to a custom directory
                File dir = new File("uploaded_images");
                if (!dir.exists()) dir.mkdir();
                File outputFile = new File(dir, fileName);
                ImageIO.write(bImage, "jpg", outputFile);
                logger.info("Image saved to: " + outputFile.getAbsolutePath());

                // Display the image
                JFrame f = new JFrame("Server - " + fileName);
                ImageIcon icon = new ImageIcon(bImage);
                JLabel l = new JLabel();
                l.setIcon(icon);
                f.add(l);
                f.pack();
                f.setVisible(true);
            } catch (Exception e) {
                logger.severe("Client handler error: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.severe("Socket close error: " + e.getMessage());
                }
            }
        }

        // AES decryption
        private byte[] decrypt(byte[] encrypted) throws Exception {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(encrypted);
        }
    }
}
