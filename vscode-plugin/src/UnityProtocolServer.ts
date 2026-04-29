import * as net from 'net';
import { EventEmitter } from 'events';
import {
    LuaUnityClass,
    LuaUnityMember,
    LuaUnityMethod,
    LuaUnityParameter,
    LuaUnityTypeRef
} from './LuaAnnotationGenerator';

export interface UnityProtocolServerEvents {
    library: [LuaUnityClass[]];
    error: [Error];
    listening: [number];
    stopped: [];
}

export class UnityProtocolServer extends EventEmitter {
    private server: net.Server | undefined;
    private client: net.Socket | undefined;

    start(port: number): void {
        if (this.server) {
            return;
        }

        this.server = net.createServer(socket => this.handleClient(socket));
        this.server.on('error', error => this.emit('error', error));
        this.server.listen(port, '127.0.0.1', () => this.emit('listening', port));
    }

    stop(): void {
        this.client?.destroy();
        this.client = undefined;

        this.server?.close(() => this.emit('stopped'));
        this.server = undefined;
    }

    isRunning(): boolean {
        return this.server !== undefined;
    }

    override on<K extends keyof UnityProtocolServerEvents>(
        eventName: K,
        listener: (...args: UnityProtocolServerEvents[K]) => void
    ): this {
        return super.on(eventName, listener);
    }

    private handleClient(socket: net.Socket): void {
        if (this.client) {
            socket.destroy();
            return;
        }

        this.client = socket;
        let pending: Buffer = Buffer.alloc(0);

        socket.on('data', chunk => {
            try {
                pending = Buffer.concat([pending, chunk]);
                pending = this.processPackets(pending);
            } catch (error) {
                this.emit('error', error instanceof Error ? error : new Error(String(error)));
                socket.destroy();
            }
        });
        socket.on('close', () => {
            if (this.client === socket) {
                this.client = undefined;
            }
        });
        socket.on('error', error => this.emit('error', error));
    }

    private processPackets(buffer: Buffer): Buffer {
        let offset = 0;

        while (buffer.length - offset >= 8) {
            const packetSize = buffer.readInt32LE(offset);
            if (packetSize < 8) {
                throw new Error(`Invalid LuaUnity packet size: ${packetSize}`);
            }
            if (buffer.length - offset < packetSize) {
                break;
            }

            const protocol = buffer.readInt32LE(offset + 4);
            const payload = buffer.subarray(offset + 8, offset + packetSize);
            if (protocol === 0) {
                this.emit('library', parseLibrary(payload));
            }

            offset += packetSize;
        }

        return buffer.subarray(offset);
    }
}

class BinaryReader {
    private offset = 0;

    constructor(private readonly buffer: Buffer) {}

    get available(): number {
        return this.buffer.length - this.offset;
    }

    readBoolean(): boolean {
        return this.readByte() !== 0;
    }

    readByte(): number {
        this.ensure(1);
        return this.buffer.readUInt8(this.offset++);
    }

    readInt32(): number {
        this.ensure(4);
        const value = this.buffer.readInt32LE(this.offset);
        this.offset += 4;
        return value;
    }

    readString(): string {
        const length = this.readInt32();
        this.ensure(length);
        const value = this.buffer.toString('utf8', this.offset, this.offset + length);
        this.offset += length;
        return value;
    }

    private ensure(length: number): void {
        if (this.available < length) {
            throw new Error('Unexpected end of LuaUnity payload.');
        }
    }
}

function parseLibrary(payload: Buffer): LuaUnityClass[] {
    const reader = new BinaryReader(payload);
    const classes: LuaUnityClass[] = [];

    while (reader.available > 0) {
        const fullName = reader.readString();
        if (fullName.length === 0) {
            break;
        }

        const hasBaseType = reader.readBoolean();
        const baseType = hasBaseType ? reader.readString() : undefined;
        const fields = readMembers(reader);
        const properties = readMembers(reader);
        const methods = readMethods(reader);

        classes.push({ fullName, baseType, fields, properties, methods });
    }

    return classes;
}

function readMembers(reader: BinaryReader): LuaUnityMember[] {
    const count = reader.readInt32();
    const members: LuaUnityMember[] = [];

    for (let i = 0; i < count; i++) {
        members.push({
            name: reader.readString(),
            type: readType(reader)
        });
    }

    return members;
}

function readMethods(reader: BinaryReader): LuaUnityMethod[] {
    const count = reader.readInt32();
    const methods: LuaUnityMethod[] = [];

    for (let i = 0; i < count; i++) {
        const name = reader.readString();
        const isStatic = reader.readBoolean();
        const parameters = readParameters(reader);
        const returnType = readType(reader);
        methods.push({ name, isStatic, parameters, returnType });
    }

    return methods;
}

function readParameters(reader: BinaryReader): LuaUnityParameter[] {
    const count = reader.readInt32();
    const parameters: LuaUnityParameter[] = [];

    for (let i = 0; i < count; i++) {
        parameters.push({
            name: reader.readString(),
            type: readType(reader)
        });
    }

    return parameters;
}

function readType(reader: BinaryReader): LuaUnityTypeRef {
    const kind = reader.readByte();
    if (kind === 1) {
        return { ...readType(reader), isArray: true };
    }

    return { name: reader.readString() };
}
