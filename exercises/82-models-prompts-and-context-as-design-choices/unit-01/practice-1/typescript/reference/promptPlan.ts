/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. */

export const MIN_CACHEABLE = 512; // tokens: a shorter prefix cannot be cached

export const tokens = (text: string): number => Math.ceil(text.length / 4); // one token per four characters, rounded up

function fill(text: string, variables: Record<string, string>): string {
  return text.replace(/\{(\w+)\}/g, (_match, name: string) => {
    if (!(name in variables)) throw new Error(`missing variable: ${name}`);
    return String(variables[name]);
  });
}

export function assemble(modules: any[], variables: Record<string, string>, budget: number): any {
  const stat = modules.filter((m) => m.static);
  const dynamic = modules.filter((m) => !m.static);
  for (const m of stat) {
    if (/\{\w+\}/.test(m.text)) throw new Error(`static module ${m.name} holds a variable, which would break the cache`);
  }
  const kept = dynamic.map((m) => ({ name: m.name, text: fill(m.text, variables), priority: m.priority ?? 0 }));
  const blocks = stat.map((m) => ({ name: m.name, text: m.text }));
  const prefix = blocks.reduce((sum, b) => sum + tokens(b.text), 0);
  const dropped: string[] = [];
  while (prefix + kept.reduce((sum, k) => sum + tokens(k.text), 0) > budget) {
    if (kept.length === 0) throw new Error("over budget: the static modules alone exceed it");
    let victim = 0;
    kept.forEach((k, i) => {
      if (k.priority < kept[victim].priority || (k.priority === kept[victim].priority && i > victim)) victim = i;
    });
    dropped.push(kept.splice(victim, 1)[0].name);
  }
  const used = prefix + kept.reduce((sum, k) => sum + tokens(k.text), 0);
  blocks.push(...kept.map((k) => ({ name: k.name, text: k.text })));
  return { blocks, tokens: used, dropped, breakpoint: stat.length > 0 && prefix >= MIN_CACHEABLE ? stat.length - 1 : null };
}

export function chooseModel(workload: any, models: any[]): string | null {
  const fit = models.filter((m) => m.tier >= workload.tier && m.latency_ms <= workload.max_latency_ms);
  if (fit.length === 0) return null;
  return [...fit].sort((a, b) => a.price_out - b.price_out || (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))[0].name;
}

export function reusablePrefix(a: any, b: any): number {
  const ia = a.breakpoint;
  const ib = b.breakpoint;
  if (ia === null || ib === null || ia !== ib) return 0;
  const same = a.blocks.slice(0, ia + 1).every((block: any, i: number) => block.name === b.blocks[i].name && block.text === b.blocks[i].text);
  return same ? a.blocks.slice(0, ia + 1).reduce((sum: number, block: any) => sum + tokens(block.text), 0) : 0;
}
