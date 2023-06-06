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
    
    private static String ext;
    private static BufferedImage image;
    private static int width;
    private static int height;
    
    public static void main() throws IOException{
        int key_length = 5;
        byte[] key = new byte[key_length*2];
        String[] msg;

        keyGen(key_length, key);
        
        msg = message();
        
        load();
        
        
        
        //for(int i=0;i<msg.length;i++)
            //System.out.println(msg[i]);
        //for(int i=0;i<key.length; i++)
        //System.out.println(key[i]);
    }
    
    
    private static void encodeData(){
        //modifying pixel value
        System.out.println("Updating values");
        width = image.getWidth();
        height = image.getHeight();
        for(int y = 0; y<height; y++){
            for(int x = 0; x<width; x++){
                int p = image.getRGB(x,y);
                int a = (p>>24)&0xff;
                int r = (p>>16)&0xff;
                int g = (p>>8)&0xff;
                int b = p&0xff;
                int avg = (r+g+b)/3;
                p = (a<<24) | (r<<16) | (g<<8) | b;
                image.setRGB(x, y, p);
            }
        }
        System.out.println("Finished decolouring.");
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
    
    
    private static String[] message(){
        Scanner ob = new Scanner(System.in);
        System.out.println("Enter your message : ");
        String msg = ob.nextLine();
        char[] msg_char = msg.toCharArray();
        String[] msg_char_bin = new String[msg_char.length];

        for(int i=0;i<msg_char.length;i++)
            msg_char_bin[i] = toBinary(msg_char[i]);
            
        return msg_char_bin;
    }

    private static String toBinary(char letter){
        String bin = "";
        for(int i = letter; i>0; i=i/2){
            bin = (i%2)+bin;
        }
        return bin;
    }
    
    
    private static void keyGen(int key_length, byte[] key_ar){
        Scanner ob = new Scanner(System.in);
        System.out.println("Enter "+key_length*2+" size key (numbers only).");
        
        String key_ = "";
        while(true){
            key_ = ob.next();
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
}