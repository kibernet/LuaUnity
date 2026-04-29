package com.kibernet.luaunity.model;

import java.util.List;

public final class LuaUnityClass {
    private final String fullName;
    private final String baseType;
    private final List<LuaUnityMember> fields;
    private final List<LuaUnityMember> properties;
    private final List<LuaUnityMethod> methods;

    public LuaUnityClass(
            String fullName,
            String baseType,
            List<LuaUnityMember> fields,
            List<LuaUnityMember> properties,
            List<LuaUnityMethod> methods
    ) {
        this.fullName = fullName;
        this.baseType = baseType;
        this.fields = List.copyOf(fields);
        this.properties = List.copyOf(properties);
        this.methods = List.copyOf(methods);
    }

    public String getFullName() {
        return fullName;
    }

    public String getBaseType() {
        return baseType;
    }

    public List<LuaUnityMember> getFields() {
        return fields;
    }

    public List<LuaUnityMember> getProperties() {
        return properties;
    }

    public List<LuaUnityMethod> getMethods() {
        return methods;
    }
}
