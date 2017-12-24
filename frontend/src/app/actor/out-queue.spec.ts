import { OutQueue } from './out-queue';
import { Message } from './message';
import { Address } from './address';

describe('OutQueue', () => {
    it('should add and remove in the correct order', () => {
        const fixture: OutQueue = new OutQueue();
        let actual: Message | undefined;



        fixture.add(new Message(Address.fromString('a1'), Address.fromString('b1'), 'java.lang.String', 'str1'));
        fixture.add(new Message(Address.fromString('a2'), Address.fromString('b2'), 'java.lang.String', 'str2'));

        actual = fixture.remove();
        if (actual === undefined) { // typeguard
            throw new Error();
        }
        expect(Address.fromString('a1').equals(actual.source)).toEqual(true);
        expect(Address.fromString('b1').equals(actual.destination)).toEqual(true);
        expect(actual.type).toEqual('java.lang.String');
        expect(actual.data).toEqual('str1');



        fixture.add(new Message(Address.fromString('a3'), Address.fromString('b3'), 'java.lang.String', 'str3'));
        fixture.add(new Message(Address.fromString('a4'), Address.fromString('b4'), 'java.lang.String', 'str4'));
        fixture.add(new Message(Address.fromString('a5'), Address.fromString('b5'), 'java.lang.String', 'str5'));

        actual = fixture.remove();
        if (actual === undefined) { // typeguard
            throw new Error();
        }
        expect(Address.fromString('a2').equals(actual.source)).toEqual(true);
        expect(Address.fromString('b2').equals(actual.destination)).toEqual(true);
        expect(actual.type).toEqual('java.lang.String');
        expect(actual.data).toEqual('str2');

        actual = fixture.remove();
        if (actual === undefined) { // typeguard
            throw new Error();
        }
        expect(Address.fromString('a3').equals(actual.source)).toEqual(true);
        expect(Address.fromString('b3').equals(actual.destination)).toEqual(true);
        expect(actual.type).toEqual('java.lang.String');
        expect(actual.data).toEqual('str3');

        actual = fixture.remove();
        if (actual === undefined) { // typeguard
            throw new Error();
        }
        expect(Address.fromString('a4').equals(actual.source)).toEqual(true);
        expect(Address.fromString('b4').equals(actual.destination)).toEqual(true);
        expect(actual.type).toEqual('java.lang.String');
        expect(actual.data).toEqual('str4');

        actual = fixture.remove();
        if (actual === undefined) { // typeguard
            throw new Error();
        }
        expect(Address.fromString('a5').equals(actual.source)).toEqual(true);
        expect(Address.fromString('b5').equals(actual.destination)).toEqual(true);
        expect(actual.type).toEqual('java.lang.String');
        expect(actual.data).toEqual('str5');

        actual = fixture.remove();
        expect(actual).toBeUndefined();
    });
});
