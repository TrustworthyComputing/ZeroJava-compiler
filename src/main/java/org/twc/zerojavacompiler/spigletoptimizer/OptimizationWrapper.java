package org.twc.zerojavacompiler.spigletoptimizer;

public class OptimizationWrapper {

    private final String instr_or_temp_;
    private final String const_prop_;

    public OptimizationWrapper(String instr_or_temp) {
        this.instr_or_temp_ = instr_or_temp;
        this.const_prop_ = null;
    }

    public OptimizationWrapper(String instr_or_temp, String const_prop_) {
        this.instr_or_temp_ = instr_or_temp;
        this.const_prop_ = const_prop_;
    }

    /**
     * If a constant propagation is found, return it.
     * If a copy propagation is found, it will be in the instr_or_temp_ field.
     * Otherwise, return the temp data.
     **/
    public String getOptimizedTemp() {
        if (this.const_prop_ != null) {
            return this.const_prop_;
        } else {
            return this.instr_or_temp_;
        }
    }

    public String getInstr_or_temp_() {
        return this.instr_or_temp_;
    }

}
