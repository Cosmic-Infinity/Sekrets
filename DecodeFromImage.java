import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class DecodeFromImage{
    private static String ext; //stores image extensiom
    private static BufferedImage image; //stores image
    private static int width; //stores image width
    private static int height; //stores image height
    private static int msg_bin_length; //stores the length of binary equivalent of the string
    private static int msg_count; //stores the number of characters in the word 

    public static void main() throws IOException{

        //initialise
        // there is no upper limit on the length of the key. But if the key is too long and the message too short, some digits of the key will go unused

        ext = "";
        image = null;
        width = 0;
        height = 0;
        msg_bin_length = 0;
        msg_count = 0;
        //initialised

        load(); //loading image
        byte[] key = keyInput(); //calling method which takes key input from user
        int key_length = key.length % 2;

        //String[] msg = message(); //the binary of each character in message is stored in the cells of String array msg[] 
        /*if(! test(key)){
        System.out.println("This key is incompatible for the message. Try another key, or a different image.");
        }

        encodeDataHelper(msg, key);*/

        decodeData(key);
    }

    private static void decodeData(byte[] key) {
        int x = 0, y = 0, kc = 0;
        StringBuilder decodedMessage = new StringBuilder();
        byte[] keyOriginal = new byte[key.length];
        System.arraycopy(key, 0, keyOriginal, 0, key.length);

        while (y < height) {
            // 1. Read 5 bits for the length of the next character's binary (continuous, red channel only, 1 if r==255, else 0)
            StringBuilder lenBin = new StringBuilder();
            int lenStartX = x, lenStartY = y;
            for (int i = 0; i < 5; i++) {
                if (y >= height) {
                    System.out.println("Image ended while reading length bits.");
                    break;
                }
                int p = image.getRGB(x, y);
                int r = (p >> 16) & 0xff;
                int bit = (r == 255) ? 1 : 0;
                lenBin.append(bit);
                System.out.println("Length bit " + i + " at (" + x + "," + y + "): " + bit + " (r=" + r + ")");
                x++;
                if (x >= width) {
                    x = 0;
                    y++;
                }
            }
            System.out.println("Read length bits from (" + lenStartX + "," + lenStartY + ") to (" + x + "," + y + "): " + lenBin.toString());
            if (lenBin.length() < 5) break;
            int charBinLen = Integer.parseInt(lenBin.toString(), 2);
            System.out.println("Decoded length: " + charBinLen);
            if (charBinLen == 0 || charBinLen > 16) break;

            // 2. Read the actual character's binary using the key (R/G/B, 1 if channel==255, else 0)
            StringBuilder charBin = new StringBuilder();
            int dataStartX = x, dataStartY = y;
            while (charBin.length() < charBinLen && y < height) {
                // Key skipping logic
                while (key[kc + 1] == 0) {
                    kc += 2;
                    if (kc >= key.length) {
                        // Reset key to original state
                        System.arraycopy(keyOriginal, 0, key, 0, key.length);
                        kc = 0;
                        break;
                    }
                }
                key[kc + 1]--;

                x = x + key[kc];
                if (x >= width) {
                    x = x - key[kc];
                    int added_to_x = width - x;
                    y++;
                    x = x + (key[kc] - added_to_x);
                    if (y >= height) break;
                }

                int p = image.getRGB(x-1, y);
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;
                int pos = charBin.length() % 3;
                int bit;
                if (pos == 0) {
                    bit = (r == 255) ? 1 : 0;
                } else if (pos == 1) {
                    bit = (g == 255) ? 1 : 0;
                } else {
                    bit = (b == 255) ? 1 : 0;
                }
                charBin.append(bit);
                System.out.println("Data bit " + charBin.length() + " at (" + x + "," + y + "): " + bit + " (r=" + r + ",g=" + g + ",b=" + b + ")");
            }
            System.out.println("Read data bits from (" + dataStartX + "," + dataStartY + ") to (" + x + "," + y + "): " + charBin.toString());
            if (charBin.length() == charBinLen) {
                int charCode = Integer.parseInt(charBin.toString(), 2);
                System.out.println("Decoded char: '" + (char)charCode + "' (" + charCode + ")");
                decodedMessage.append((char)charCode);
            } else {
                System.out.println("Image ended while reading character bits.");
                break;
            }
        }
        if (decodedMessage.length() > 0) {
            System.out.println("Decoded message: " + decodedMessage.toString());
        } else {
            System.out.println("No message could be decoded.");
        }
    }

    private static char FromBinary(long number){
        int num = 0;
        for(int i = 0; number>0; number=number/10, i++){
            num = (int)(Math.pow(2,i)*(number%10))+num;
        }
        return (char)num;
    }

    private static void load()throws IOException{
        //load
        System.out.println("Loadig Image...");
        try {

            File input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "imageCOMPLETE.jpg");
            ext = ".jpg";
            if(!(input.exists())){
                input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "iimageCOMPLETE.jpeg");
                ext = ".jpeg";
            }
            if(!(input.exists())){
                input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "imageCOMPLETE.png");
                ext = ".png";
            }
            if(!(input.exists())){
                input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "imageCOMPLETE.gif");
                ext = ".gif";
            }if(!(input.exists())){
                System.out.println("No image file with name \"image\" found.\nNOTE: Only JPG/JPEG, PNG and GIF is supported.");
                System.exit(1);
            }
            //File input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.jpg");
            //File input = new File(System.getProperty("user.dir") + File.separator + "image.jpg");
            image = ImageIO.read(input);
            width = image.getWidth();
            height = image.getHeight();
        }
        catch (IOException e) {
            System.out.println("Error: " + e);
        }
        System.out.println("Image loaded.");
    }

    private static byte[] keyInput(){
        Scanner ob = new Scanner(System.in);
        System.out.println("Enter your key (numbers only).");

        String key_ = "";
        while(true){
            key_ = ob.next();
            try{
                for(int i=0; i<key_.length(); i++){
                    if(!Character.isDigit(key_.charAt(i)))
                        throw new Exception("Key can contain only numbers");
                }

                if(key_.length()%2 == 1){
                    System.out.println("Invalid key size. Try Again");
                    continue;
                }
                break;
            }catch(Exception e){
                System.out.println("An ERROR occured.\n"+e);
                System.out.println("Try Again");
            }
        }

        byte[] key_ar = new byte[key_.length()];

        for(int i=0; i<key_.length(); i++)
            key_ar[i] = Byte.parseByte(key_.charAt(i)+"");

        return(key_ar);
    }
}