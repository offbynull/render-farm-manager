import { InQueue, InQueueMarkResult } from './in-queue';
import { Message } from './message';
import { Address } from './address';

describe('InQueue', () => {
    it('should be able mark and flush', () => {
        const fixture: InQueue = new InQueue();
        let actual: InQueueMarkResult;


        fixture.add(new Message(Address.fromString('a1'), Address.fromString('b1'), 'java.lang.String', 'str1'));
        fixture.add(new Message(Address.fromString('a2'), Address.fromString('b2'), 'java.lang.String', 'str2'));

        actual = fixture.mark();
        expect(actual.offset).toEqual(0);
        expect(actual.messages.length).toEqual(2);
        expect(actual.messages[0].source.toString()).toEqual('a1');
        expect(actual.messages[0].destination.toString()).toEqual('b1');
        expect(actual.messages[0].type).toEqual('java.lang.String');
        expect(actual.messages[0].data).toEqual('str1');
        expect(actual.messages[1].source.toString()).toEqual('a2');
        expect(actual.messages[1].destination.toString()).toEqual('b2');
        expect(actual.messages[1].type).toEqual('java.lang.String');
        expect(actual.messages[1].data).toEqual('str2');


        fixture.add(new Message(Address.fromString('a3'), Address.fromString('b3'), 'java.lang.String', 'str3'));

        actual = fixture.mark();
        expect(actual.offset).toEqual(0);
        expect(actual.messages.length).toEqual(3);
        expect(actual.messages[0].source.toString()).toEqual('a1');
        expect(actual.messages[0].destination.toString()).toEqual('b1');
        expect(actual.messages[0].type).toEqual('java.lang.String');
        expect(actual.messages[0].data).toEqual('str1');
        expect(actual.messages[1].source.toString()).toEqual('a2');
        expect(actual.messages[1].destination.toString()).toEqual('b2');
        expect(actual.messages[1].type).toEqual('java.lang.String');
        expect(actual.messages[1].data).toEqual('str2');
        expect(actual.messages[2].source.toString()).toEqual('a3');
        expect(actual.messages[2].destination.toString()).toEqual('b3');
        expect(actual.messages[2].type).toEqual('java.lang.String');
        expect(actual.messages[2].data).toEqual('str3');


        fixture.add(new Message(Address.fromString('a4'), Address.fromString('b4'), 'java.lang.String', 'str4'));
        fixture.add(new Message(Address.fromString('a5'), Address.fromString('b5'), 'java.lang.String', 'str5'));

        fixture.forward();
        actual = fixture.mark();
        expect(actual.offset).toEqual(3);
        expect(actual.messages.length).toEqual(2);
        expect(actual.messages[0].source.toString()).toEqual('a4');
        expect(actual.messages[0].destination.toString()).toEqual('b4');
        expect(actual.messages[0].type).toEqual('java.lang.String');
        expect(actual.messages[0].data).toEqual('str4');
        expect(actual.messages[1].source.toString()).toEqual('a5');
        expect(actual.messages[1].destination.toString()).toEqual('b5');
        expect(actual.messages[1].type).toEqual('java.lang.String');
        expect(actual.messages[1].data).toEqual('str5');
    });
});
