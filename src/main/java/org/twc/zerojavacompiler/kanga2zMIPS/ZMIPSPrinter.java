package org.twc.zerojavacompiler.kanga2zMIPS;

public class ZMIPSPrinter {

    private int indent_;
    private final StringBuilder asm_;
    private boolean newline_;

    public ZMIPSPrinter() {
        indent_ = 0;
        asm_ = new StringBuilder();
        newline_ = true;
    }

    public void print(String s) {
        if (newline_) {
            for (int i = 0; i < indent_; i++) {
                asm_.append("\t");
            }
            newline_ = false;
        }
        asm_.append(s).append(" ");
    }

    public void printLabel(String s) {
        asm_.append(s).append(":");
    }

    public void println(String s) {
        if (newline_) {
            for (int i = 0; i < indent_; i++) {
                asm_.append("\t");
            }
        }
        asm_.append(s).append("\n");
        newline_ = true;
    }

    public void println() {
        asm_.append("\n");
        newline_ = true;
    }

    public void begin(String method) {
        indent_ = 2;
        println(".text");
        println(".globl " + method);
        indent_ = 0;
        println(method + ":");
        indent_ = 2;
    }

    public void end() {
        indent_ = 0;
        println();
    }

    public String toString() {
        return asm_.toString();
    }

}