class Main {
	public static void main(String[] a) {}
}

class Base {
	int data;
	public int get() {
		return data;
	}
}

class Derived extends Base {
	public int get(int x) {
		return x;
	}
}
