class test15{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;
    int k;

    public int start(){

	i = 4;
	k = 0;

	return this.mutual1();
    }

    public int mutual1(){

	int j;

	i = i - 1;

	if (i < 0)
		k = 0;
	else {
		System.out.println(k);
		k = 1;
		j = this.mutual2();
	}

	return k;

    }

    public int mutual2(){

	int j;

	i = i - 1;

	if (i < 0)
		k = 0;
	else {
		System.out.println(k);
		k = 0;
		j = this.mutual1();
	}

	return k;
    }
}
