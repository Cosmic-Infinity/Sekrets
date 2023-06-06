class DecodeFromImage{
    public static void main(){

    }
    
    
    public static char FromBinary(long number){
        int num = 0;
        for(int i = 0; number>0; number=number/10, i++){
            num = (int)(Math.pow(2,i)*(number%10))+num;
        }
        return (char)num;
    }
}