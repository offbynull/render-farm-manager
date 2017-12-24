import { Message } from './message';
import { Validate } from '../utils/validate';

export class OutQueue {
    private _offset: number = 0;
    private messages: Message[] = [];

    public add(message: Message): void {
        Validate.notNullOrUndefined(message);
        this.messages.push(message);
        this._offset += 1;
    }

    public remove(): Message | undefined {
        return this.messages.shift();
    }

    public get offset(): number {
        return this._offset;
    }

    public get length(): number {
        return this.messages.length;
    }
}
