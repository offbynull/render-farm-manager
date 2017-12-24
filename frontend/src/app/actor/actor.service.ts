import { EventEmitter } from 'eventemitter3';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Message } from './message';
import { ActorCommunicator } from './actor-communicator';
import { Address } from './address';
import { InQueue, InQueueMarkResult } from './in-queue';
import { OutQueue } from './out-queue';
import { Validate } from '../utils/validate';
import { environment } from '../../environments/environment';

type ReadyCallback = (err: any, addr: Address) => void;

@Injectable()
export class ActorService {
    private communicator: ActorCommunicator;

    private _self?: Address;
    private _selfCallbacks: ReadyCallback[];

    private _statusEmitter: EventEmitter;
    private _messageEmitter: EventEmitter;

    private inQueue: InQueue;
    private outQueue: OutQueue;
    private outQueuePopper?: any;

    constructor(http: HttpClient) {
        Validate.notNullOrUndefined(http);

        this.communicator = new ActorCommunicator(environment.actorApiEndpoint, http);

        this._self = undefined;
        this._selfCallbacks = [];

        this._statusEmitter = new EventEmitter();
        this._messageEmitter = new EventEmitter();

        this.inQueue = new InQueue();
        this.outQueue = new OutQueue();
        this.outQueuePopper = undefined;

        this.init();
    }


    public ready(cb: ReadyCallback): void {
        Validate.notNullOrUndefined(cb);
        if (this._self !== undefined) {
            setTimeout(() => cb(undefined, <Address> this._self), 0);
        } else {
            this._selfCallbacks.push(cb);
        }
    }

    public statusEmitter(): EventEmitter {
        return this._statusEmitter;
    }

    public messageEmitter(): EventEmitter {
        return this._messageEmitter;
    }





    private init(): void {
        this._statusEmitter.emit('init_start');
        this.communicator.address(
            (e, o) => this.initCallback(e, o));
    }

    private initCallback(err: Error | undefined, address: Address | undefined): void {
        if (err !== undefined) {
            this.initErrorCallback(err);
        } else if (address !== undefined) {
            this.initGoodCallback(address);
        } else {
            throw new Error('This should never happen');
        }
    }

    private initGoodCallback(address: Address): void {
        try {
            // we're handling JSON objects that came directly from server -- make sure that what we get back is correct
            Validate.notNullOrUndefined(address);
            Validate.isTrue(address instanceof Address);

            while (this._selfCallbacks.length > 0) {
                const cb: ReadyCallback | undefined = this._selfCallbacks.pop();
                try {
                    if (cb !== undefined) {
                        cb(undefined, address);
                    }
                } catch (e) {
                    // do nothing
                }
            }

            this._statusEmitter.emit('init_success', address);

            // start communicating now that we have an address
            this._self = address;
            this.comm();
        } catch (e) {
            this._statusEmitter.emit('init_error');
            this.init();
        }
    }

    private initErrorCallback(error: Error): void {
        this._statusEmitter.emit('init_fail');
        this.init();
    }







    public writeMessage(source: string | Address, destination: string | Address, type: string, data: Object): void {
        Validate.notNullOrUndefined(source);
        Validate.notNullOrUndefined(destination);
        Validate.notNullOrUndefined(type);
        Validate.notNullOrUndefined(data);

        const sourceAddr: Address = source instanceof Address ? source : Address.fromString(source);
        const destinationAddr: Address = destination instanceof Address ? destination : Address.fromString(destination);

        const msg = new Message(sourceAddr, destinationAddr, type, data);
        this.inQueue.add(msg);
    }





    private comm(): void {
        Validate.isTrue(this._self !== undefined);

        this._statusEmitter.emit('connect_start');

        const id: string = (<Address>this._self).getElement((<Address>this._self).size() - 1);
        const inQueueMark: InQueueMarkResult = this.inQueue.mark();
        const outQueueOffset: number = this.outQueue.offset;
        this.communicator.communicate(
            id,
            outQueueOffset,
            inQueueMark.offset,
            inQueueMark.messages,
            (e, o) => this.commCallback(e, o));
    }

    private commCallback(err: Error | undefined, outQueue: Message[] | undefined): void {
        if (err !== undefined) {
            this.commErrorCallback(err);
        } else if (outQueue !== undefined) {
            this.commGoodCallback(outQueue);
        } else {
            throw new Error('This should never happen');
        }
    }

    private commGoodCallback(outQueue: Message[]): void {
        try {
            // we're handling JSON objects that came directly from server -- make sure that what we get back is correct
            Validate.notNullOrUndefined(outQueue);
            Validate.isTrue(outQueue instanceof Array);
            Validate.notNullOrUndefined(outQueue);

            // dump incoming messages into outQueue
            outQueue.forEach(m => this.outQueue.add(m));

            // discard outgoing messages before last mark
            this.inQueue.forward();

            // if outQueuePopper isn't active, activate it...
            if (this.outQueuePopper === undefined && this.outQueue.length > 0) {
                this.outQueuePopper = setTimeout(() => this.popAndProcessOutQueue(), 0);
            }

            this._statusEmitter.emit('connection_success');
        } catch (e) {
            this._statusEmitter.emit('connection_error', e);
        } finally {
            this.comm();
        }
    }

    private commErrorCallback(error: Error): void {
        this._statusEmitter.emit('connection_fail', error);
        this.comm();
    }

    private popAndProcessOutQueue(): void {
        const msg: Message | undefined = this.outQueue.remove();

        if (msg !== undefined) {
            let emitAddress: Address = msg.destination;

            while (true) {
                const val: string = emitAddress.toString();
                this._messageEmitter.emit(val, msg);

                if (emitAddress.size() === 1) {
                    break;
                }
                emitAddress = emitAddress.removeSuffix(1);
            }
        }

        if (this.outQueue.length === 0) {
            this.outQueuePopper = undefined;
        } else {
            this.outQueuePopper = setTimeout(() => this.popAndProcessOutQueue(), 0);
        }
    }
}
