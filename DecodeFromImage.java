import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class DecodeFromImage {
    private static final int KEY_DIGIT_COUNT = 10;
    private static final int HEADER_BIT_COUNT = 32;
    private static final int DEBUG_OFF = 0;
    private static final int DEBUG_WHITE = 1;
    private static final int DEBUG_BLACK = 2;
    private static final int DEBUG_CHANNEL = 3;
    private static final int DEBUG_MODE = 3;

    private static final Scanner INPUT = new Scanner(System.in);

    private static BufferedImage image;
    private static int imageWidth;
    private static int imageHeight;
    private static int payloadByteLength;
    private static int cursorX;
    private static int cursorY;
    private static int keyCursor;

    public static void main(String[] args) throws IOException {
        initialise();
        loadImage();

        byte[] key = readKey();
        decodeMessage(key);
    }

    private static void initialise() {
        image = null;
        imageWidth = 0;
        imageHeight = 0;
        payloadByteLength = 0;
        cursorX = 0;
        cursorY = 0;
        keyCursor = 0;
    }

    private static void loadImage() throws IOException {
        System.out.println("Loading image...");
        try {
            File input = selectNewestEncodedImage();
            if (input == null) {
                System.out.println("No encoded image found. NOTE: Only JPG/JPEG, PNG and GIF is supported.");
                System.exit(1);
            }

            image = ImageIO.read(input);
            if (DEBUG_MODE != DEBUG_OFF) {
                System.out.println("\nColour Model : " + image.getColorModel() + "\n");
            }
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
        System.out.println("Image loaded.");
    }

    private static File selectNewestEncodedImage() {
        File desktopFolder = new File(System.getProperty("user.home") + File.separator + "Desktop");
        File newestFile = null;
        long newestModifiedTime = Long.MIN_VALUE;
        File[] files = desktopFolder.listFiles();
        if (files == null) {
            return null;
        }

        for (File candidate : files) {
            String fileName = candidate.getName().toLowerCase();
            boolean isEncodedImage = fileName.startsWith("imagecomplete")
                && (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".gif"));
            if (candidate.exists() && isEncodedImage && candidate.lastModified() > newestModifiedTime) {
                newestFile = candidate;
                newestModifiedTime = candidate.lastModified();
            }
        }

        return newestFile;
    }

    private static byte[] readKey() {
        while (true) {
            System.out.println("Enter your key (numbers only).");
            String keyText = INPUT.nextLine().trim();
            if (!isValidKeyText(keyText)) {
                System.out.println("Invalid key. Enter exactly " + KEY_DIGIT_COUNT + " digits, and make sure at least one run length is greater than zero.");
                continue;
            }
            return parseKey(keyText);
        }
    }

    private static boolean isValidKeyText(String keyText) {
        if (keyText.length() != KEY_DIGIT_COUNT) {
            return false;
        }

        boolean hasPositiveRunLength = false;
        for (int i = 0; i < keyText.length(); i++) {
            char digit = keyText.charAt(i);
            if (!Character.isDigit(digit)) {
                return false;
            }
            if ((i % 2 == 1) && digit > '0') {
                hasPositiveRunLength = true;
            }
        }

        return hasPositiveRunLength;
    }

    private static byte[] parseKey(String keyText) {
        byte[] key = new byte[keyText.length()];
        for (int i = 0; i < keyText.length(); i++) {
            key[i] = Byte.parseByte(keyText.charAt(i) + "");
        }
        return key;
    }

    private static void decodeMessage(byte[] key) {
        byte[] originalKey = new byte[key.length];
        System.arraycopy(key, 0, originalKey, 0, key.length);

        StringBuilder headerBits = new StringBuilder();
        for (int bitIndex = 0; bitIndex < HEADER_BIT_COUNT; bitIndex++) {
            int bit = readSequentialBit();
            if (bit < 0) {
                System.out.println("Image ended while reading header bits.");
                System.out.println("No message could be decoded.");
                return;
            }
            headerBits.append(bit);
            logVerbose("Header bit " + bitIndex + " at (" + previousX() + "," + previousY() + "): " + bit);
        }

        int obfuscatedLength = Integer.parseInt(headerBits.toString(), 2);
        payloadByteLength = obfuscatedLength ^ deriveHeaderMask(key);
        System.out.println("Decoded payload byte length: " + payloadByteLength);

        byte[] payloadBytes = new byte[payloadByteLength];
        int payloadBitIndex = 0;
        for (int byteIndex = 0; byteIndex < payloadByteLength; byteIndex++) {
            int currentByte = 0;
            for (int bitInByte = 0; bitInByte < 8; bitInByte++) {
                int bit = readKeyedBit(key, originalKey, payloadBitIndex);
                if (bit < 0) {
                    System.out.println("Image ended while reading payload bits.");
                    System.out.println("No message could be decoded.");
                    return;
                }
                currentByte = (currentByte << 1) | bit;
                payloadBitIndex++;
                logVerbose("Payload bit " + payloadBitIndex + " at (" + cursorX + "," + cursorY + "): " + bit);
            }
            payloadBytes[byteIndex] = (byte) currentByte;
        }

        String decodedMessage = new String(payloadBytes, StandardCharsets.UTF_8);
        System.out.println("Decoded message: " + decodedMessage);
    }

    private static int readSequentialBit() {
        if (cursorY >= imageHeight) {
            return -1;
        }

        int pixel = image.getRGB(cursorX, cursorY);
        int red = (pixel >> 16) & 0xff;
        int bit = red & 1;
        advanceSequentialCursor();
        return bit;
    }

    private static void advanceSequentialCursor() {
        cursorX++;
        if (cursorX >= imageWidth) {
            cursorX = 0;
            cursorY++;
        }
    }

    private static int readKeyedBit(byte[] key, byte[] originalKey, int payloadBitIndex) {
        advanceKeyState(key, originalKey);

        cursorX += key[keyCursor];
        if (cursorX >= imageWidth) {
            cursorX -= key[keyCursor];
            int pixelsLeftInRow = imageWidth - cursorX;
            cursorY++;
            cursorX += (key[keyCursor] - pixelsLeftInRow);
            if (cursorY >= imageHeight) {
                return -1;
            }
        }

        int pixel = image.getRGB(cursorX, cursorY);
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = pixel & 0xff;

        int channelIndex = payloadBitIndex % 3;
        if (channelIndex == 0) {
            return red & 1;
        } else if (channelIndex == 1) {
            return green & 1;
        }
        return blue & 1;
    }

    private static void advanceKeyState(byte[] key, byte[] originalKey) {
        while (key[keyCursor + 1] == 0) {
            keyCursor += 2;
            if (keyCursor >= key.length) {
                System.arraycopy(originalKey, 0, key, 0, key.length);
                keyCursor = 0;
                break;
            }
        }
        key[keyCursor + 1]--;
    }

    private static int deriveHeaderMask(byte[] key) {
        int mask = 0x6d2b79f5;
        for (byte digit : key) {
            mask = Integer.rotateLeft(mask ^ (digit & 0xff), 5) + 0x9e3779b9;
        }
        return mask;
    }

    private static void logVerbose(String message) {
        if (DEBUG_MODE != DEBUG_OFF) {
            System.out.println(message);
        }
    }

    private static int previousX() {
        return cursorX == 0 ? imageWidth - 1 : cursorX - 1;
    }

    private static int previousY() {
        return cursorX == 0 ? cursorY - 1 : cursorY;
    }
}
