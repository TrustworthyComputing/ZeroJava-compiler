class ShadowField{

    public static void main(String[] args) { 
        A a;
        a = new A();
        a = a.foo();
        System.out.println(a.get());
    }

}


class A {

    int x;

    public A foo(){
        A x;
        x = new A();
        return x;
    }

    public int get(){
        return x;
    }

}
