package org.twc.zerojavacompiler.basetype;

import java.util.List;

public class Base_t {

    private final String name;

    public Base_t(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean containsName(final List<? extends Base_t> list, final String name) {
        return list.stream().anyMatch(o -> o.getName().equals(name));
    }

    public Base_t getByName(final List<? extends Base_t> list, final String name) {
        return list.stream().filter(o -> o.getName().equals(name)).findFirst().orElse(null);
    }

}

