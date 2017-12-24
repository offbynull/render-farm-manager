import { ValueObject, List } from 'immutable';

import { Validate } from '../utils/validate';

const DELIM: number = ':'.charCodeAt(0);
const ESCAPE: number = '\\'.charCodeAt(0);

export class Address implements ValueObject {
    private readonly addressElements: string[];

    /**
     * Converts an escaped address string back in to an {@link Address}. Pass the result of {@link #toString() } in to this method to
     * recreate the address.
     * <p>
     * Addresses in text form must only contain printable US-ASCII characters and are delimited by colons ({@code ':'}). For example ...
     * <ul>
     * <li>{@code Address.of("one:two")} would produce an address with the elements {@code ["one", "two"]}.</li>
     * <li>{@code Address.of("one\:two")} would produce an address with the elements {@code ["one:two"]}.</li>
     * <li>{@code Address.of("one\\two")} would produce an address with the elements {@code ["one\two"]}.</li>
     * <li>{@code Address.of("::")} would produce an address with the elements {@code ["", "", ""]}.</li>
     * <li>{@code Address.of("")} would produce an address with the element {@code []}.</li>
     * <li>{@code Address.of("a\")} would be invalid (bad escape sequence).</li>
     * <li>{@code Address.of("\a")} would be invalid (bad escape sequence).</li>
     * </ul>
     * @param textAddress address in text-form
     * @return new address
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code textAddress} is malformed (not properly escaped or not printable US-ASCII)
     */
    public static fromString(textAddress: string): Address {
        Validate.notNullOrUndefined(textAddress);

        let input: string | null = textAddress;
        const elements: string[] = [];
        while (input !== null) {
            const ret: [string | null, string] = this.readAndUnescapeNextElement(input);

            input = ret[0];
            const element: string | null = ret[1];

            elements.push(element);
        }

        return new Address(elements);
    }

    /**
     * Converts an array of strings to an {@link Address}.
     *
     * @param elements list of printable US-ASCII strings
     * @return new address
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any element in {@code elements} is malformed (not printable US-ASCII), or if {@code offset} is
     * {@code < 0 || > elements.length}
     */
    public static of(...elements: string[]): Address {
        Validate.notNullOrUndefined(elements);
        Validate.noNullOrUndefinedElements(elements);
        Validate.isTrue(elements.length > 0);

        elements.forEach(x => {// is US-ASCII
            for (let i = 0; i < x.length; i++) {// this should cause surrogate pairs to fail as well, which is what we want!
                const ch = x.charCodeAt(i);
                Validate.isTrue(ch >= 0x20 && ch < 0x7F, 'Not printable ASCII');
            }
        });
        return new Address(elements);
    }

    private static escapeElement(element: string): string { // only escapes the delimiter -- ':'
        Validate.notNullOrUndefined(element);

        let output: string = '';

        // http://stackoverflow.com/a/3585791   printable ASCII check
        for (let i = 0; i < element.length; i++) {// this should cause surrogate pairs to fail as well, which is what we want!
            const ch: number = element.charCodeAt(i);
            Validate.isTrue(ch >= 0x20 && ch < 0x7F, 'Not printable ASCII'); // this should cause surrogate pairs to fail as well, which is
                                                                             // what we want!
            switch (ch) {
                case DELIM:
                    output += String.fromCharCode(ESCAPE) + String.fromCharCode(DELIM);
                    break;
                case ESCAPE:
                    output += String.fromCharCode(ESCAPE) + String.fromCharCode(ESCAPE);
                    break;
                default:
                    output += String.fromCharCode(ch);
                    break;
            }
        }

        return output.toString();
    }

    private static readAndUnescapeNextElement(input: string): [string | null, string] { // "\:" -> ":" and "\\" -> "\"
        Validate.notNullOrUndefined(input);

        let output: string = '';

        // http://stackoverflow.com/a/3585791   printable ASCII check
        let escapeMode: boolean = false;
        let ch: number;
        while (input.length > 0) {
            ch = input.slice(0, 1).charCodeAt(0); // get left-most/first character from str
            input = input.slice(1);               // pop left-most/first character off str

            Validate.isTrue(ch >= 0x20 && ch < 0x7F, 'Not printable ASCII'); // this should cause surrogate pairs to fail as well, which is
                                                                             // what we want!

            if (escapeMode) {
                switch (ch) {
                    case DELIM:
                        output += String.fromCharCode(DELIM);
                        break;
                    case ESCAPE:
                        output += String.fromCharCode(ESCAPE);
                        break;
                    default:
                        throw Error(`Unrecognized escape sequence: ${ch}`);
                }

                escapeMode = false;
            } else {
                if (ch === ESCAPE) {
                    // This is the start of an escape sequence. Character after this one will determine what will be dumped.
                    escapeMode = true;
                    continue;
                } else if (ch === DELIM) {
                    // We're unescaping an address element. Encounter a non-escaped separator (colon) is the end of the address element.
                    break;
                }

                output += String.fromCharCode(ch);
            }
        }

        return [input.length === 0 ? null : input, output];
    }

    private constructor(addressElements: string[]) {
        this.addressElements = addressElements.slice(0); // clone
    }

