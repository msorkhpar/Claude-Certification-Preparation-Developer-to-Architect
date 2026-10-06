export const meta = {
  name: 'quiz-quality-batch',
  description: 'Audit and rework weak quiz items for one batch of course modules, with an independent reader and a fix loop',
  phases: [
    { title: 'Modules', detail: 'author audits and reworks each module, a reader judges, fix and re-read up to twice' },
    { title: 'Mocks', detail: 'the same loop, one agent per mock exam page, after the level modules' },
    { title: 'Resolve', detail: 'items still weak: fix or restore the base text, judge against the base, revert any that got worse' },
  ],
}

const A = args
const REPO = A.repo, SCRATCH = A.scratch, BASE = A.base

const RUBRIC = `
QUALITY RUBRIC (judge every item: page quiz q, module quiz m, mock or pool item x)
An item PASSES when a learner who understood the page can reach the key by reasoning about the scenario, and a learner who only skimmed cannot pick it by its form.
R1 Derivation over recall. The key is reached by applying the page's idea to the scenario, not by spotting a near-verbatim page sentence. A key that copies a page sentence, or a stem that only asks "what does the page say", is WEAK: recast the key as the concrete action or explanation for this scenario.
R2 Plausible distractors. Each wrong option is a mistake a real practitioner would make: a real technique that is right in a different situation, a misconception the page addresses, or a fix aimed at the symptom instead of the cause. No strawmen: nothing absurd or extreme ("anywhere in the world", "without any involvement at all", "every single message"), no option that contradicts the stem, no arbitrary filler.
R3 Parallel form. The four (or five) options match in length, grammar, specificity and shape. The key is not noticeably the longest or shortest, nor the only composite ("X, then Y"), only hedged, only conditional, only one with a colon or a contrast, nor the only specific and bounded one. No mirror pair (two options that differ by one word or a negation, one of them the key).
R4 No leak. The stem does not paraphrase the key, state the rule the key applies, or carry a word that names the key's quality.
R5 One best answer, and correct. The key is right according to the page; no distractor is also defensible (for example a legitimate mitigation). An item that rests on the course's own design or practice rule rather than documented product behaviour says so in the stem (for example "In this module's example, ...").
R6 Rule-outs that work. Each wrong option's explanation quotes a page passage (verbatim, 4 words or more) that makes THAT option false, not one that only names the topic or repeats the key's quote.
R7 No near-duplicate of another item of the module, or of an item the QUIZ-POLISH list names as near.
Verdicts: PASS (leave the item byte-identical), WEAK (rework), FAIL (rework).`

const CONSTRAINTS = `
MINIMAL-CHANGE CONSTRAINTS (hard rules)
- Edit only the quiz sections of the pages in your scope ("## Quiz", "## Module quiz", "## Mock exam", each with its <details> answer key) and, through the builder below, exercises/<module>/tests/quiz.json. Never hand-edit quiz.json.
- Lesson prose: never change or delete an existing sentence (other quizzes and mock items quote lesson sentences verbatim). Only when no existing passage can rule out a needed distractor, and no other plausible distractor can be ruled out by existing text, you may ADD one short sentence to the lesson prose of that page, stating only what the page already states or directly implies. Do it rarely. Never touch code blocks, example output blocks, tables, headings, links, the header lines (Level, Module, Exams, After this page, Checked on) or the practice section.
- Leave PASS items byte-identical. For WEAK/FAIL items change only what fixes the weakness: often one or two options with their rule-outs, or a stem clause. Do not rewrite a whole item when one option is the problem.
- Keep, for every item: its number and position, the count of items per section, the key LETTER (write new option text into the same letter slots), the scope, the "(Select two.)" form with five options a-e, any scenario label ("Scenario S1, ..."), and for mock or pool items the module and page the item is drawn from (the "(module N, page M)" reference in the key's explanation and the pages tables depend on it; the item's position fixes its domain).
- Never introduce a product fact that is not stated on the pages in scope, and keep the key correct according to the page. Do not use outside knowledge to change claims; if you think a page claim is wrong, report it under concerns instead.
- Course conventions: the page's own spelling (British), they/them for unnamed people, no source names, no URLs, no personal data, no record of who said what. Plain markdown only inside items: no links, no HTML, no line breaks inside an option, backticks only for code identifiers as the page already does.
- Format: options are "   - **a**: text" with no final period. The folded key paragraph is "N. **c**. Why the key is best. *a* is ruled out because ... \\"verbatim quote\\" .... *b* is ruled out because ...", one sentence per wrong option, in the order the page already uses. Every quoted phrase of 4 words or more must be verbatim in the lesson prose (page quiz: that page; module quiz: any page of the module; mock or pool item: the pages of its level scope, outside quiz sections).
- Other agents edit other modules in the same working tree at the same time: never run a git command that writes (no add, commit, checkout, switch, stash, reset, restore, clean), never run tools/build_quiz_json.py (it rewrites every module), and never edit files of another module, tools/, examples/ or docs/. Read-only git (diff, show, log, grep) is fine.`

