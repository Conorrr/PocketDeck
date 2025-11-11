export function capitalise(input: string | undefined) {
    if (!input || input.length < 1) {
        return input;
    }
    return input.substring(0, 1).toUpperCase() + input.substring(1);
}