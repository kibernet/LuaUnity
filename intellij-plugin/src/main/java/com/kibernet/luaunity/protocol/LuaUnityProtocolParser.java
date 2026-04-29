package com.kibernet.luaunity.protocol;

import com.kibernet.luaunity.model.LuaUnityClass;
import com.kibernet.luaunity.model.LuaUnityMember;
import com.kibernet.luaunity.model.LuaUnityMethod;
import com.kibernet.luaunity.model.LuaUnityParameter;
import com.kibernet.luaunity.model.LuaUnityTypeRef;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;

public final class LuaUnityProtocolParser {
    private LuaUnityProtocolParser() {
    }

    public static List<LuaUnityClass> parseLibrary(byte[] payload) throws EOFException {
        BinaryReader reader = new BinaryReader(payload);
        List<LuaUnityClass> classes = new ArrayList<>();

        while (reader.available() > 0) {
            String fullName = reader.readString();
            if (fullName.isEmpty()) {
                break;
            }

            String baseType = reader.readBoolean() ? reader.readString() : null;
            List<LuaUnityMember> fields = readMembers(reader);
            List<LuaUnityMember> properties = readMembers(reader);
            List<LuaUnityMethod> methods = readMethods(reader);
            classes.add(new LuaUnityClass(fullName, baseType, fields, properties, methods));
        }

        return classes;
    }

    private static List<LuaUnityMember> readMembers(BinaryReader reader) throws EOFException {
        int count = reader.readInt();
        List<LuaUnityMember> members = new ArrayList<>(Math.max(count, 0));
        for (int i = 0; i < count; i++) {
            members.add(new LuaUnityMember(reader.readString(), readType(reader)));
        }
        return members;
    }

    private static List<LuaUnityMethod> readMethods(BinaryReader reader) throws EOFException {
        int count = reader.readInt();
        List<LuaUnityMethod> methods = new ArrayList<>(Math.max(count, 0));
        for (int i = 0; i < count; i++) {
            String name = reader.readString();
            boolean staticMethod = reader.readBoolean();
            List<LuaUnityParameter> parameters = readParameters(reader);
            LuaUnityTypeRef returnType = readType(reader);
            methods.add(new LuaUnityMethod(name, staticMethod, parameters, returnType));
        }
        return methods;
    }

    private static List<LuaUnityParameter> readParameters(BinaryReader reader) throws EOFException {
        int count = reader.readInt();
        List<LuaUnityParameter> parameters = new ArrayList<>(Math.max(count, 0));
        for (int i = 0; i < count; i++) {
            parameters.add(new LuaUnityParameter(reader.readString(), readType(reader)));
        }
        return parameters;
    }

    private static LuaUnityTypeRef readType(BinaryReader reader) throws EOFException {
        byte kind = reader.readByte();
        if (kind == 1) {
            LuaUnityTypeRef baseType = readType(reader);
            return new LuaUnityTypeRef(baseType.getName(), true);
        }
        return new LuaUnityTypeRef(reader.readString(), false);
    }
}