const CHECKS = (prefix) => `
CHECKS (run after your edits, repeat until clean)
1. python3 ${SCRATCH}/build_one.py ${REPO} ${prefix}      (rebuilds only this module's quiz.json from the pages)
2. python3 tools/check_quiz.py ${prefix}      (must report the module "ok" with no finding; keep any "key is the longest" warning share at or below 30 percent)
3. python3 ${SCRATCH}/check_invariants.py ${REPO} ${BASE} ${prefix}      (must exit 0: ids, order, page, scope, key letter, select count and option letters unchanged)
4. git -C ${REPO} diff ${BASE} -- course/${prefix}* : confirm every change is inside a quiz section, or is one of your allowed one-sentence prose additions.
Work from ${REPO} as the current directory.`

const AUTHOR_SCHEMA = {
  type: 'object',
  properties: {
    scope: { type: 'string' },
    items: { type: 'array', items: { type: 'object', properties: {
      id: { type: 'string' },
      verdict_before: { type: 'string', enum: ['PASS', 'WEAK', 'FAIL'] },
      edited: { type: 'boolean' },
      reasons: { type: 'array', items: { type: 'string' } },
      change: { type: 'string' },
    }, required: ['id', 'verdict_before', 'edited'] } },
    prose_additions: { type: 'array', items: { type: 'object', properties: {
      page: { type: 'string' }, sentence: { type: 'string' }, why: { type: 'string' } }, required: ['page', 'sentence', 'why'] } },
    checks: { type: 'string', description: 'last output line of each of the four checks' },
    concerns: { type: 'array', items: { type: 'string' }, description: 'doubtful page claims, items you could not fix within the rules, anything a site builder should know' },
  },
  required: ['scope', 'items', 'prose_additions', 'checks', 'concerns'],
}

const READER_SCHEMA = {
  type: 'object',
  properties: {
    scope: { type: 'string' },
    verdicts: { type: 'array', items: { type: 'object', properties: {
      id: { type: 'string' },
      changed: { type: 'boolean' },
      verdict: { type: 'string', enum: ['PASS', 'WEAK', 'FAIL'] },
      reason: { type: 'string' },
      fix: { type: 'string', description: 'a concrete repair when not PASS' },
    }, required: ['id', 'changed', 'verdict'] } },
    prose_ok: { type: 'boolean', description: 'false if any lesson prose outside quiz sections was changed beyond allowed one-sentence additions, or an addition states something the page does not support' },
    prose_notes: { type: 'string' },
    checks_ok: { type: 'boolean' },
  },
  required: ['scope', 'verdicts', 'prose_ok', 'checks_ok'],
}

