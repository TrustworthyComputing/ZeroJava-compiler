class FieldAndClassConflict {

    public static void main(String[] args){ 
	System.out.println(new A().B());
    }

}



class A {

    A A;

    public int B(){
	return 1;
    }
}

class B {

    B A;

}