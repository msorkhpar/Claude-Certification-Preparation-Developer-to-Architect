/* The mock exam: progress, a way to leave and come back, and a score per domain, in the page.

   ⭐ **Graded here, in the page, from the key the page carries**: no request, no container,
   no model, the same over `file://` and served (R8). The key block is the quiz's own shape
   (`{question id: {"key": option id, "says": {option id: sentence}}}`, written by
   `render/page/quiz.py`), and the plan block holds the pass mark and the domains' titles.

   ⭐ **The rule, in full**: a question is right when the chosen option is its key; the exam
   is submitted only when EVERY question is answered, and a submit with a question open names
   each one by its number and grades nothing; a submitted exam shows each question's verdict and
   the sentence of the option the reader chose, a score per domain and the whole exam's score
   against the declared pass mark, and locks the answers until the reader starts again. A
   percent is rounded DOWN, so 2 of 3 is 66 and never reads as a pass at 67. `exercise.quiz.mock`
   is the same rule in Python, and a browser test reads this page against it.

   ⭐ **Leaving and returning**: every change is kept in the reader's own browser store under
   `studyforge.mock.v1`, per exam, and read back when the page opens, so a reader who closes the
   tab resumes where they were, and a submitted exam reads as submitted. ⛔ Never on the server.
   Where the browser refuses storage the exam still works and simply forgets on reload.

   ⭐ **A passed exam is recorded as passed in the reader's own store** (`study-progress.js`,
   where a quiz's pass is kept), so its card reads *passed* after a reload.

   ⭐ Every word this file says is read off the markup, where Python put it. The shared
   `practice-quiz.js` finds no `practice` part named `key` in this section and does nothing to
   it, which is why the two scripts never both act on one exam. */

