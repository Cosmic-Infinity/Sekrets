/* Not all characters and languages are supported considering this is a console application.
 * ISO-8859-1 characters are supported in java, meaning all English and Latin characters can be worked with.
 * Emojis or certain special characters might not get read. I tried the program with Hindi, with varying degree of success.
 */

import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class EncodeToImage {
    private static String ext; // stores image extensiom
    private static BufferedImage image; // stores image
    private static int width; // stores image width
    private static int height; // stores image height
    private static int msg_bins_length; // stores the length of binary equivalent of the string
    private static int chr_count; // stores the number of characters in the word
    // private static Scanner ob = new Scanner(System.in);
    static int x, y, kc;

    public static void main() {
        try {
            System.out.println("Starting main..."); // Add this line

            initialise();
            System.out.println("Initialise complete."); // Add this line

            load(); // loading image
            System.out.println("Load complete."); // Add this line

            int key_length = 5; // there is no upper limit on the length of the key. But if the key is too long
            // and the message too short, some digits of the key will go unused
            byte[] key = new byte[key_length * 2];

            keyGen(key_length, key); // generating key, each digit of which which is stored in a byte array
            System.out.println("KeyGen complete."); // Add this line

            String[] msg = message(); // the binary of each character in message is stored in the cells of String
            // array msg[]
            System.out.println("Message complete."); // Add this line

            // System.out.println("Message binary :");
            // for (int i = 0; i < msg.length; i++)
            // System.out.print(msg[i] + " ");

            // System.out.println("\nKey Digits :");
            // for (int i = 0; i < key.length; i++)
            // System.out.print(key[i] + " ");

            if (!test(key)) {
                System.out.println("This key is incompatible for the message. Try another key, or a different image.");
                return;
            }

            encodeDataHelper(msg, key);
            System.out.println("encodeDataHelper complete."); // Add this line

            printOutput();
            System.out.println("printOutput complete."); // Add this line

        } catch (IOException e) {
            System.out.println("IOException in main: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception in main: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean test(byte[] key) {
        long total_image_pixels = (long) width * height;
        long estimated_pixels_needed = 0;

        // 1. 5 bits per character for length
        estimated_pixels_needed += (long) chr_count * 5;

        // 2. Actual bits for all characters
        estimated_pixels_needed += msg_bins_length;

        // 3. Estimate pixels skipped by the key mechanism
        long estimated_pixels_skipped_by_key = 0;
        if (key.length >= 2 && msg_bins_length > 0) {
            long total_skip_actions_in_one_key_cycle = 0;
            long total_bits_encoded_in_one_key_cycle = 0;
            for (int i = 0; i < key.length; i += 2) {
                if (key[i + 1] > 0) {
                    total_skip_actions_in_one_key_cycle += (long) key[i] * key[i + 1];
                    total_bits_encoded_in_one_key_cycle += key[i + 1];
                }
            }
            if (total_bits_encoded_in_one_key_cycle > 0) {
                double avg_skip_per_payload_bit = (double) total_skip_actions_in_one_key_cycle
                        / total_bits_encoded_in_one_key_cycle;
                estimated_pixels_skipped_by_key = (long) (msg_bins_length * avg_skip_per_payload_bit);
            }
        }
        estimated_pixels_needed += estimated_pixels_skipped_by_key;

        final int leeway_adjustment = 100;
        boolean is_compatible = (total_image_pixels >= (estimated_pixels_needed - leeway_adjustment));
        return is_compatible;
    }

    private static void encodeDataWithKey(String message, byte[] key) {
        int f = 0;
        byte[] key_ = new byte[key.length];
        for (int i = 0; i < key.length; i++) {
            key_[i] = key[i];
        }
        while (y < height) {
            while (x < width) {
                if (f == message.length())
                    return;

                while (key[kc + 1] == 0) {
                    kc += 2;
                    if (kc >= key.length) {
                        // reset the key to initial state and continue.
                        System.out.println("key reset");
                        for (int i = 0; i < key.length; i++) {
                            key[i] = key_[i];
                        }
                        kc = 0;
                        break; // Exit the while loop after resetting key
                    }
                }
                key[kc + 1]--;

                x = x + key[kc];
                if (x >= width) {
                    x = x - key[kc];
                    int added_to_x = width - x;
                    y++;
                    x = x + (key[kc] - added_to_x);
                    if (y >= height) {
                        System.out.println("Image is too small.");
                        return;
                    }
                }

                int p = image.getRGB(x, y);
                int a = (p >> 24) & 0xff;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;
                // int avg = (r+g+b)/3;

                if (f % 3 == 0) {
                    g = 0;
                    b = 0;
                    r = message.charAt(f) == '1' ? r : r / 6;
                } else if (f % 3 == 1) {
                    r = 0;
                    b = 0;
                    g = message.charAt(f) == '1' ? g : g / 6;
                } else if (f % 3 == 2) {
                    g = 0;
                    r = 0;
                    b = message.charAt(f) == '1' ? b : b / 6;
                }

                f++;
                p = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, p);

            }
        }
        // System.out.println("Finished modifying.");
    }

    private static void encodeDataContinuous(String size) {
        // Pad to 5 bits if necessary
        while (size.length() < 5)
            size = "0" + size;
        int f = 0;
        for (; y < height; y++) {
            for (; x < width; x++) {
                int p = image.getRGB(x, y);
                int a = p >>> 24;
                int r = p >>> 16;
                int g = p >>> 8;
                int b = p & 0xff;

                r = size.charAt(f) == '1' ? r : r / 6;
                g = 0;
                b = 0;

                f++;
                p = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, p);
                if (f == size.length())
                    return;
            }
            x = 0;
        }
    }

    private static void encodeDataHelper(String[] message, byte[] key) {
        for (int i = 0; i < chr_count; i++) {
            String charBin = message[i];
            String lenBits = Integer.toBinaryString(charBin.length());
            // Pad to 5 bits
            while (lenBits.length() < 5)
                lenBits = "0" + lenBits;
            System.out.println("Helper ran");
            System.out.println("Sent len : " + lenBits);
            encodeDataContinuous(lenBits);

            System.out.println("Sent Message : " + charBin);
            encodeDataWithKey(charBin, key);
        }
        printOutput();
    }

    private static void keyGen(int key_length, byte[] key_ar) {

        System.out.println("Enter " + key_length * 2 + " size key (numbers only).");

        String key_ = "";
        while (true) {
            // key_ = ob.next();
            key_ = "2222222222";
            try {
                for (int i = 0; i < key_.length(); i++) {
                    if (!Character.isDigit(key_.charAt(i)))
                        throw new Exception("Key can contain only numbers");
                }

                if (key_.length() != (key_length * 2)) {
                    System.out.println("Invalid key size. Enter key of size " + key_length * 2);
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println("An ERROR occured.\n" + e);
                System.out.println("Try Again");
            }
        }

        for (int i = 0; i < key_.length(); i++)
            key_ar[i] = Byte.parseByte(key_.charAt(i) + "");
    }

    private static String toBinary(char letter) {
        String bin = Integer.toBinaryString(letter);
        if (bin.length() > 16) {
            bin = bin.substring(bin.length() - 16); // Should never happen for char, but just in case
        }
        return bin;
    }

    private static void load() throws IOException {
        // load
        System.out.println("Loadig Image...");
        try {

            File input = new File(
                    System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.jpg");
            ext = ".jpg";
            if (!(input.exists())) {
                input = new File(
                        System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.jpeg");
                ext = ".jpeg";
            }
            if (!(input.exists())) {
                input = new File(
                        System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.png");
                ext = ".png";
            }
            if (!(input.exists())) {
                input = new File(
                        System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.gif");
                ext = ".gif";
            }
            if (!(input.exists())) {
                System.out.println(
                        "No image file with name \"image\" found.\nNOTE: Only JPG/JPEG, PNG and GIF is supported.");
                System.exit(1);
            }
            // File input = new File(System.getProperty("user.home") + File.separator +
            // "Desktop" + File.separator + "image.jpg");
            // File input = new File(System.getProperty("user.dir") + File.separator +
            // "image.jpg");
            image = ImageIO.read(input);
            System.out.println("\nColour Model : " + image.getColorModel() + "\n");
            width = image.getWidth();
            height = image.getHeight();
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
        System.out.println("Image loaded.");
    }

    private static void printOutput() {
        // image output sample
        try {
            File output_file = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator
                    + "imageCOMPLETE" + ext);
            ImageIO.write(image, "png", output_file);
            System.out.println("Image printed.");
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

    private static String[] message() {
        Scanner ob = new Scanner(System.in);
        System.out.println("Enter your message : ");
        String msg = ob.nextLine();
        char[] msg_characters = msg.toCharArray();
        chr_count = msg_characters.length;
        String[] msg_characters_bin = new String[chr_count];

        msg_bins_length = 0;
        for (int i = 0; i < chr_count; i++) {
            msg_characters_bin[i] = toBinary(msg_characters[i]);
            msg_bins_length += msg_characters_bin[i].length();
        }
        ob.close();
        return msg_characters_bin;
    }

    private static void initialise() {
        // initialise
        ext = "";
        image = null;
        width = 0;
        height = 0;
        msg_bins_length = 0;
        chr_count = 0;
        x = 0;
        y = 0;
        kc = 0;
        // initialised
    }
}