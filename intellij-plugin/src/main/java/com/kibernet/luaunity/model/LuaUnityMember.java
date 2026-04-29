package com.kibernet.luaunity.model;

public final class LuaUnityMember {
    private final String name;
    private final LuaUnityTypeRef type;

    public LuaUnityMember(String name, LuaUnityTypeRef type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public LuaUnityTypeRef getType() {
        return type;
    }
}
