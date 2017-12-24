export class Validate {
    private constructor() {
    }

    public static notNullOrUndefined(object: any | undefined | null, message?: string): void {
        if (object === null || object === undefined) {
            throw new Error(message);
        }
    }

    public static noNullOrUndefinedElements(object: any[], message?: string): void {
        for (const element of object) {
            if (element === null || element === undefined) {
                throw new Error(message);
            }
        }
    }

    public static isTrue(expression: boolean , message?: string): void {
        if (expression === false) {
            throw new Error(message);
        }
    }
}
