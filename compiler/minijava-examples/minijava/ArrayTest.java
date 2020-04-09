class ArrayTest{
    public static void main(String[] a){
        boolean n;
        n = new Test().start(10);
    }
}

class Test {

	public boolean start(int sz){
		int[] b;
		int l;
		int i;
		b = new int[sz];
		l = b.length;
		i = 0;
		while(i < (l)){
			b[i] = i;
			System.out.println(b[i]);
			i = i + 1;
		}
		return true;
	}

}
