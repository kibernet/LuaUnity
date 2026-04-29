package com.kibernet.luaunity.model;

import java.util.List;

public final class LuaUnityMethod {
    private final String name;
    private final boolean staticMethod;
    private final List<LuaUnityParameter> parameters;
    private final LuaUnityTypeRef returnType;

    public LuaUnityMethod(String name, boolean staticMethod, List<LuaUnityParameter> parameters, LuaUnityTypeRef returnType) {
        this.name = name;
        this.staticMethod = staticMethod;
        this.parameters = List.copyOf(parameters);
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public boolean isStaticMethod() {
        return staticMethod;
    }

    public List<LuaUnityParameter> getParameters() {
        return parameters;
    }

    public LuaUnityTypeRef getReturnType() {
        return returnType;
    }
}
