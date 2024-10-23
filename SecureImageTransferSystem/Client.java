import javax.swing.*;
import java.net.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Client {

    private static final String AES_KEY = "1234567890123456";  // Secret key for encryption

    public static void main(String args[]) throws Exception {
        Socket socket;
        BufferedImage img = null;
        File imageFile = new File("digital_image_processing.jpg");

        // Validate image size and format
        if (!imageFile.exists() || !isValidImage(imageFile)) {
            System.out.println("Invalid image file.");
            return;
        }

        socket = new Socket("localhost", 4000);
        System.out.println("Client is running.");

        try {
            // Read image from disk
            System.out.println("Reading image from disk.");
            img = ImageIO.read(imageFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            baos.flush();
            byte[] imageData = baos.toByteArray();
            baos.close();

            // Encrypt the image
            byte[] encryptedData = encrypt(imageData);

            // Send image to server
            System.out.println("Sending image to server.");
            OutputStream out = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(out);

            // Send metadata
            dos.writeUTF(imageFile.getName());
            dos.writeInt(img.getWidth());
            dos.writeInt(img.getHeight());

            // Send encrypted image size and data
            dos.writeInt(encryptedData.length);
            dos.write(encryptedData, 0, encryptedData.length);

            System.out.println("Image sent to server.");
            dos.close();
            out.close();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        } finally {
            socket.close();
        }
    }

    // AES encryption
    private static byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    // Validate image file format and size
    private static boolean isValidImage(File imageFile) {
        String fileName = imageFile.getName();
        long fileSize = imageFile.length();

        if (fileSize > 5 * 1024 * 1024) { // 5 MB size limit
            System.out.println("File size exceeds the limit.");
            return false;
        }

        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".png")) {
            System.out.println("Invalid file format. Only JPG and PNG are allowed.");
            return false;
        }

        return true;
    }
}
