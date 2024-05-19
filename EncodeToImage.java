/* Not all characters and languages are supported considering this is a console application.
 * ISO-8859-1 characters are supported in java, meaning all English and Latin characters can be worked with.
 * Emojis or certain special characters might not get read. I tried the program with Hindi, with varying degree of success.
 */

import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

class EncodeToImage{
    private static String ext; //stores image extensiom
    private static BufferedImage image; //stores image
    private static int width; //stores image width
    private static int height; //stores image height
    private static int msg_bin_length; //stores the length of binary equivalent of the string
    private static int msg_count; //stores the number of characters in the word 

    public static void main() throws IOException{

        
        initialise();
        load(); //loading image
        
        int key_length = 5; // there is no upper limit on the length of the key. But if the key is too long and the message too short, some digits of the key will go unused
        byte[] key = new byte[key_length*2];
        
        keyGen(key_length, key); //generating key, each digit of which which is stored as a byte array
        String[] msg = message(); //the binary of each character in message is stored in the cells of String array msg[] 
        
        System.out.println("Message binary :");
        for(int i=0; i<msg.length; i++)
            System.out.print(msg[i] + " ");
            
        System.out.println("\nKey Digits :");
        for(int i=0; i<key.length; i++)
            System.out.print(key[i] + " ");
        
        if(! test(key)){
            System.out.println("This key is incompatible for the message. Try another key, or a different image.");
        }

        encodeDataHelper(msg, key);

        //for(int i=0;i<msg.length;i++)
        //System.out.println(msg[i]);
        //for(int i=0;i<key.length; i++)
        //System.out.println(key[i]);
    }

    private static boolean test(byte[] key){
        
        return true;
        /*
        long sum = 0;
        for(int i=0; i<msg_bin_length; i++){
            sum=sum+ key[i%key.length]; // digit of the key is taken in sets of 2. the first digit says the number of pixels to skip. the second digit says how many times to skip the same number of pixels. after each skip the (message) data is encoded to either of the R G or B pixel of the image.
        }
        sum+=msg_count;
        
        return ((width*height) < (sum - 100)); // checking if the key and message length combination exceeds the amount of pixels available on the image 
        */
    }

    static int x = 0, y = 0;
    private static void encodeDataWithKey(String message, byte[] key){
        int f = 0, kc = 0;
        for( ; y<height; y++){
            for( ; x<width; x++){
                if(f == message.length()){
                    return;
                }                

                x = x+key[kc];
                if(x >= width){
                    x = x - key[kc];
                    int added_to_x = width - x;
                    y++;
                    x = x + (key[kc] - added_to_x);
                }
                key[kc+1]--;
                if(key[kc+1] == 0)
                    kc+=2;

                int p = image.getRGB(x,y);
                int a = (p>>24)&0xff;
                int r = (p>>16)&0xff;
                int g = (p>>8)&0xff;
                int b = p&0xff;
                //int avg = (r+g+b)/3;

                if(f%3 == 0){
                    g=0;b=0;
                    r = message.charAt(f) == '1'? r : r/6;
                }
                else if(f%3 == 1){
                    r=0;b=0;
                    g = message.charAt(f) == '1'? g : g/6;
                }
                else if(f%3 == 2){
                    g=0;r=0;
                    b = message.charAt(f) == '1'? b : b/6;
                }

                f++;
                p = (a<<24) | (r<<16) | (g<<8) | b;
                image.setRGB(x, y, p);
                
            }
        }
        //System.out.println("Finished modifying.");
    }

    private static void encodeDataContinuous(String size){
        int f = 0;
        for( ; y<height; y++){
            for( ; x<width; x++){
                
                int p = image.getRGB(x,y);
                int a = p>>>24;
                int r = p>>>16;
                int g = p>>>8;
                int b = p&0xff;
                //int avg = (r+g+b)/3;
                
                a = 100;
                r = size.charAt(f) == '1'? r : r/6;
                g = 0;
                b = 0;
                
                //System.out.println(message.charAt(f));
                
                f++;

                p = (a<<24) | (r<<16) | (g<<8) | b;
                image.setRGB(x, y, p);
                if(f == size.length()){
                    return;
                }
            }
        }
        //System.out.println("Finished modifying.");
    }

    private static void encodeDataHelper(String[] message, byte[] key){
        for(int f = 0; f < msg_count; f++){
            
            System.out.println("Sent len : "+toBinary((char)message[f].length()));
            encodeDataContinuous(toBinary((char)message[f].length()));
            
            System.out.println("Sent Message : "+message[f]);
            encodeDataWithKey(message[f], key);
        }

        printOutput();

    }

    

    private static void keyGen(int key_length, byte[] key_ar){
        Scanner ob = new Scanner(System.in);
        System.out.println("Enter "+key_length*2+" size key (numbers only).");

        String key_ = "";
        while(true){
            //key_ = ob.next();
            key_ = "2222222222";
            try{
                for(int i=0; i<key_.length(); i++){
                    if(!Character.isDigit(key_.charAt(i)))
                        throw new Exception("Key can contain only numbers");
                }

                if(key_.length()!=(key_length*2)){
                    System.out.println("Invalid key size. Enter key of size "+key_length*2);
                    continue;
                }
                break;
            }catch(Exception e){
                System.out.println("An ERROR occured.\n"+e);
                System.out.println("Try Again");
            }
        }

        for(int i=0; i<key_.length(); i++)
            key_ar[i] = Byte.parseByte(key_.charAt(i)+"");
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static String toBinary(char letter){
        String bin = "";
        for(int i = letter; i>0; i=i/2){
            bin = (i%2)+bin;
        }
        return bin;
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
            System.out.println("\nColour Model : "+image.getColorModel()+"\n");
            width = image.getWidth();
            height = image.getHeight();
        }
        catch (IOException e) {
            System.out.println("Error: " + e);
        }
        System.out.println("Image loaded.");
    }
    
    private static void printOutput(){
        //image output sample
        try {
            File output_file = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "imageCOMPLETE" + ext);
            ImageIO.write(image, "png", output_file);
            System.out.println("Image printed.");
        }
        catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }
    
    private static String[] message(){
        //takes message input
        //stores each character of the message to msg_char[] (temp)
        //stores charcacter count of message to msg_count
        //storess the binary of each character to 'String msg_char_bin[]' ->> msg_char_bin[index] = [0100001]
        
        Scanner ob = new Scanner(System.in);
        System.out.println("Enter your message : ");
        String msg = ob.nextLine();
        char[] msg_char = msg.toCharArray();
        /* */msg_count = msg_char.length;
        /* */String[] msg_char_bin = new String[msg_count];

        for(int i=0;i<msg_count;i++){
            msg_char_bin[i] = toBinary(msg_char[i]);
            msg_bin_length+= (msg_char_bin[i]+"").length();
        }

        return msg_char_bin;
    }
    
    private static void initialise(){
        //initialise
        ext = "";
        image = null;
        width = 0;
        height = 0;
        msg_bin_length = 0;
        msg_count = 0;
        x=0;
        y=0;
        //initialised
    }
}