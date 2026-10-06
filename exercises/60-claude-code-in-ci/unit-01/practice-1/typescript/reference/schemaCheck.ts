/** Provided: the errors of a value against the JSON Schema subset the structured outputs support. Do not edit. */
export type Schema = { [key: string]: any };

const isKind = (kind: string, value: unknown): boolean =>
  kind === "object" ? typeof value === "object" && value !== null && !Array.isArray(value)
    : kind === "array" ? Array.isArray(value)
    : kind === "string" ? typeof value === "string"
    : kind === "boolean" ? typeof value === "boolean"
    : kind === "null" ? value === null
    : kind === "integer" ? typeof value === "number" && Number.isInteger(value)
    : kind === "number" ? typeof value === "number" : false;

/** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
export function schemaCheck(value: any, schema: Schema, path = "$"): string[] {
  const wants: string[] = Array.isArray(schema.type) ? schema.type : schema.type ? [schema.type] : [];
  if (wants.length > 0 && !wants.some((k) => isKind(k, value))) return [`${path}: expected ${wants.join(" or ")}`];
  const errors: string[] = [];
  if (schema.enum && !schema.enum.includes(value)) errors.push(`${path}: ${JSON.stringify(value)} is not one of ${JSON.stringify(schema.enum)}`);
  if (isKind("object", value)) {
    for (const k of schema.required ?? []) if (!(k in value)) errors.push(`${path}.${k}: is required`);
    if (schema.additionalProperties === false) for (const k of Object.keys(value)) if (!(k in (schema.properties ?? {}))) errors.push(`${path}.${k}: is not allowed`);
    for (const [k, sub] of Object.entries(schema.properties ?? {})) if (k in value) errors.push(...schemaCheck(value[k], sub as Schema, `${path}.${k}`));
  }
  if (Array.isArray(value) && schema.items) value.forEach((item, i) => errors.push(...schemaCheck(item, schema.items, `${path}[${i}]`)));
  return errors;
}
