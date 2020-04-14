class ShadowBaseField {

    public static void main(String[] args){
        A a;
        a = new A();
        System.out.println(a.getX());
        a = new B();
        System.out.println(a.getX());
    }

}


class A {

    int x;

    public int getX(){
        return x;
    }

}


class B extends A {

    int x;

    public int getX(){
        x = 1;
        return x;
    }

}
