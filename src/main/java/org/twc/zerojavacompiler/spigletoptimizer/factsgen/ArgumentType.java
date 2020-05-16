package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class ArgumentType extends FactType {

    public String temp;

    public ArgumentType(String meth_name, String temp) {
        super(meth_name);
        this.temp = "\"" + temp + "\"";
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "arg(" + this.meth_name + ", " + this.temp + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