function authorPrompt(u) {
  return `You are improving the quality of quiz items in a certification-preparation course (repository at ${REPO}). Scope: ${u.desc}.
Goal: every item in scope meets the rubric, with the smallest change that gets it there. Many items already pass; leave those untouched.
${RUBRIC}
${CONSTRAINTS}
${CHECKS(u.prefix)}
PROCESS
1. Read docs/process/QUIZ-POLISH.md and note every entry for this scope (ids are page slug and question: q page quiz, m module quiz, x mock item). These are known weak items with the reason a reader gave; fix them unless you find the reason no longer applies.
2. Read ${u.readHint} in full, lesson prose and quiz sections both.
3. Judge every item in scope with the rubric. Be strict on R1 to R3: a key that is a page sentence, a distractor nobody would pick, a key that stands out by length or shape.
4. Rework the WEAK and FAIL items within the constraints. For each new distractor, find the verbatim page passage that rules it out before you write it.
5. Run the checks until clean. If a check flags an item you did not touch, fix it the same minimal way and say so.
6. Return the report: every item in scope with its verdict before your work, whether you edited it, the reasons, and a one-line description of the change; the prose additions; the checks' last lines; concerns.`
}

function readerPrompt(u, author) {
  const edited = (author?.items || []).filter(i => i.edited).map(i => i.id)
  return `You are an independent quiz reader for a certification-preparation course (repository at ${REPO}). You are not the author, and you edit nothing.
Scope: ${u.desc}. Another agent has just reworked some quiz items in this scope; the base commit before the work is ${BASE}.
See the change with: git -C ${REPO} diff ${BASE} -- course/${u.prefix}*   (the quiz.json under exercises/${u.prefix}*/tests is generated from the pages).
Items the author reports as edited: ${edited.join(', ') || '(none)'}. Prose additions reported: ${JSON.stringify(author?.prose_additions || [])}.
${RUBRIC}
The author worked under these constraints, which you also enforce:
${CONSTRAINTS}
YOUR TASK
1. Read ${u.readHint} in full (the current version).
2. Judge EVERY edited item strictly against the rubric, and compare it with its base version: it must be better, the key must still be correct according to the page, it must be answerable from the page, and every quoted rule-out must actually make its own option false. A rework that trades one tell for another (for example the key becomes the shortest, or the only option without a reason clause) is WEAK.
3. Judge every item the author left unchanged too, more quickly: report any that is clearly WEAK or FAIL by R1 to R3 or R5 (verdict, reason, fix); you may report the unchanged items that pass as a single PASS entry each or omit them.
4. Check that no lesson prose changed except allowed one-sentence additions that the page supports (prose_ok).
5. Run, read-only: python3 tools/check_quiz.py ${u.prefix} and python3 ${SCRATCH}/check_invariants.py ${REPO} ${BASE} ${u.prefix} from ${REPO}. checks_ok is true only when the scope's items have no finding and the invariants hold.${u.mock ? ' Another agent is working on another page of the same module at the same time: judge only findings for the items of your page.' : ''}
For every non-PASS verdict give a concrete fix: which option or clause to change, to what kind of text, and the page passage that would rule it out.`
}

function fixPrompt(u, reader, round) {
  const todo = (reader?.verdicts || []).filter(v => v.verdict !== 'PASS')
  return `You are improving quiz items in a certification-preparation course (repository at ${REPO}). Scope: ${u.desc}. This is fix round ${round}.
An independent reader judged the current state and found these items not yet good enough:
${JSON.stringify(todo, null, 1)}
Prose: ${reader?.prose_ok === false ? 'NOT OK: ' + (reader?.prose_notes || '') + ' Restore or correct it.' : 'ok'}. Checks ok: ${reader?.checks_ok}.
Repair each listed item (you may use a better repair than the one suggested), keeping every other item byte-identical.
${RUBRIC}
${CONSTRAINTS}
${CHECKS(u.prefix)}
Read ${u.readHint} first (current version). Return the report for the items you touched (items list may hold only those), the prose additions now present in scope (all of them, including earlier ones), the checks' last lines, and concerns, including any listed item you judge cannot be improved within the rules and why.`
}


const RESOLVE_SCHEMA = {
  type: 'object',
  properties: {
    scope: { type: 'string' },
    items: { type: 'array', items: { type: 'object', properties: {
      id: { type: 'string' }, decision: { type: 'string', enum: ['fixed', 'restored', 'kept'] }, note: { type: 'string' } }, required: ['id', 'decision'] } },
    checks: { type: 'string' },
    concerns: { type: 'array', items: { type: 'string' } },
  },
  required: ['scope', 'items', 'checks'],
}

