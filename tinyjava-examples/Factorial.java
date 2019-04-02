class Factorial{
    public static void main(String[] a){
        System.out.println(111111111);
        System.out.println(new Fac().ComputeFac(10));
        System.out.println(222222222);
    }
}

class Fac {
    public int ComputeFac(int num){
        int num_aux ;
        if (num < 1)
            num_aux = 1 ;
        else
            num_aux = num * (this.ComputeFac(num-1)) ;
        return num_aux ;
    }
}
