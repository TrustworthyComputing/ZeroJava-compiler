class ManyClasses {
    public static void main(String[] x) {
        boolean rv;
        A a;
        B b;
        C c;
        b = new B();
        c = new C();
        rv = b.set();
        rv = c.reset();
        System.out.println(b.get());
        System.out.println(c.get());
    }
}

class A {
    boolean data;

    public int get(){
        int rv;
        if(data){
            rv = 1;
        }
        else{
            rv = 0;
        }
        return rv;
    }
}

class B extends A {
    public boolean set() {
        boolean old;
        old = data;
        data = true;
        return data;
    }
}

class C extends B {
    public boolean reset() {
        boolean old;
        old = data;
        data = false;
        return data;
    }
}
