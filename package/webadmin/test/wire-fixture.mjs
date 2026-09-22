// The public web API's list boundary: JSON arrays, singleton values, aliases and
// unaliased XStream class wrappers. Production uses platform.api.asList.
export function asList(value, key) {
    if (value == null || value === '') return [];
    if (key && typeof value === 'object' && !Array.isArray(value)) {
        if (value[key] !== undefined) value = value[key];
        else {
            const keys = Object.keys(value).filter(k => !k.startsWith('@'));
            if (keys.length === 1 && (keys[0].split('.').at(-1).toLowerCase() === key.toLowerCase()
                || Array.isArray(value[keys[0]]))) value = value[keys[0]];
        }
    }
    return value == null || value === '' ? [] : Array.isArray(value) ? value : [value];
}

