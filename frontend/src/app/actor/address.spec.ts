import { Address } from './address';

describe('Address', () => {
    it('must construct single element address from string', () => {
        const fixture: Address = Address.fromString('hi');
        expect(fixture.getElements()).toEqual(['hi']);
    });

    it('must construct multi-element address from string', () => {
        const fixture: Address = Address.fromString('hi:to:you');
        expect(fixture.getElements()).toEqual(['hi', 'to', 'you']);
    });

    it('must construct multi-element address from string with escape sequences', () => {
        const fixture: Address = Address.fromString('\\:h\\\\i\\::\\\\t\\:o\\\\:you');
        expect(fixture.getElements()).toEqual([':h\\i:', '\\t:o\\', 'you']);
    });

    it('must construct address from empty string', () => {
        const fixture: Address = Address.fromString('');
        expect(fixture.getElements()).toEqual(['']);
    });

    it('must properly convert address to and from string', () => {
        const fixture: Address = Address.of(':h\\i:', '\\t:o\\', 'you');
        const fixtureStr: string = fixture.toString();
        const reconstructed: Address = Address.fromString(fixture.toString());
        expect(fixture).toEqual(reconstructed);
    });

    it('must identify as prefix', () => {
        const parent: Address = Address.of('one', 'two');
        const fixture: Address = Address.of('one', 'two', 'three');
        expect(parent.isPrefixOf(fixture)).toBe(true);
    });

    it('must not identify as prefix', () => {
        const other: Address = Address.of('one', 'two', 'three');
        const fixture: Address = Address.of('one', 'xxx', 'three');
        expect(other.isPrefixOf(fixture)).toBe(false);
    });

    it('must not identify as prefix when shorter', () => {
        const other: Address = Address.of('one', 'two', 'three');
        const fixture: Address = Address.of('one', 'two');
        expect(other.isPrefixOf(fixture)).toBe(false);
    });

    it('must identify as prefix when equal', () => {
        const other: Address = Address.of('one', 'two', 'three');
        const fixture: Address = Address.of('one', 'two', 'three');
        expect(other.isPrefixOf(fixture)).toBe(true);
    });

    it('must identify as parent or equal when parent', () => {
        const parent: Address = Address.of('one', 'two');
        const fixture: Address = Address.of('one', 'two', 'three');
        expect(parent.isPrefixOf(fixture)).toBe(true);
    });

    it('must identify as parent or equal when equal', () => {
        const parent: Address = Address.of('one', 'two', 'three');
        const fixture: Address = Address.of('one', 'two', 'three');
        expect(parent.isPrefixOf(fixture)).toBe(true);
    });

    it('must not identify as parent or equal', () => {
        const other: Address = Address.of('one', 'two', 'three');
        const fixture: Address = Address.of('one', 'xxx', 'three');
        expect(other.isPrefixOf(fixture)).toBe(false);
    });

    it('must relativize to address', () => {
        const parent: Address = Address.of('one', 'two');
        const fixture: Address = Address.of('one', 'two', 'three');
        expect(Address.of('three')).toEqual(fixture.removePrefix(parent));
    });

    it('must fail if relativized to empty', () => {
        const parent: Address = Address.of('one', 'two');
        const fixture: Address = Address.of('one', 'two');
        expect(() => fixture.removePrefix(parent)).toThrowError();
    });

    it('must fail to remove prefix', () => {
        const other: Address = Address.of('one', 'two', 'three');
        const fixture: Address = Address.of('one', 'xxx', 'three');
        expect(() => fixture.removePrefix(other)).toThrowError();
    });


    it('must remove suffix', () => {
        const fixture: Address = Address.of('one', 'xxx', 'three');
        expect(Address.of('one')).toEqual(fixture.removeSuffix(2));
    });

    it('must not remove suffix if remove count is 0', () => {
        const fixture: Address = Address.of('one', 'xxx', 'three');
        expect(fixture).toEqual(fixture.removeSuffix(0));
    });

    it('must fail to remove suffix if remove count is all', () => {
        const fixture: Address = Address.of('one', 'xxx', 'three');
        expect(() => fixture.removeSuffix(3)).toThrowError();
    });

    it('must fail to remove suffix if remove count is out of bounds', () => {
        const fixture: Address = Address.of('one', 'xxx', 'three');
        expect(() => fixture.removeSuffix(4)).toThrowError();
    });

    it('must fail to construct an address with 0 elements', () => {
        expect(() => Address.of()).toThrowError();
    });

    it('must equal when address is the same', () => {
        const a: Address = Address.fromString('a:b:c');
        const b: Address = Address.fromString('a:b:c');

        expect(a.equals(b)).toEqual(true);
        expect(b.equals(a)).toEqual(true);
        expect(a.equals(a)).toEqual(true);
        expect(b.equals(b)).toEqual(true);
    });

    it('must have the same hashcode when address is the same', () => {
        const a: Address = Address.fromString('a:b:c');
        const b: Address = Address.fromString('a:b:c');

        expect(a.hashCode()).toEqual(b.hashCode());
        expect(b.hashCode()).toEqual(a.hashCode());
        expect(a.hashCode()).toEqual(a.hashCode());
        expect(b.hashCode()).toEqual(b.hashCode());
    });

    it('must have equal same when address is different', () => {
        const a: Address = Address.fromString('a:b:c');
        const b: Address = Address.fromString('a:b:c:d');

        expect(a.equals(b)).toEqual(false);
        expect(b.equals(a)).toEqual(false);
    });

    it('must have different hashcode when address is different', () => {
        const a: Address = Address.fromString('a:b:c');
        const b: Address = Address.fromString('a:b:c:d');

        expect(a.hashCode() === b.hashCode()).toEqual(false);
    });
});
