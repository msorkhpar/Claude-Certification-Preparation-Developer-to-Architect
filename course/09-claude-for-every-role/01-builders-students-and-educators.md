# Builders, students and educators

**Level:** Foundations · **Module 9:** Claude for every role · **Page 1 of 2**
**Exams:** AS4, AS6, AS2 (and X: the audience-specific courses go beyond the Associate guide)

**After this page you can** apply the four habits of module 5 (delegate, describe, discern, take responsibility) to a
builder, a student and an educator, and say for each what they hand to Claude, what they must check and what stays
theirs.

Checked against the public descriptions of the Anthropic Academy courses "AI Fluency for Builders", "AI Fluency for
students" and "AI Fluency for educators" (lesson lists and course summaries, read 2026-10-02, no sign-in), and against
the Associate exam guide (domains 2, 4 and 6, version 1.0, July 2026). Only the public course descriptions were read;
the courses' own lesson content was not, and nothing here is taken from it. Every worked example is invented for this
page.

## Why it matters

The Associate exam is written for people in many jobs: operations, marketing, project management, education,
communications. Its scenarios put the same four skills in different settings, and the right answer often depends on
what the person in the scenario owns. A student owns their learning, a builder owns what ships, an educator owns the
assessment. This module shows the shared ideas flexing; it is not tested as a list of audiences, and no blueprint
names it, which is why it carries code X beyond the blueprint.

## The idea

### The shared frame

Anthropic's AI Fluency material names four competencies: **Delegation, Description, Discernment and Diligence** (the "4D
framework"). Module 5 taught the same four as habits under plainer names:

| Official name | The course's habit | The question it asks |
|---|---|---|
| Delegation | Delegate | Which parts of this job go to Claude, and which stay with me? |
| Description | Describe | Have I said what I want, so the job can be done and judged? |
| Discernment | Discern | Is what came back accurate, complete, fair and fit for its reader? |
| Diligence | Take responsibility | Will I stand behind this, tell people honestly what Claude did, and have I protected the data? |

The audience courses reuse this frame and change where the weight falls. Description and Discernment form an inner loop
(you describe, judge the result, describe again); Delegation and Diligence frame it from outside (decide what to hand
over, then own what you ship). The rest of this page, and the next, show how the weight shifts by role. The
examples of what each person does are this course's own illustrations of the idea, not the courses' content.

### Builders

The public course for builders covers how AI works and its capabilities and limits, then delegation "and the builder's
toolkit", description for building well, discernment for builders (including code and user experience), and standing
behind what you build. A builder is someone who owns the whole arc from a customer's problem to a shipped solution.

- **Delegates:** first drafts of code and tests, boilerplate, explanations of unfamiliar code, user-interview
  summaries, variations of an interface text, a first pass at a requirements list.
- **Describes:** the problem and the constraints, not only the task: who the user is, what already exists, what must
  not change, what "done" looks like, and an example of acceptable output.
- **Checks (discernment):** that code runs and passes tests, that it does what the description meant, and that a
  generated interface works for a real user; the courses name code and user experience as the two places a builder
  must look hardest. Claude can produce plausible code that does the wrong thing or an interface that looks right and
  confuses people.
- **Owns (diligence):** the shipped result. If it fails in production, "the AI wrote it" is not an answer. Security,
  licences and the data used in prompts are the builder's to watch.

*Exam angle:* a scenario about a team shipping an AI-assisted feature is usually testing whether the person verified
and owns the output, or trusted a fluent answer. The builder who turns a first draft into a tested, reviewed change
is doing the right thing; the one who ships without running it is the wrong option.

### Students

The public course for students teaches the framework and then AI as a learning partner, AI in career planning and "being
the human in the loop". The aim stated on the page is to develop skills that enhance learning, career planning and
academic success through responsible collaboration.

- **Delegates:** explanations of a hard idea at a lower level, practice questions, feedback on a draft's structure,
  brainstorming topics, a study plan, mock interviews.
- **Does not delegate:** the thinking the course exists to build. If the assignment is to analyse, writing the analysis
  with Claude and submitting it defeats the point and may breach the institution's rules.
