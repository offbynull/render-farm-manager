import { Address } from './address';
import { Validate } from '../utils/validate';

export class Message {
    constructor(
        public readonly source: Address,
        public readonly destination: Address,
        public readonly type: string,
        public readonly data: Object) {
            Validate.notNullOrUndefined(source);
            Validate.notNullOrUndefined(destination);
            Validate.notNullOrUndefined(type);
            Validate.notNullOrUndefined(data);
        }
}
