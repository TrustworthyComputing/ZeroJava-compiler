class CallFromSuper {

    public static void main(String[] args){
        B b;
        int rv;
        b = new B();
        rv = b.foo();
        System.out.println(rv);
    }

}


class A {

    public int foo(){
        return 1;
    }

}


class B extends A {


}
