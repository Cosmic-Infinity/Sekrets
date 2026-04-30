import java.security.SecureRandom;

/**
 * Shared utilities for Sekrets encoder/decoder.
 * Consolidates common constants, validation, and helper methods.
 */
public class SekretsUtil {
    // Constants
    public static final int KEY_DIGIT_COUNT = 10;
    public static final int KEY_PAIR_COUNT = KEY_DIGIT_COUNT / 2;
    public static final int HEADER_BIT_COUNT = 32;

    // Debug mode (0 = off, 1= white 2 = black, 3 = modified channel, with lighter
    // colour means 1, darker means 0)
    public static int DEBUG_MODE = 0;

    // Debug mode constants
    public static final int DEBUG_OFF = 0;
    public static final int DEBUG_WHITE = 1;
    public static final int DEBUG_BLACK = 2;
    public static final int DEBUG_CHANNEL = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    // Magic constants for header masking
    private static final int HEADER_MASK_BASE = 0x6d2b79f5;
    private static final int HEADER_MASK_ROTATE = 5;
    private static final int HEADER_MASK_INCREMENT = 0x9e3779b9;

    /**
     * Validates a key string: must be exactly KEY_DIGIT_COUNT digits,
     * all numeric, and at least one odd-positioned digit must be > 0.
     */
    public static boolean isValidKeyText(String keyText) {
        if (keyText.length() != KEY_DIGIT_COUNT) {
            return false;
        }

        boolean hasPositiveRunLength = false;
        for (int i = 0; i < keyText.length(); i++) {
            char digit = keyText.charAt(i);
            if (!Character.isDigit(digit)) {
                return false;
            }
            // Odd-positioned digits (1, 3, 5, ...) represent run lengths
            if ((i % 2 == 1) && digit > '0') {
                hasPositiveRunLength = true;
            }
        }

        return hasPositiveRunLength;
    }

    /**
     * Parses a string of digits into a byte array.
     */
    public static byte[] parseKey(String keyText) {
        byte[] key = new byte[keyText.length()];
        for (int i = 0; i < keyText.length(); i++) {
            key[i] = Byte.parseByte(keyText.charAt(i) + "");
        }
        return key;
    }

    /**
     * Generates a fully random key with varied skip and run-length values.
     * Each pair: skip (0-9) and run-length (1-9).
     */
    public static byte[] generateRandomKey() {
        byte[] key = new byte[KEY_DIGIT_COUNT];
        for (int pairIndex = 0; pairIndex < KEY_PAIR_COUNT; pairIndex++) {
            key[pairIndex * 2] = (byte) RANDOM.nextInt(10); // Skip: 0-9
            key[pairIndex * 2 + 1] = (byte) (RANDOM.nextInt(9) + 1); // Run-length: 1-9
        }
        return key;
    }

    /**
     * Generates the minimal key: skip=0, run-length=1 for all pairs.
     * Used as fallback when a random key is too complex for the image.
     */
    public static byte[] buildMinimalKey() {
        byte[] key = new byte[KEY_DIGIT_COUNT];
        for (int pairIndex = 0; pairIndex < KEY_PAIR_COUNT; pairIndex++) {
            key[pairIndex * 2] = 0;
            key[pairIndex * 2 + 1] = 1;
        }
        return key;
    }

    /**
     * Formats a key byte array as a readable string.
     */
    public static String formatKey(byte[] key) {
        StringBuilder sb = new StringBuilder();
        for (byte digit : key) {
            sb.append(digit);
        }
        return sb.toString();
    }

    /**
     * Derives the obfuscation mask for the message length header.
     * Uses a deterministic function of the key.
     */
    public static int deriveHeaderMask(byte[] key) {
        int mask = HEADER_MASK_BASE;
        for (byte digit : key) {
            mask = Integer.rotateLeft(mask ^ (digit & 0xff), HEADER_MASK_ROTATE) + HEADER_MASK_INCREMENT;
        }
        return mask;
    }

    /**
     * Advances the key cursor, cycling through skip/run-length pairs.
     * Mutates the key array in-place by decrementing run-length values.
     */
    public static void advanceKeyState(byte[] key, byte[] originalKey, int[] keyCursorRef) {
        int keyCursor = keyCursorRef[0];
        while (key[keyCursor + 1] == 0) {
            keyCursor += 2;
            if (keyCursor >= key.length) {
                System.arraycopy(originalKey, 0, key, 0, key.length);
                keyCursor = 0;
                break;
            }
        }
        key[keyCursor + 1]--;
        keyCursorRef[0] = keyCursor;
    }

    /**
     * Logs a message only if debug mode is enabled.
     */
    public static void logVerbose(String message, int debugMode) {
        if (debugMode != DEBUG_OFF) {
            System.out.println(message);
        }
    }

    /**
     * Extracts Desktop path for image files.
     */
    public static String getDesktopPath() {
        return System.getProperty("user.home") + java.io.File.separator + "Desktop";
    }
}
