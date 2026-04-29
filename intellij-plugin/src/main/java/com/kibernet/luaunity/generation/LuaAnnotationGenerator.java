package com.kibernet.luaunity.generation;

import com.kibernet.luaunity.model.LuaUnityClass;
import com.kibernet.luaunity.model.LuaUnityMember;
import com.kibernet.luaunity.model.LuaUnityMethod;
import com.kibernet.luaunity.model.LuaUnityParameter;
import com.kibernet.luaunity.model.LuaUnityTypeRef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class LuaAnnotationGenerator {
    private static final Map<String, String> TYPE_MAP = Map.ofEntries(
            Map.entry("System.Single", "number"),
            Map.entry("System.Double", "number"),
            Map.entry("System.Decimal", "number"),
            Map.entry("System.Int16", "number"),
            Map.entry("System.Int32", "number"),
            Map.entry("System.Int64", "number"),
            Map.entry("System.SByte", "number"),
            Map.entry("System.UInt16", "number"),
            Map.entry("System.UInt32", "number"),
            Map.entry("System.UInt64", "number"),
            Map.entry("System.Boolean", "boolean"),
            Map.entry("System.String", "string"),
            Map.entry("System.Object", "table"),
            Map.entry("System.Void", "void")
    );

    public int generate(List<LuaUnityClass> classes, Path outputDirectory) throws IOException {
        deleteDirectory(outputDirectory);
        Files.createDirectories(outputDirectory);

        Set<String> namespaces = collectNamespaces(classes);
        for (String namespace : namespaces) {
            writeNamespace(outputDirectory, namespace);
        }
        for (LuaUnityClass luaClass : classes) {
            writeClass(outputDirectory, luaClass);
        }

        return namespaces.size() + classes.size();
    }

    private void writeNamespace(Path outputDirectory, String namespace) throws IOException {
        String[] parts = namespace.split("\\.");
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= parts.length; i++) {
            String current = String.join(".", List.of(parts).subList(0, i));
            lines.add(current + " = " + current + " or {}");
        }
        Files.writeString(outputDirectory.resolve(namespace + ".ns.lua"), String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private void writeClass(Path outputDirectory, LuaUnityClass luaClass) throws IOException {
        List<String> lines = new ArrayList<>();
        for (LuaUnityMember field : luaClass.getFields()) {
            lines.add("---@field public " + sanitizeIdentifier(field.getName()) + " " + renderType(field.getType()));
        }
        for (LuaUnityMember property : luaClass.getProperties()) {
            lines.add("---@field public " + sanitizeIdentifier(property.getName()) + " " + renderType(property.getType()));
        }

        if (luaClass.getBaseType() == null || luaClass.getBaseType().isBlank()) {
            lines.add("---@class " + luaClass.getFullName());
        } else {
            lines.add("---@class " + luaClass.getFullName() + " : " + convertTypeName(luaClass.getBaseType()));
        }
        lines.add("local m = {}");

        for (List<LuaUnityMethod> methods : groupByName(luaClass.getMethods()).values()) {
            LuaUnityMethod first = methods.get(0);
            lines.add("");
            for (int i = 1; i < methods.size(); i++) {
                lines.add(renderOverload(methods.get(i)));
            }
            for (LuaUnityParameter parameter : first.getParameters()) {
                lines.add("---@param " + sanitizeIdentifier(parameter.getName()) + " " + renderType(parameter.getType()));
            }
            String returnType = renderType(first.getReturnType());
            if (!"void".equals(returnType)) {
                lines.add("---@return " + returnType);
            }
            String receiver = first.isStaticMethod() ? "." : ":";
            String parameters = first.getParameters().stream()
                    .map(parameter -> sanitizeIdentifier(parameter.getName()))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            lines.add("function m" + receiver + sanitizeIdentifier(first.getName()) + "(" + parameters + ") end");
        }

        lines.add(luaClass.getFullName() + " = m");
        lines.add("return m");
        Files.writeString(outputDirectory.resolve(luaClass.getFullName() + ".lua"), String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private Set<String> collectNamespaces(List<LuaUnityClass> classes) {
        Set<String> namespaces = new TreeSet<>();
        for (LuaUnityClass luaClass : classes) {
            String[] parts = luaClass.getFullName().split("\\.");
            for (int i = 1; i < parts.length; i++) {
                namespaces.add(String.join(".", List.of(parts).subList(0, i)));
            }
        }
        return namespaces;
    }

    private Map<String, List<LuaUnityMethod>> groupByName(List<LuaUnityMethod> methods) {
        Map<String, List<LuaUnityMethod>> grouped = new LinkedHashMap<>();
        for (LuaUnityMethod method : methods) {
            grouped.computeIfAbsent(method.getName(), ignored -> new ArrayList<>()).add(method);
        }
        return grouped;
    }

    private String renderOverload(LuaUnityMethod method) {
        String parameters = method.getParameters().stream()
                .map(parameter -> sanitizeIdentifier(parameter.getName()) + ": " + renderType(parameter.getType()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        String returnType = renderType(method.getReturnType());
        return "void".equals(returnType)
                ? "---@overload fun(" + parameters + ")"
                : "---@overload fun(" + parameters + "): " + returnType;
    }

    private String renderType(LuaUnityTypeRef typeRef) {
        String converted = convertTypeName(typeRef.getName());
        return typeRef.isArray() ? converted + "[]" : converted;
    }

    private String convertTypeName(String name) {
        return TYPE_MAP.getOrDefault(name, name);
    }

    private String sanitizeIdentifier(String name) {
        String safe = name.replaceAll("[^A-Za-z0-9_]", "_");
        return safe.matches("^[A-Za-z_].*") ? safe : "_" + safe;
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