const JUDGE_SCHEMA = {
  type: 'object',
  properties: {
    scope: { type: 'string' },
    verdicts: { type: 'array', items: { type: 'object', properties: {
      id: { type: 'string' }, verdict: { type: 'string', enum: ['PASS', 'WEAK', 'FAIL'] },
      vs_base: { type: 'string', enum: ['better', 'same', 'worse'] }, reason: { type: 'string' } }, required: ['id', 'verdict', 'vs_base'] } },
    checks_ok: { type: 'boolean' },
  },
  required: ['scope', 'verdicts', 'checks_ok'],
}

function resolvePrompt(u, open) {
  return `You are finishing a quiz quality pass in a certification-preparation course (repository at ${REPO}). Scope: ${u.desc}.
After two fix rounds an independent reader still judges these items not good enough:
${open[0].reason === undefined && A.openFile ? 'Items: ' + open.map(o => o.id).join(', ') + '. Read each item\'s verdict, reason and suggested fix from the JSON file ' + A.openFile + ' (the entry whose unit is ' + u.label + ').' : JSON.stringify(open, null, 1)}
The base version of every page (before the pass) is at commit ${BASE}: git -C ${REPO} show ${BASE}:<path>. For each listed item compare the base text with the current text and decide:
- fixed: the reader's point can be repaired within the rules; repair it (minimal change) so the item passes the rubric and is better than the base.
- restored: the base version is as good as or better than anything you can reach; restore the item's stem, options and key paragraph to the base text exactly (byte-identical to the base).
- kept: the current text is clearly better than the base and the remaining point cannot be repaired within the rules; leave it and say why.
Never leave an item worse than its base version (for example a new near-duplicate of another item, a rule-out weaker than the base one, or a distractor that is now defensible).
${RUBRIC}
${CONSTRAINTS}
${CHECKS(u.prefix)}
Read ${u.readHint} first (current version). Touch only the listed items. Return one entry per listed item, the checks' last lines and concerns.`
}

function judgePrompt(u, ids) {
  return `You are an independent quiz reader for a certification-preparation course (repository at ${REPO}); you edit nothing. Scope: ${u.desc}.
Judge only these items: ${ids.join(', ')}. For each, read the current text and the base text (git -C ${REPO} show ${BASE}:<path>, or git -C ${REPO} diff ${BASE} -- course/${u.prefix}*), and read ${u.readHint}.
Give the rubric verdict of the current text, and vs_base: better, same (including restored byte-identical to base) or worse than the base text. Worse means a reader would rather have the base item: a new tell, a new near-duplicate, a weaker rule-out, a defensible distractor, a key that is no longer clearly correct, or a meaning drift away from what the page teaches.
${RUBRIC}
Run read-only from ${REPO}: python3 tools/check_quiz.py ${u.prefix} and python3 ${SCRATCH}/check_invariants.py ${REPO} ${BASE} ${u.prefix}; checks_ok is true only when the scope's items have no finding and the invariants hold.${u.mock ? ' Another agent may work on another page of the same module: judge only findings for your page.' : ''}`
}

function revertPrompt(u, ids) {
  return `In the repository at ${REPO}, scope ${u.desc}: restore these quiz items to their base text exactly: ${ids.join(', ')}.
For each, take the item's stem line, its option lines and its numbered paragraph in the folded answer key from the base commit (git -C ${REPO} show ${BASE}:<path>) and put them back byte-identical, leaving every other item as it is now.
${CONSTRAINTS}
${CHECKS(u.prefix)}
Return the scope, one entry per item with decision 'restored', and the checks' last lines.`
}

async function resolveUnit(u, open, phase) {
  if (!open.length) return null
  const tag = u.label
  const res = await agent(resolvePrompt(u, open), { label: `resolve ${tag}`, phase, schema: RESOLVE_SCHEMA, effort: 'high' })
  const ids = open.map(o => o.id)
  const judge = await agent(judgePrompt(u, ids), { label: `judge ${tag}`, phase, schema: JUDGE_SCHEMA, effort: 'high', agentType: 'office-review' })
  const worse = (judge?.verdicts || []).filter(v => v.vs_base === 'worse').map(v => v.id)
  let revert = null
  if (worse.length) revert = await agent(revertPrompt(u, worse), { label: `revert ${tag}`, phase, schema: RESOLVE_SCHEMA, effort: 'medium' })
  return { resolve: res, judge, reverted: worse, revert }
}

