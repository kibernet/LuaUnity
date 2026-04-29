package com.kibernet.luaunity.model;

public final class LuaUnityTypeRef {
    private final String name;
    private final boolean array;

    public LuaUnityTypeRef(String name, boolean array) {
        this.name = name;
        this.array = array;
    }

    public String getName() {
        return name;
    }

    public boolean isArray() {
        return array;
    }
}
