import { HttpClient } from '@angular/common/http';

import { Address } from './address';
import { Message } from './message';
import { Validate } from '../utils/validate';

type CommCallback = (err: any, outQueue: Message[] | undefined) => void;
type AddrCallback = (err: any, self: Address | undefined) => void;

export class ActorCommunicator {
    private baseUrl: string;
    private http: HttpClient;

    constructor(baseUrl: string, http: HttpClient) {
        Validate.notNullOrUndefined(baseUrl);
        Validate.notNullOrUndefined(http);
        this.http = http;
        this.baseUrl = baseUrl;
    }



    public address(cb: AddrCallback): void {
        Validate.notNullOrUndefined(cb);

        const url: string = this.baseUrl + '/address';
        this.http.get(url, { responseType: 'text' }).subscribe(
            data => this.addrGoodResponse(data, cb),
            err => this.addrErrorResponse(err, cb)
        );
    }

    private addrGoodResponse(response: string, cb: AddrCallback): void {
        try {
            // we're handling JSON objects that came directly from server -- make sure that what we get back is correct
            Validate.notNullOrUndefined(response);
            Validate.isTrue(typeof response === 'string');

            cb(undefined, Address.fromString(response));
        } catch (e) {
            cb(e, undefined);
        }
    }

    private addrErrorResponse(error: any, cb: AddrCallback): void {
        cb(error, undefined);
    }





    public communicate(id: string, outQueueOffset: number, inQueueOffset: number, inQueue: Message[], cb: CommCallback): void {
        Validate.notNullOrUndefined(id);
        Validate.notNullOrUndefined(outQueueOffset);
        Validate.notNullOrUndefined(inQueueOffset);
        Validate.notNullOrUndefined(inQueue);
        Validate.notNullOrUndefined(cb);
        Validate.noNullOrUndefinedElements(inQueue);
        Validate.isTrue(outQueueOffset >= 0);
        Validate.isTrue(inQueueOffset >= 0);

        const inQueueForTransmit: CommMessage[] = inQueue
            .map(x => new CommMessage(x.source.toString(), x.destination.toString(), x.type, x.data));

        const url: string = this.baseUrl + '/rfm';
        const req: CommRequest = new CommRequest(id, outQueueOffset, inQueueOffset, inQueueForTransmit);

        this.http.post<CommResponse>(url, req).subscribe(
            data => this.commGoodResponse(data, cb),
            err => this.commErrorResponse(err, cb)
        );
    }

    private commGoodResponse(response: CommResponse, cb: CommCallback): void {
        try {
            // we're handling JSON objects that came directly from server -- make sure that what we get back is correct
            Validate.notNullOrUndefined(response);

            const outQueue = response.outQueue;
            Validate.notNullOrUndefined(outQueue);
            Validate.isTrue(outQueue instanceof Array);

            const outQueueForConsumption: Message[] = outQueue
                .map(x => new Message(Address.fromString(x.source), Address.fromString(x.destination), x.type, x.data));

            cb(undefined, outQueueForConsumption);
        } catch (e) {
            cb(e, undefined);
        }
    }

    private commErrorResponse(error: any, cb: CommCallback): void {
        cb(error, undefined);
    }
}

class CommMessage {
    constructor(
        public readonly source: string,
        public readonly destination: string,
        public readonly type: string,
        public readonly data: Object) {}
}

class CommRequest {
    constructor(
        public readonly id: string,
        public readonly outQueueOffset: number,
        public readonly inQueueOffset: number,
        public readonly inQueue: CommMessage[]) {}
}

class CommResponse {
    constructor(public readonly outQueue: CommMessage[]) {}
}
