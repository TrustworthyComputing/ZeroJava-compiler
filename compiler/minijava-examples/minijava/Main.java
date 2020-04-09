
class Main {

    public static void main(String[] a){
        ArrayTest ab;
        ab=new ArrayTest ();
        System.out.println(ab.test(3));
    }

}
class ArrayTest {
	int weee;
    int wre;
    int aaa;
    public int test(int num){
        int i;
        int[] intArray;
        intArray = new int[num];
        aaa=0;
        System.out.println(aaa);
        System.out.println(intArray.length);
        i = 0;
        System.out.println(111);
        while (i < (intArray.length)) {
            System.out.println(i + 1);
            intArray[i] = i + 1;
            i = i + 1;
        }
        System.out.println(222);
        i = 0;
        while (i < (intArray.length)) {
            System.out.println(intArray[i]);
            i = i + 1;
        }
        System.out.println(333);
        return intArray.length;
    }
}
class B extends ArrayTest {
    int aaa;
    int we;

    public int test(int num){
        int i;
        int[] intArray;
        intArray = new int[num];
        aaa=12;
        System.out.println(aaa);
        System.out.println(intArray.length);
        i = 0;
        System.out.println(111);
        while (i < (intArray.length)) {
            System.out.println(i + 1);
            intArray[i] = i + 1;
            i = i + 1;
        }
        System.out.println(222);
        i = 0;
        while (i < (intArray.length)) {
            System.out.println(intArray[i]);
            i = i + 1;
        }
        System.out.println(333);
        return intArray.length;
    }
}