    /**
     * Gets the number of elements that make up this address.
     * @return number of elements that make up this address
     */
    public size(): number {
        return this.addressElements.length;
    }

    /**
     * Gets if this address is empty.
     * @return {@code true} if empty, otherwise {@code false}
     */
    public isEmpty(): boolean {
        return this.addressElements.length === 0;
    }

    /**
     * Get elements that make up this address.
     * @return elements that make up this address
     */
    public getElements(): string[] {
        return this.addressElements.slice(0); // clone
    }

    /**
     * Get a element at {@code idx} from the list of elements that make up this address.
     * @param idx element index
     * @return element at {@code idx}
     * @throws IllegalArgumentException if {@code idx} is negative or greater than the number of elements that make up this address
     */
    public getElement(idx: number): string {
        Validate.isTrue(idx >= 0 && idx < this.addressElements.length);

        return this.addressElements[idx];
    }

    /**
     * Adds elements to the end of this address. Equivalent to calling {@code appendSuffix(Address.of(elements)}.
     * @param elements elements to appendSuffix
     * @return copy of this address with {@code elements} appended
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public appendSuffix(suffix: string[] | Address): Address {
        Validate.notNullOrUndefined(suffix);
        let elements: string[];
        if (suffix instanceof Array) {
            Validate.noNullOrUndefinedElements(suffix);
            elements = this.addressElements.concat(suffix);
        } else if (suffix instanceof Address) {
            Validate.noNullOrUndefinedElements(suffix.addressElements);
            elements = this.addressElements.concat(suffix.addressElements);
        } else {
            throw Error();
        }

        return new Address(elements);
    }

    /**
     * Returns {@code true} if this address is a prefix of {@code other} or if this address is equal to {@code other}. Otherwise returns
     * {@code false}.
     * <p>
     * For example...
     * {@code isParentOf(Address.of("one", "two"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isParentOf(Address.of("one"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isParentOf(Address.of("one", "two", "three"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isParentOf(Address.of("one", "two", "three", "four"), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isParentOf(Address.of(""), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isParentOf(Address.of("xxxxx", "two"), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isParentOf(Address.of("one", "xxxxx"), Address.of("one", "two", "three"))} returns {@code false}
     * @param other address to check against
     * @return {@code true} if this address is a prefix of {@code child}, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     */
    public isPrefixOf(other: Address): boolean {
        if (other.addressElements.length < this.addressElements.length) {
            return false;
        }

        const arr1: string[] = other.addressElements.slice(0, this.addressElements.length);
        const arr2: string[] = this.addressElements;

        for (let i = arr1.length; i--;) {
            if (arr1[i] !== arr2[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Removes elements in {@code prefix} from the beginning of this address.
     * <p>
     * For example ...
     * <ul>
     * <li>{@code Address.of("one", "two").removeParent("one")} will return {@code Address.of("two")}.</li>
     * <li>{@code Address.of("one", "two").removeParent("one", "two")} will return {@code Address.of()}.</li>
     * <li>{@code Address.of("xxx").removeParent("one", "two")} will throw an exception.</li>
     * </ul>
     * @param prefix address to remove from the beginning of this address
     * @return copy of this address with the last {@code parent} removed from the beginning
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if this address does not start with {@code parent}, or if the {@code prefix} is equal to
     * {@code parent} (the result would have no elements)
     */
    public removePrefix(prefix: Address): Address {
        Validate.notNullOrUndefined(prefix);
        Validate.isTrue(prefix.isPrefixOf(this));

        const subList: string[] = this.addressElements.slice(prefix.addressElements.length, this.addressElements.length);
        Validate.isTrue(subList.length > 0);
        return new Address(subList);
    }

    /**
     * Removes a number of address elements from the end of this address. For example, removing {@code 2} address elements from
     * {@code ["test1", "test2", "test3", "test4"]} will result in {@code ["test1", "test2"]}.
     * @param count number of address elements to remove from the tail of this address
     * @return a copy of this address with the last {@code removeCount} address elements removed
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the number of address elements in this address is less than {@code removeCount}, or if the
     * {@code removeSuffix >= 0 || removeSuffix < this.length} (if {@code removeSuffix == this.length) the result would have no elements)
     */
    public removeSuffix(count: number): Address {
        Validate.isTrue(count >= 0 && count < this.addressElements.length);
        const subList: string[] = this.addressElements.slice(0, this.addressElements.length - count);
        return new Address(subList);
    }

    hashCode(): number {
        let hash: number = 3;
        hash = 79 * hash + List<string>(this.addressElements).hashCode();
        return hash;
    }

    equals(other: any): boolean {
        if (other === null || other === undefined) {
            return false;
        }
        if (!(other instanceof Address)) {
            return false;
        }

        const addressElementList1: List<string> = List<string>(this.addressElements);
        const addressElementList2: List<string> = List<string>(other.addressElements);
        if (!addressElementList1.equals(addressElementList2)) {
            return false;
        }
        return true;
    }

    public toString(): string {
        return this.addressElements.map(x => Address.escapeElement(x)).join(String.fromCharCode(DELIM));
    }
}
