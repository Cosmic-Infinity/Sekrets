import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class DecodeFromImage {

    private static final Scanner INPUT = new Scanner(System.in);

    private static BufferedImage image;
    private static int imageWidth;
    private static int imageHeight;
    private static int payloadByteLength;
    private static int cursorX;
    private static int cursorY;
    private static int keyCursor;

    public static void main(String[] args) throws IOException {
        try {
            initialise();
            loadImage();

            byte[] key = readKey();
            decodeMessage(key);
        } finally {
            INPUT.close();
        }
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
            if (SekretsUtil.DEBUG_MODE != 0) {
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
        File desktopFolder = new File(SekretsUtil.getDesktopPath());
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
            if (!SekretsUtil.isValidKeyText(keyText)) {
                System.out.println("Invalid key. Enter exactly " + SekretsUtil.KEY_DIGIT_COUNT
                        + " digits, and make sure at least one run length is greater than zero.");
                continue;
            }
            return SekretsUtil.parseKey(keyText);
        }
    }

    private static void decodeMessage(byte[] key) {
        byte[] originalKey = new byte[key.length];
        System.arraycopy(key, 0, originalKey, 0, key.length);

        StringBuilder headerBits = new StringBuilder();
        for (int bitIndex = 0; bitIndex < SekretsUtil.HEADER_BIT_COUNT; bitIndex++) {
            int bit = readSequentialBit();
            if (bit < 0) {
                System.out.println("Image ended while reading header bits.");
                System.out.println("No message could be decoded.");
                return;
            }
            headerBits.append(bit);
            logVerbose("Header bit " + bitIndex + " at (" + previousX() + "," + previousY() + "): " + bit);
        }

        int obfuscatedLength = (int) Long.parseLong(headerBits.toString(), 2);
        payloadByteLength = obfuscatedLength ^ SekretsUtil.deriveHeaderMask(key);
        System.out.println("Decoded payload byte length: " + payloadByteLength);

        byte[] payloadBytes = new byte[payloadByteLength];
        int payloadBitIndex = 0;
        int payloadBitCount = payloadByteLength * 8;

        while (cursorY < imageHeight) {
            while (cursorX < imageWidth) {
                if (payloadBitIndex == payloadBitCount) {
                    break;
                }

                int bit = readKeyedBit(key, originalKey, payloadBitIndex);
                if (bit < 0) {
                    System.out.println("Image ended while reading payload bits.");
                    System.out.println("No message could be decoded.");
                    return;
                }

                int byteIndex = payloadBitIndex / 8;
                int bitOffset = 7 - (payloadBitIndex % 8);
                payloadBytes[byteIndex] |= (byte) (bit << bitOffset);
                payloadBitIndex++;
                logVerbose("Payload bit " + payloadBitIndex + " at (" + cursorX + "," + cursorY + "): " + bit);
            }

            if (payloadBitIndex == payloadBitCount) {
                break;
            }
            cursorX = 0;
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
        int[] keyCursorRef = { keyCursor };
        SekretsUtil.advanceKeyState(key, originalKey, keyCursorRef);
        keyCursor = keyCursorRef[0];

        cursorX += key[keyCursor];
        if (cursorX >= imageWidth) {
            int totalPixels = (int) (cursorX % imageWidth);
            cursorY += cursorX / imageWidth;
            cursorX = totalPixels;
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

    private static void logVerbose(String message) {
        SekretsUtil.logVerbose(message, SekretsUtil.DEBUG_MODE);
    }

    private static int previousX() {
        return cursorX == 0 ? imageWidth - 1 : cursorX - 1;
    }

    private static int previousY() {
        return cursorX == 0 ? cursorY - 1 : cursorY;
    }
}