- **Describes:** their level, the task, what they already understand and what kind of help they want ("ask me
  questions before explaining").
- **Checks:** every fact and citation that goes into graded work; whether an explanation is actually correct; whether the
  tool is leading them to a conclusion rather than helping them reach one.
- **Owns:** the work submitted, honesty about help received under the rules of their course, and what they learned.

*Exam angle:* disclosure and appropriate use. When the setting has a rule about AI help, follow it; when it has none,
say what you did. A student who asks Claude to explain and quiz them is using it as a partner; one who submits its
essay as their own is not.

### Educators

The public courses cover applying the framework to teaching practice and institutional strategy, to course design and
learning outcomes, and to learning materials and assignments. A separate course serves pK-12 educators, aimed at
supporting student learning while staying true to the educator's mission.

- **Delegates:** draft lesson plans, example problems at several levels, rubric first drafts, differentiated versions of
  a reading, feedback phrasing, administrative correspondence.
- **Describes:** the learning outcome first, then the audience, the time available and the standard; an outcome
  stated clearly is what makes a draft assessable.
- **Checks:** accuracy of the content taught, bias or stereotypes in examples, reading level, alignment with the
  stated outcome, and whether an assignment can be completed by pasting it into an AI (and whether that matters).
- **Owns:** what students are taught, how they are assessed and the fairness of grades. Judging student work and
  decisions with consequences for a student stay with the educator, and the usage policy treats academic testing and
  admissions as a high-risk use needing human review (module 10).
- **Protects:** student data. Names, grades and records fall under the organisation's rules, and anonymising comes
  before uploading (module 10).

*Exam angle:* a scenario where an AI-generated rubric or lesson is used as is tests discernment (is it accurate and
aligned?) and ownership.

### What changes between the three

| | Builder | Student | Educator |
|---|---|---|---|
| Weight falls on | Discernment of code and user experience | Delegation: not handing over the learning | Description of outcomes; diligence for student data |
| The costly mistake | Shipping unrun output | Submitting AI work as one's own thinking | Using an unchecked draft or exposing student records |
| Natural check | Run it, test it, review it | Verify facts and citations, explain it back | Check accuracy, bias and alignment to the outcome |

## Traps

1. **Assuming one set of rules fits every role.** The four habits are shared, but what each person owns differs, and
   the right answer in a scenario depends on that owner.
2. **Treating a fluent first draft as finished work.** Whatever the role, the draft is the start of the judging.
3. **Skipping the rules of the setting.** A course, a school or a company may state what AI use is allowed and what
   must be disclosed; those rules come before the tool's convenience.

## Quiz

1. A developer asks Claude for a data-import function, pastes it into the codebase and ships it after a glance because
   it reads cleanly. Which habit did she skip?
   - **a**: Responsibility: tell the users that an AI assisted with the release
   - **b**: Delegation: hand the whole function to a different person to write instead
   - **c**: Description: phrase the request as a longer, more detailed paragraph
   - **d**: Discernment: execute it against realistic cases and inspect the results

2. A university student must compose an analytical essay for a graded course that allows AI help only for planning. Which
   use fits best?
   - **a**: Ask Claude to probe the outline, then do the analysing personally
   - **b**: Ask Claude for the full essay, then edit the style to sound personal
   - **c**: Have Claude write the argument, and cite it as a source
   - **d**: Paste the essay prompt and submit the response with minor tweaks

3. A teacher asks Claude for a quiz on a unit and gets ten questions. Which step best reflects discernment and
   ownership?
   - **a**: Share it with the class so the pupils can find any faults
   - **b**: Trust it, because the model wrote a similar quiz last month
   - **c**: Check the answers and their fit with the stated outcome
   - **d**: Ask Claude to rate how correct its own questions are

<details>
<summary>Answer key</summary>

1. **d**. The builder's costly mistake is "Shipping unrun output", and the natural check is to "Run it, test it, review it". *b* is ruled out because "A builder is someone who owns the whole arc from a customer's problem to a shipped solution", so the function is hers to verify. *c* is ruled out because the weight for builders falls on "Discernment of code and user experience", and a longer request does not test the result. *a* is ruled out because telling users is honest but does not test anything, and the natural check is to "Run it, test it, review it".
2. **a**. A student uses Claude as a partner by questioning the outline while the thinking remains theirs. *b* is ruled out because "the thinking the course exists to build" must not be handed over, and editing style does not return it. *c* is ruled out because "writing the analysis with Claude and submitting it defeats the point", whatever the citation says. *d* is ruled out because "submits its essay as their own" is the example of misuse given, and minor tweaks do not change that.
3. **c**. Discernment is judging accuracy and alignment with the stated outcome, and the educator owns what students are taught. *b* is ruled out because the educator "Checks: accuracy of the content taught", and a past good quiz does not show this one is right. *a* is ruled out because the educator "Owns: what students are taught, how they are assessed and the fairness of grades", and passing the checking to pupils gives that up. *d* is ruled out because asking Claude to mark itself is not the check, and the page lists "accuracy of the content taught" among the things the educator checks personally.

</details>
