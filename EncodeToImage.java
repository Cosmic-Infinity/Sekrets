/*
 * Although the underlying encoding is UTF-8, the console input/output may not handle all Unicode characters properly.
 * Emojis or certain special characters might appear malformed. I tried the program with Hindi, with varying degree of success.
 * The program works though, no information is lost, it's just a visual limitation of the console.
 */

import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.NoSuchElementException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class EncodeToImage {
    private static final int MAX_LOCK_RETRIES = 3;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Scanner INPUT = new Scanner(System.in);

    private static BufferedImage image;
    private static int imageWidth;
    private static int imageHeight;
    private static int payloadByteLength;
    private static int cursorX, cursorY, keyCursor;
    private static String imageExtension;

    public static void main(String[] args) {
        try {
            initialise();
            loadImage();

            byte[] payloadBytes = readMessageBytes();
            payloadByteLength = payloadBytes.length;

            if (!canFitWithoutKey(payloadByteLength)) {
                System.out.println("The message is too large for this image even with the simplest key. Use a larger image or a smaller message.");
                return;
            }

            byte[] key = chooseKey(payloadBytes);
            if (key == null) {
                return;
            }

            encodeMessage(payloadBytes, key);
            writeOutputImage();
            System.out.println("Encoding complete.");
        } catch (IOException e) {
            System.out.println("IOException in main: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception in main: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            INPUT.close();
        }
    }

    private static void initialise() {
        imageExtension = "";
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
        String desktopPath = SekretsUtil.getDesktopPath();
        File input = new File(desktopPath + File.separator + "image.jpg");
        if (!input.exists()) {
            input = new File(desktopPath + File.separator + "image.jpeg");
        }
        if (!input.exists()) {
            input = new File(desktopPath + File.separator + "image.png");
        }
        if (!input.exists()) {
            input = new File(desktopPath + File.separator + "image.gif");
        }
        if (!input.exists()) {
            System.out.println(
                    "No image file with name \"image\" found.\nNOTE: Only JPG/JPEG, PNG and GIF is supported.");
            throw new IOException("Image file not found");
        }

        // Extract extension from the found file
        String filename = input.getName();
        int dotIndex = filename.lastIndexOf('.');
        imageExtension = (dotIndex > 0) ? filename.substring(dotIndex + 1).toLowerCase() : "png";

        image = ImageIO.read(input);
        if (image == null) {
            throw new IOException("Failed to read image: " + input.getName());
        }

        if (SekretsUtil.DEBUG_MODE != 0) {
            System.out.println("\nColour Model : " + image.getColorModel() + "\n");
        }
        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        System.out.println("Image loaded.");
    }

    private static byte[] readMessageBytes() {
        logMajor("Enter your message : ");
        String messageText = INPUT.nextLine();
        return messageText.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] chooseKey(byte[] payloadBytes) {
        while (true) {
            logMajor("Would you like to input a key? (y/n): ");
            String answer = INPUT.nextLine().trim();

            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return promptForCompatibleKey(payloadBytes);
            }

            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
                return generateCompatibleAutoKey(payloadBytes);
            }

            System.out.println("Please answer yes or no.");
        }
    }

    private static byte[] promptForCompatibleKey(byte[] payloadBytes) {
        while (true) {
            byte[] key = promptForKey();
            if (isKeyCompatible(key, payloadBytes.length)) {
                logMajor("Using user-provided key: " + SekretsUtil.formatKey(key));
                return key;
            }
            System.out.println("This key is incompatible for the image and message. Try another key, use a smaller message, or use a larger image.");
        }
    }

    private static byte[] promptForKey() {
        while (true) {
            System.out.println("Enter a " + SekretsUtil.KEY_DIGIT_COUNT + " digit key (numbers only).");
            String keyText = INPUT.nextLine().trim();

            if (!SekretsUtil.isValidKeyText(keyText)) {
                System.out.println("Invalid key size or format. Enter exactly " + SekretsUtil.KEY_DIGIT_COUNT
                        + " digits, and make sure at least one run length is greater than zero.");
                continue;
            }

            return SekretsUtil.parseKey(keyText);
        }
    }

    private static byte[] generateCompatibleAutoKey(byte[] payloadBytes) {
        byte[] key = SekretsUtil.generateRandomKey();
        if (!isKeyCompatible(key, payloadBytes.length)) {
            key = SekretsUtil.buildMinimalKey();
        }

        logMajor("Generated key: " + SekretsUtil.formatKey(key));
        return key;
    }

    // generateRandomKey() and buildMinimalKey() are delegated to SekretsUtil

    private static boolean canFitWithoutKey(int payloadByteLength) {
        long requiredBits = SekretsUtil.HEADER_BIT_COUNT + (long) payloadByteLength * 8L;
        long availableBits = (long) imageWidth * imageHeight;
        return availableBits >= requiredBits;
    }

    private static boolean isKeyCompatible(byte[] key, int payloadByteLength) {
        if (!canFitWithoutKey(payloadByteLength)) {
            return false;
        }

        int payloadBitCount = payloadByteLength * 8;
        int simulationX = SekretsUtil.HEADER_BIT_COUNT % imageWidth;
        int simulationY = SekretsUtil.HEADER_BIT_COUNT / imageWidth;
        int simulationKeyCursor = 0;
        int currentRunLength = 0;
        int lastPixelIndex = -1;
        HashSet<Integer> visitedPixels = new HashSet<Integer>();
        byte[] workingKey = new byte[key.length];
        byte[] originalKey = new byte[key.length];
        System.arraycopy(key, 0, workingKey, 0, key.length);
        System.arraycopy(key, 0, originalKey, 0, key.length);

        for (int bitIndex = 0; bitIndex < payloadBitCount; bitIndex++) {
            while (workingKey[simulationKeyCursor + 1] == 0) {
                simulationKeyCursor += 2;
                if (simulationKeyCursor >= workingKey.length) {
                    System.arraycopy(originalKey, 0, workingKey, 0, workingKey.length);
                    simulationKeyCursor = 0;
                    break;
                }
            }

            workingKey[simulationKeyCursor + 1]--;

            simulationX += workingKey[simulationKeyCursor];
            if (simulationX >= imageWidth) {
                simulationY += simulationX / imageWidth;
                simulationX = simulationX % imageWidth;
                if (simulationY >= imageHeight) {
                    return false;
                }
            }

            int pixelIndex = simulationY * imageWidth + simulationX;
            if (pixelIndex != lastPixelIndex) {
                currentRunLength = 0;
                if (visitedPixels.contains(pixelIndex)) {
                    return false;
                }
                visitedPixels.add(pixelIndex);
                lastPixelIndex = pixelIndex;
            }

            currentRunLength++;
            if (currentRunLength > 3) {
                return false;
            }
        }

        return true;
    }

    // formatKey() is delegated to SekretsUtil

    private static void encodeMessage(byte[] payloadBytes, byte[] key) {
        int obfuscatedHeader = payloadBytes.length ^ SekretsUtil.deriveHeaderMask(key);
        String payloadLengthBits = toFixedBinary(obfuscatedHeader, SekretsUtil.HEADER_BIT_COUNT);
        logVerbose("Helper ran");
        logVerbose("Sent count: " + payloadLengthBits);
        writeSequentialBits(payloadLengthBits);

        logVerbose("Sent Message : " + new String(payloadBytes, StandardCharsets.UTF_8));
        writeKeyedPayload(payloadBytes, key);
    }

    private static void writeSequentialBits(String bits) {
        for (int bitIndex = 0; bitIndex < bits.length(); bitIndex++) {
            if (cursorY >= imageHeight) {
                return;
            }

            int pixel = image.getRGB(cursorX, cursorY);
            int alpha = (pixel >> 24) & 0xff;
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = pixel & 0xff;

                boolean bitIsOne = bits.charAt(bitIndex) == '1';
                pixel = applyDebugStyle(alpha, red, green, blue, 0, bitIsOne);
            image.setRGB(cursorX, cursorY, pixel);

            advanceSequentialCursor();
        }
    }

    private static void advanceSequentialCursor() {
        cursorX++;
        if (cursorX >= imageWidth) {
            cursorX = 0;
            cursorY++;
        }
    }

    private static void writeKeyedPayload(byte[] payloadBytes, byte[] key) {
        int payloadBitIndex = 0;
        int payloadBitCount = payloadBytes.length * 8;
        byte[] originalKey = new byte[key.length];
        System.arraycopy(key, 0, originalKey, 0, key.length);

        while (cursorY < imageHeight) {
            while (cursorX < imageWidth) {
                if (payloadBitIndex == payloadBitCount) {
                    return;
                }

                int[] keyCursorRef = { keyCursor };
                SekretsUtil.advanceKeyState(key, originalKey, keyCursorRef);
                keyCursor = keyCursorRef[0];
                cursorX += key[keyCursor];
                if (cursorX >= imageWidth) {
                    int totalPixels = (int) (cursorX % imageWidth);
                    cursorY += cursorX / imageWidth;
                    cursorX = totalPixels;
                    if (cursorY >= imageHeight) {
                        System.out.println("Image is too small.");
                        return;
                    }
                }

                int pixel = image.getRGB(cursorX, cursorY);
                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                boolean bitIsOne = isPayloadBitOne(payloadBytes, payloadBitIndex);
                int channelIndex = payloadBitIndex % 3;
                pixel = applyDebugStyle(alpha, red, green, blue, channelIndex, bitIsOne);
                image.setRGB(cursorX, cursorY, pixel);
                payloadBitIndex++;
            }
            cursorX = 0;
        }
    }


    private static boolean isPayloadBitOne(byte[] payloadBytes, int payloadBitIndex) {
        int byteIndex = payloadBitIndex / 8;
        int bitOffset = 7 - (payloadBitIndex % 8);
        return (((payloadBytes[byteIndex] & 0xff) >> bitOffset) & 1) == 1;
    }

    private static String toFixedBinary(int value, int bitCount) {
        String bits = Integer.toBinaryString(value);
        while (bits.length() < bitCount) {
            bits = "0" + bits;
        }
        if (bits.length() > bitCount) {
            bits = bits.substring(bits.length() - bitCount);
        }
        return bits;
    }

    private static int forceParity(int value, boolean makeOdd) {
        boolean isOdd = (value & 1) == 1;
        if (isOdd == makeOdd) {
            return value;
        }

        if (value == 0) {
            return 1;
        }
        if (value == 255) {
            return 254;
        }
        return value + 1;
    }

    private static int applyDebugStyle(int alpha, int red, int green, int blue, int channelIndex, boolean bitIsOne) {
        if (SekretsUtil.DEBUG_MODE == 1) {
            return (alpha << 24) | (255 << 16) | (255 << 8) | 255;
        } else if (SekretsUtil.DEBUG_MODE == 2) {
            return (alpha << 24);
        } else if (SekretsUtil.DEBUG_MODE == 3) {
            int visibleValue = bitIsOne ? 255 : 64;
            if (channelIndex == 0) {
                red = visibleValue;
                green = 0;
                blue = 0;
            } else if (channelIndex == 1) {
                red = 0;
                green = visibleValue;
                blue = 0;
            } else {
                red = 0;
                green = 0;
                blue = visibleValue;
            }
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        if (channelIndex == 0) {
            red = forceParity(red, bitIsOne);
        } else if (channelIndex == 1) {
            green = forceParity(green, bitIsOne);
        } else {
            blue = forceParity(blue, bitIsOne);
        }

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static void logMajor(String message) {
        System.out.println(message);
    }

    private static void logVerbose(String message) {
        SekretsUtil.logVerbose(message, SekretsUtil.DEBUG_MODE);
    }

    private static void writeOutputImage() {
        String desktopPath = SekretsUtil.getDesktopPath();
        File fixedOutputFile = new File(desktopPath + File.separator + "imageCOMPLETE." + imageExtension);

        for (int retryCount = 0; retryCount < MAX_LOCK_RETRIES; retryCount++) {
            if (tryWriteImage(fixedOutputFile)) {
                System.out.println("Image printed.");
                System.out.println("Output file: " + fixedOutputFile.getName());
                return;
            }

            System.out.println("The output file appears to be in use. Close any viewer using it, then press Enter to try again.");
            waitForUserToCloseFile();
        }

        File fallbackOutputFile = buildRandomFallbackOutputFile();
        if (tryWriteImage(fallbackOutputFile)) {
            System.out.println("Image printed.");
            System.out.println("Output file: " + fallbackOutputFile.getName());
            return;
        }

        System.out.println("Error: Unable to write the encoded image.");
    }

    private static boolean tryWriteImage(File outputFile) {
        try {
            ImageIO.write(image, imageExtension, outputFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void waitForUserToCloseFile() {
        try {
            INPUT.nextLine();
        } catch (NoSuchElementException e) {
            System.out.println("Input is not available in this terminal. Retrying automatically.");
        }
    }

    private static File buildRandomFallbackOutputFile() {
        int randomSuffix = RANDOM.nextInt(1000);
        String suffixText = String.format("%03d", randomSuffix);
        String desktopPath = SekretsUtil.getDesktopPath();
        return new File(desktopPath + File.separator + "imageCOMPLETE-" + suffixText + "." + imageExtension);
    }
}