async function runUnit(u, phase) {
  const tag = u.label
  const author = await agent(authorPrompt(u), { label: `author ${tag}`, phase, schema: AUTHOR_SCHEMA, effort: 'high' })
  let reader = await agent(readerPrompt(u, author), { label: `read ${tag}`, phase, schema: READER_SCHEMA, effort: 'high', agentType: 'office-review' })
  const rounds = []
  for (let round = 1; round <= 2; round++) {
    const open = (reader?.verdicts || []).filter(v => v.verdict !== 'PASS')
    if (reader && !open.length && reader.prose_ok && reader.checks_ok) break
    if (!reader) break
    const fix = await agent(fixPrompt(u, reader, round), { label: `fix${round} ${tag}`, phase, schema: AUTHOR_SCHEMA, effort: 'high' })
    const allEdited = { items: [...(author?.items || []), ...(fix?.items || [])], prose_additions: fix?.prose_additions || author?.prose_additions || [] }
    reader = await agent(readerPrompt(u, allEdited), { label: `reread${round} ${tag}`, phase, schema: READER_SCHEMA, effort: 'high', agentType: 'office-review' })
    rounds.push({ round, fix })
  }
  const open = (reader?.verdicts || []).filter(v => v.verdict !== 'PASS').map(v => ({ id: v.id, verdict: v.verdict, reason: v.reason, fix: v.fix }))
  const resolved = await resolveUnit(u, open, 'Resolve')
  return { unit: tag, author, rounds, finalReader: reader, resolved }
}

function unitOf(label) {
  if (label.includes('/')) {
    const [m, pg] = label.split('/')
    const p = (A.mockPageList || []).find(x => x.module.startsWith(m) && x.page.startsWith(pg))
    return { label, prefix: m, desc: `every mock exam or pool item on the single page course/${p.module}/${p.page}; the other pages of the module are out of scope`,
      readHint: `course/${p.module}/${p.page}, and for each item you judge, the lesson page its explanation names as (module N, page M)`, mock: true }
  }
  return { label, prefix: label.slice(0, 2), desc: `every quiz item (page quizzes and the module quiz) of module ${label}, whose pages are course/${label}/*.md`,
    readHint: `every page of course/${label}/`, mock: false }
}

if (A.resolveOnly) {
  phase('Resolve')
  const out = await pipeline(A.resolveOnly, x => resolveUnit(unitOf(x.unit), x.ids.map(id => ({ id })), 'Resolve').then(r => ({ unit: x.unit, ...r })))
  return { resolveResults: out }
}

phase('Modules')
const moduleUnits = (A.modules || []).map(m => ({
  label: m, prefix: m.slice(0, 2), desc: `every quiz item (page quizzes and the module quiz) of module ${m}, whose pages are course/${m}/*.md`,
  readHint: `every page of course/${m}/`, mock: false,
}))
const moduleResults = await pipeline(moduleUnits, u => runUnit(u, 'Modules'))
log(`modules done: ${moduleResults.filter(Boolean).length} of ${moduleUnits.length}`)

phase('Mocks')
const mockUnits = (A.mockPages || []).map(p => ({
  label: `${p.module.slice(0, 2)}/${p.page.slice(0, 2)}`, prefix: p.module.slice(0, 2),
  desc: `every mock exam or pool item on the single page course/${p.module}/${p.page} (items ${p.page.replace('.md', '')}#x1 onward); the other pages of the module are out of scope`,
  readHint: `course/${p.module}/${p.page}, and for each item you judge, the lesson page its explanation names as (module N, page M)`, mock: true,
}))
const mockResults = await pipeline(mockUnits, u => runUnit(u, 'Mocks'))
return { moduleResults, mockResults }
