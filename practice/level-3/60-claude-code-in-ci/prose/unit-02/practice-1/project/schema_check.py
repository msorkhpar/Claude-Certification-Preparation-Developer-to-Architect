"""Provided: the errors of a value against the JSON Schema subset the structured outputs support. Do not edit."""


def schema_check(value, schema, path="$"):
    """Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false."""
    kinds = {"object": dict, "array": list, "string": str, "boolean": bool, "null": type(None)}
    want = schema.get("type")
    wants = want if isinstance(want, list) else [want] if want else []
    ok = not wants
    for kind in wants:
        if kind == "integer":
            ok = ok or (isinstance(value, int) and not isinstance(value, bool))
        elif kind == "number":
            ok = ok or (isinstance(value, (int, float)) and not isinstance(value, bool))
        else:
            ok = ok or isinstance(value, kinds[kind])
    if not ok:
        return [f"{path}: expected {' or '.join(wants)}"]
    errors = []
    if "enum" in schema and value not in schema["enum"]:
        errors.append(f"{path}: {value!r} is not one of {schema['enum']}")
    if isinstance(value, dict):
        errors += [f"{path}.{k}: is required" for k in schema.get("required", []) if k not in value]
        if schema.get("additionalProperties") is False:
            errors += [f"{path}.{k}: is not allowed" for k in value if k not in schema.get("properties", {})]
        for k, sub in schema.get("properties", {}).items():
            if k in value:
                errors += schema_check(value[k], sub, f"{path}.{k}")
    if isinstance(value, list) and "items" in schema:
        for i, item in enumerate(value):
            errors += schema_check(item, schema["items"], f"{path}[{i}]")
    return errors
