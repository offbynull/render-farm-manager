import { Message } from './message';
import { Validate } from '../utils/validate';

export class InQueue {
    private offset: number = 0;
    private _mark: number = 0;

    private messages: Message[] = [];

    public add(message: Message): void {
        Validate.notNullOrUndefined(message);
        this.messages.push(message);
    }

    public mark(): InQueueMarkResult {
        this._mark = this.offset + this.messages.length;
        return new InQueueMarkResult(this.offset, this.messages.slice(0));
    }

    public forward(): void {
        this.messages = this.messages.slice(this._mark - this.offset);
        this.offset = this._mark;
    }
}

export class InQueueMarkResult {
    constructor(public readonly offset: number, public readonly messages: Message[]) {}
}
