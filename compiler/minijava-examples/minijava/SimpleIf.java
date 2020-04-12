class SimpleIf {

    public static void main(String [] a) {
        if (3 < 5) {
            System.out.println(1);
        }

        if (3 <= 5) {
            System.out.println(2);
        }

        if (13 != 15) {
            System.out.println(3);
        }

        if (23 > 1) {
            System.out.println(4);
        }

        if (23 >= 23) {
            System.out.println(5);
        }

        if ((10 < 100) && (3 != 15)) {
            System.out.println(6);
        } else {
            System.out.println(0);
        }

        if ((10 < 100) && (15 < 3)) {
            System.out.println(0);
        } else {
            System.out.println(7);
        }

        if ((10 < 100) || (15 < 3)) {
            System.out.println(8);
        } else {
            System.out.println(0);
        }

        if ((100 < 10) || (15 < 3)) {
            System.out.println(0);
        } else {
            System.out.println(9);
        }

        if ((100 < 10) || (15 == 15)) {
            System.out.println(10);
        } else {
            System.out.println(0);
        }

        Prover.answer(0);
    }

}
