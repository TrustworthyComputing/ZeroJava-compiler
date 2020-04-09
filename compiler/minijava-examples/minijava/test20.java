// test20: more inheritance
//
// correct output: 999

class test20 {
    public static void main(String[] args) {
        System.out.println(new C23().init(new B23()));
    }
}

class A23 {
    int i1;
    int i2;
    int i3;

    public int init(A23 a) {
        i2 = a.getI1();
        i3 = 222;
        i1 = this.setI1(i2 + i3);
        return i1;
    }

    public int getI1() {
        return i1;
    }

    public int setI1(int i) {
        return i;
    }
}

class B23 extends A23 {
    int i1;
    int i4;

    public int init(A23 a) {
        A23 a_local;
        a_local = new A23();
        i4 = a.getI1();
        i1 = this.setI1(i4);
        return a_local.init(this);
    }

    public int getI1() {
        return i1;
    }

    public int setI1(int i) {
        return i + 111;
    }
}

class C23 extends B23 {
    int i1;
    int i5;

    public int init(A23 a) {
        i5 = 333;
        i1 = this.setI1(i5);
        return a.init(this);
    }

    public int getI1() {
        return i1;
    }

    public int setI1(int i) {
        return i*2;
    }
}
