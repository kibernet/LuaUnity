import * as fs from 'fs/promises';
import * as path from 'path';

export interface LuaUnityTypeRef {
    name: string;
    isArray?: boolean;
}

export interface LuaUnityMember {
    name: string;
    type: LuaUnityTypeRef;
}

export interface LuaUnityMethod {
    name: string;
    isStatic: boolean;
    parameters: LuaUnityParameter[];
    returnType: LuaUnityTypeRef;
}

export interface LuaUnityParameter {
    name: string;
    type: LuaUnityTypeRef;
}

export interface LuaUnityClass {
    fullName: string;
    baseType?: string;
    fields: LuaUnityMember[];
    properties: LuaUnityMember[];
    methods: LuaUnityMethod[];
}

const typeMap = new Map<string, string>([
    ['System.Single', 'number'],
    ['System.Double', 'number'],
    ['System.Decimal', 'number'],
    ['System.Int16', 'number'],
    ['System.Int32', 'number'],
    ['System.Int64', 'number'],
    ['System.SByte', 'number'],
    ['System.UInt16', 'number'],
    ['System.UInt32', 'number'],
    ['System.UInt64', 'number'],
    ['System.Boolean', 'boolean'],
    ['System.String', 'string'],
    ['System.Object', 'table'],
    ['System.Void', 'void']
]);

export class LuaAnnotationGenerator {
    async generate(classes: LuaUnityClass[], outputDirectory: string): Promise<number> {
        await fs.rm(outputDirectory, { recursive: true, force: true });
        await fs.mkdir(outputDirectory, { recursive: true });

        const namespaces = collectNamespaces(classes);
        for (const namespace of namespaces) {
            await this.writeNamespace(outputDirectory, namespace);
        }

        for (const clazz of classes) {
            await this.writeClass(outputDirectory, clazz);
        }

        return namespaces.length + classes.length;
    }

    private async writeNamespace(outputDirectory: string, namespace: string): Promise<void> {
        const filePath = path.join(outputDirectory, `${namespace}.ns.lua`);
        const lines: string[] = [];
        const parts = namespace.split('.');

        for (let i = 0; i < parts.length; i++) {
            const current = parts.slice(0, i + 1).join('.');
            lines.push(`${current} = ${current} or {}`);
        }

        await fs.writeFile(filePath, `${lines.join('\n')}\n`, 'utf8');
    }

    private async writeClass(outputDirectory: string, clazz: LuaUnityClass): Promise<void> {
        const filePath = path.join(outputDirectory, `${clazz.fullName}.lua`);
        const lines: string[] = [];

        for (const field of clazz.fields) {
            lines.push(`---@field public ${sanitizeIdentifier(field.name)} ${renderType(field.type)}`);
        }
        for (const property of clazz.properties) {
            lines.push(`---@field public ${sanitizeIdentifier(property.name)} ${renderType(property.type)}`);
        }

        const classDeclaration = clazz.baseType
            ? `---@class ${clazz.fullName} : ${convertTypeName(clazz.baseType)}`
            : `---@class ${clazz.fullName}`;
        lines.push(classDeclaration);
        lines.push('local m = {}');

        const groupedMethods = groupByName(clazz.methods);
        for (const methods of groupedMethods.values()) {
            const [first, ...overloads] = methods;
            lines.push('');
            for (const overload of overloads) {
                lines.push(renderOverload(overload));
            }
            for (const parameter of first.parameters) {
                lines.push(`---@param ${sanitizeIdentifier(parameter.name)} ${renderType(parameter.type)}`);
            }
            if (renderType(first.returnType) !== 'void') {
                lines.push(`---@return ${renderType(first.returnType)}`);
            }

            const receiver = first.isStatic ? '.' : ':';
            const parameters = first.parameters.map(parameter => sanitizeIdentifier(parameter.name)).join(', ');
            lines.push(`function m${receiver}${sanitizeIdentifier(first.name)}(${parameters}) end`);
        }

        lines.push(`${clazz.fullName} = m`);
        lines.push('return m');

        await fs.writeFile(filePath, `${lines.join('\n')}\n`, 'utf8');
    }
}

function collectNamespaces(classes: LuaUnityClass[]): string[] {
    const namespaces = new Set<string>();
    for (const clazz of classes) {
        const parts = clazz.fullName.split('.');
        for (let i = 1; i < parts.length; i++) {
            namespaces.add(parts.slice(0, i).join('.'));
        }
    }
    return [...namespaces].sort((a, b) => a.localeCompare(b));
}

function groupByName(methods: LuaUnityMethod[]): Map<string, LuaUnityMethod[]> {
    const grouped = new Map<string, LuaUnityMethod[]>();
    for (const method of methods) {
        if (!grouped.has(method.name)) {
            grouped.set(method.name, []);
        }
        grouped.get(method.name)!.push(method);
    }
    return grouped;
}

function renderOverload(method: LuaUnityMethod): string {
    const params = method.parameters
        .map(parameter => `${sanitizeIdentifier(parameter.name)}: ${renderType(parameter.type)}`)
        .join(', ');
    const returnType = renderType(method.returnType);
    return returnType === 'void'
        ? `---@overload fun(${params})`
        : `---@overload fun(${params}): ${returnType}`;
}

function renderType(type: LuaUnityTypeRef): string {
    const converted = convertTypeName(type.name);
    return type.isArray ? `${converted}[]` : converted;
}

function convertTypeName(name: string): string {
    return typeMap.get(name) ?? name;
}

function sanitizeIdentifier(name: string): string {
    const safe = name.replace(/[^A-Za-z0-9_]/g, '_');
    return /^[A-Za-z_]/.test(safe) ? safe : `_${safe}`;
}