(function () {
  'use strict';

  var EXAM = 'section[data-practice-mock]';
  var PART = 'data-mock-part';
  var QUESTION = 'data-practice-question';
  var VERDICT = 'data-practice-verdict';
  var DOMAIN = 'data-mock-domain';
  var SETTLED = 'studyforge:practice-settled';
  var STORE_KEY = 'studyforge.mock.v1';
  var VERSION = 1;

  function part(root, name) { return root.querySelector('[' + PART + '="' + name + '"]'); }
  function words(element, name) { return (element && element.getAttribute(name)) || ''; }
  function fill(template, values) {
    return template.replace(/\{(\w+)\}/g, function (all, name) {
      return Object.prototype.hasOwnProperty.call(values, name) ? String(values[name]) : all;
    });
  }

  function json(root, name) {
    var block = part(root, name);
    if (!block) { return null; }
    try {
      var read = JSON.parse(block.textContent);
      return read && typeof read === 'object' && !Array.isArray(read) ? read : null;
    } catch (error) {
      return null;
    }
  }

  /* The reader's own store, or null where the browser refuses it. ⚠️ Probed with a write:
     a `file://` page in a private window throws on the property access itself. */
  function backing() {
    try {
      var held = window.localStorage;
      held.setItem(STORE_KEY + '.probe', '1');
      held.removeItem(STORE_KEY + '.probe');
      return held;
    } catch (error) {
      return null;
    }
  }

  function everything(store) {
    try {
      var held = JSON.parse(store.getItem(STORE_KEY) || 'null');
      if (held && held.version === VERSION && held.exams && typeof held.exams === 'object') {
        return held;
      }
    } catch (error) { /* an unreadable record is an empty one */ }
    return { version: VERSION, exams: {} };
  }

  function recall(store, name) {
    if (!store) { return { answers: {}, submitted: false }; }
    var kept = everything(store).exams[name];
    var answers = kept && kept.answers && typeof kept.answers === 'object' ? kept.answers : {};
    return { answers: answers, submitted: !!(kept && kept.submitted === true) };
  }

  function keep(store, name, state) {
    if (!store) { return; }
    try {
      var held = everything(store);
      held.exams[name] = { answers: state.answers, submitted: state.submitted };
      store.setItem(STORE_KEY, JSON.stringify(held));
    } catch (error) { /* a full or refused store loses the draft, not the exam */ }
  }

  function wire(exam) {
    var key = json(exam, 'key');
    var plan = json(exam, 'plan');
    var controls = part(exam, 'controls');
    var submit = part(exam, 'submit');
    if (!key || !plan || !controls || !submit) { return; }
    var name = exam.getAttribute('data-practice-quiz');
    var passMark = Number(exam.getAttribute('data-practice-mock'));
    var offline = exam.querySelector('[data-practice-part="offline"]');
    var again = part(exam, 'again');
    var missing = part(exam, 'missing');
    var result = part(exam, 'result');
    var questions = [].slice.call(exam.querySelectorAll('[' + QUESTION + ']'));
    var store = backing();
    var state = recall(store, name);
    if (offline) { offline.hidden = true; }
    controls.hidden = false;

    function radios(question) { return [].slice.call(question.querySelectorAll('input[type="radio"]')); }

    function chosen() {
      var made = {};
      questions.forEach(function (question) {
        var picked = question.querySelector('input[type="radio"]:checked');
        if (picked) { made[question.getAttribute(QUESTION)] = picked.value; }
      });
      return made;
    }

    function restore() {
      questions.forEach(function (question) {
        var wanted = state.answers[question.getAttribute(QUESTION)];
        radios(question).forEach(function (radio) { radio.checked = radio.value === wanted; });
      });
    }

    function count() {
      var answered = Object.keys(chosen()).length;
      var meter = part(exam, 'meter');
      if (meter) { meter.value = answered; }
      var label = part(exam, 'count');
      if (label) {
        label.textContent = fill(words(part(exam, 'progress'), 'data-mock-count'),
          { answered: answered, asked: questions.length });
      }
    }

    function unanswered() {
      var made = chosen();
      return questions.filter(function (question) {
        return made[question.getAttribute(QUESTION)] === undefined;
      });
    }

    function lock(on) {
      questions.forEach(function (question) {
        radios(question).forEach(function (radio) { radio.disabled = on; });
      });
    }

    function percent(right, asked) { return asked === 0 ? 0 : Math.floor((right * 100) / asked); }

    function draw(question, answer) {
      var entry = key[question.getAttribute(QUESTION)];
      var says = question.querySelector('[data-practice-part="says"]');
      var known = entry && entry.says && Object.prototype.hasOwnProperty.call(entry.says, answer);
      if (!known) {
        question.removeAttribute(VERDICT);
        if (says) { says.textContent = ''; says.hidden = true; }
        return false;
      }
      var right = answer === entry.key;
      question.setAttribute(VERDICT, right ? 'correct' : 'wrong');
      if (says) {
        says.textContent = words(says, right ? 'data-practice-right' : 'data-practice-wrong') + ' ';
        says.insertAdjacentHTML('beforeend', entry.says[answer]);
        says.hidden = false;
      }
      return right;
    }

    function clear() {
      questions.forEach(function (question) {
        question.removeAttribute(VERDICT);
        var says = question.querySelector('[data-practice-part="says"]');
        if (says) { says.textContent = ''; says.hidden = true; }
      });
      result.hidden = true;
      exam.removeAttribute('data-mock-passed');
      again.hidden = true;
      submit.hidden = false;
      lock(false);
    }

    function show() {
      var made = chosen();
      var overall = { right: 0, asked: questions.length };
      var byDomain = {};
      questions.forEach(function (question) {
        var right = draw(question, made[question.getAttribute(QUESTION)]);
        if (right) { overall.right += 1; }
        var domain = question.getAttribute(DOMAIN);
        byDomain[domain] = byDomain[domain] || { right: 0, asked: 0 };
        byDomain[domain].asked += 1;
        if (right) { byDomain[domain].right += 1; }
      });
      var reached = overall.asked > 0 && percent(overall.right, overall.asked) >= passMark;
      var line = part(exam, 'overall');
      line.textContent = fill(words(line, reached ? 'data-mock-reached' : 'data-mock-short'), {
        right: overall.right, asked: overall.asked,
        percent: percent(overall.right, overall.asked), pass: passMark
      });
      var body = part(exam, 'domains').querySelector('tbody');
      body.textContent = '';
      var template = words(part(exam, 'domains'), 'data-mock-domain-words');
      (plan.domains || []).forEach(function (domain) {
        var score = byDomain[domain.id] || { right: 0, asked: 0 };
        var row = document.createElement('tr');
        var title = document.createElement('th');
        title.scope = 'row';
        title.textContent = domain.title;
        var cell = document.createElement('td');
        cell.textContent = fill(template, {
          right: score.right, asked: score.asked, percent: percent(score.right, score.asked)
        });
        row.appendChild(title);
        row.appendChild(cell);
        body.appendChild(row);
      });
      exam.setAttribute('data-mock-passed', reached ? 'true' : 'false');
      result.hidden = false;
      missing.hidden = true;
      submit.hidden = true;
      again.hidden = false;
      lock(true);
      return reached;
    }

    function settle(reached) {
      var progress = window.studyforge && window.studyforge.progress;
      if (reached && progress) { progress.passQuiz(name); }
      exam.dispatchEvent(new CustomEvent(SETTLED, { bubbles: true }));
    }

    exam.addEventListener('change', function () {
      if (state.submitted) { return; }
      state.answers = chosen();
      keep(store, name, state);
      count();
      if (!missing.hidden && unanswered().length === 0) { missing.hidden = true; }
    });

    submit.addEventListener('click', function () {
      var open = unanswered();
      if (open.length) {
        var numbers = open.map(function (question) { return questions.indexOf(question) + 1; });
        var list = numbers.length > 1
          ? numbers.slice(0, -1).join(', ') + ' and ' + numbers[numbers.length - 1]
          : String(numbers[0]);
        missing.textContent = fill(words(missing, 'data-mock-words'), { list: list });
        missing.hidden = false;
        var first = radios(open[0])[0];
        if (first) { first.focus(); }
        return;
      }
      state.answers = chosen();
      state.submitted = true;
      keep(store, name, state);
      settle(show());
    });

    again.addEventListener('click', function () {
      state = { answers: {}, submitted: false };
      keep(store, name, state);
      restore();
      clear();
      count();
      exam.dispatchEvent(new CustomEvent(SETTLED, { bubbles: true }));
      exam.scrollIntoView();
    });

    restore();
    count();
    if (state.submitted && unanswered().length === 0) {
      show();
    } else if (state.submitted) {
      state = { answers: state.answers, submitted: false };
      keep(store, name, state);
    }
  }

  [].slice.call(document.querySelectorAll(EXAM)).forEach(wire);
}());
