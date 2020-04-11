class SimpleIf {

    public static void main(String [] a) {
        int y;
        if ((10 < 100) && (3 < 5)) {
            y = 55;
        } else {
            y = 22;
        }
        System.out.println(y);

        if ((10 < 100) && (5 < 3)) {
            y = 55;
        } else {
            y = 22;
        }
        System.out.println(y);
    }

}
