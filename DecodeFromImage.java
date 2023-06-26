
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

    private static void decodeData(byte[] key){

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

            File input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.jpg");
            ext = ".jpg";
            if(!(input.exists())){
                input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.jpeg");
                ext = ".jpeg";
            }
            if(!(input.exists())){
                input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.png");
                ext = ".png";
            }
            if(!(input.exists())){
                input = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "image.gif");
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