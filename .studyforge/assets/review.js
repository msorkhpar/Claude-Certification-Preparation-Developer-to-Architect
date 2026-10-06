/* A spaced-review bank: show what is due, grade it in the page, and schedule what comes next.

   ⭐ **Graded here, from the key the page carries** (`{question id: {"key": option id, "says":
   {option id: sentence}}}`, written by `render/page/quiz.py`), and **scheduled here, from the plan
   block** (`{"intervals_days": [..]}`): no request, no server, no model, the same over `file://`
   and served (R8). With no script every question is shown and nothing is checked.

   ⭐ **The rule, in full.** Each item keeps a `streak` (right answers in a row) and the `last` day it
   was got right, in whole local days. An item is DUE when it has never been seen, or its streak is
   0, or `today - last` has reached `intervals_days[min(streak, steps) - 1]`. A right answer raises
   the streak and sets `last` to today; a wrong one resets the streak to 0, so the item is due
   again at once. `exercise.quiz.review.due` is the same rule in Python, and a browser test reads
   this page against it.

   ⛔ **The schedule is kept in the reader's own browser** (`studyforge.review.v1`, per bank) behind
   a guard that tolerates a refused store: where the browser refuses it, the bank still works and
   simply treats every item as new on reload. Nothing is ever sent anywhere.

   ⭐ Every word this file says is read off the markup, where Python put it. The shared
   `practice-quiz.js` finds no `practice` part named `key` in this section and does nothing to it,
   so the two scripts never both act on one bank. */

(function () {
  'use strict';

  var BANK = 'section[data-review]';
  var QUESTION = 'data-practice-question';
  var PART = 'data-review-part';
  var ACT = 'data-review-act';
  var VERDICT = 'data-review-verdict';
  var STORE_KEY = 'studyforge.review.v1';
  var WIRED = 'reviewWired';
  var DAY = 86400000;

  function part(root, name) { return root.querySelector('[' + PART + '="' + name + '"]'); }
  function act(root, name) { return root.querySelector('[' + ACT + '="' + name + '"]'); }
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
      var value = JSON.parse(block.textContent);
      return value && typeof value === 'object' && !Array.isArray(value) ? value : null;
    } catch (error) { return null; }
  }

  /* Whole LOCAL days since the epoch, so a day turns at the reader's midnight. */
  function today() { return Math.floor((Date.now() - new Date().getTimezoneOffset() * 60000) / DAY); }

  /* ⭐ The rule, once. `item` is `{streak, last}` or nothing for an item never seen. */
  function isDue(plan, item, day) {
    if (!item || !(item.streak > 0)) { return true; }
    var step = Math.min(item.streak, plan.intervals_days.length) - 1;
    return day - item.last >= plan.intervals_days[step];
  }

  function load() {
    try {
      var raw = window.localStorage.getItem(STORE_KEY);
      var parsed = raw ? JSON.parse(raw) : null;
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
    } catch (error) { return {}; }
  }

  function save(all) {
    try { window.localStorage.setItem(STORE_KEY, JSON.stringify(all)); } catch (error) { return; }
  }

  function wire(bank) {
    if (bank.dataset[WIRED]) { return; }
    var plan = json(bank, 'plan');
    var key = json(bank, 'key');
    var check = act(bank, 'check');
    var again = act(bank, 'again');
    var reset = act(bank, 'reset');
    var controls = part(bank, 'controls');
    if (!plan || !key || !check || !controls || !Array.isArray(plan.intervals_days)) { return; }
    bank.dataset[WIRED] = 'true';
    var id = bank.getAttribute('data-practice-quiz');
    var questions = [].slice.call(bank.querySelectorAll('[' + QUESTION + ']'));
    var summary = part(bank, 'summary');
    var status = part(bank, 'status');
    var everything = load();
    var items = (everything[id] && typeof everything[id] === 'object') ? everything[id] : {};

    function qid(question) { return question.getAttribute(QUESTION); }

    function clear(question) {
      question.removeAttribute(VERDICT);
      var says = part(question, 'says');
      if (says) { says.textContent = ''; says.hidden = true; }
      [].slice.call(question.querySelectorAll('input[type="radio"]')).forEach(function (one) {
        one.checked = false;
        one.disabled = false;
      });
    }

    /* Show the questions that are due and hide the rest; say what is left and when. */
    function present() {
      var day = today();
      var due = questions.filter(function (q) { return isDue(plan, items[qid(q)], day); });
      var unseen = questions.every(function (q) { return !items[qid(q)]; });
      questions.forEach(function (q) { q.hidden = due.indexOf(q) < 0; clear(q); });
      if (due.length) {
        summary.textContent = unseen
          ? fill(words(summary, 'data-review-new'), { total: questions.length })
          : fill(words(summary, 'data-review-due'), { due: due.length, total: questions.length });
      } else {
        var wait = Math.min.apply(null, questions.map(function (q) {
          var item = items[qid(q)];
          var step = Math.min(item.streak, plan.intervals_days.length) - 1;
          return item.last + plan.intervals_days[step] - day;
        }));
        summary.textContent = fill(words(summary, 'data-review-none'), { days: Math.max(wait, 1) });
      }
      status.textContent = '';
      check.hidden = due.length === 0;
      again.hidden = true;
    }

    function chosen(question) {
      var picked = question.querySelector('input[type="radio"]:checked');
      return picked ? picked.value : undefined;
    }

    function grade() {
      var day = today();
      var shown = questions.filter(function (q) { return !q.hidden; });
      var answered = 0;
      var right = 0;
      shown.forEach(function (question) {
        var answer = chosen(question);
        var entry = key[qid(question)];
        var says = part(question, 'says');
        if (answer === undefined || !entry || !entry.says
            || !Object.prototype.hasOwnProperty.call(entry.says, answer)) { return; }
        answered += 1;
        var good = answer === entry.key;
        if (good) { right += 1; }
        items[qid(question)] = good
          ? { streak: ((items[qid(question)] || {}).streak || 0) + 1, last: day }
          : { streak: 0, last: day };
        question.setAttribute(VERDICT, good ? 'correct' : 'wrong');
        if (says) {
          says.textContent = words(says, good ? 'data-review-right-word' : 'data-review-wrong-word') + ' ';
          says.insertAdjacentHTML('beforeend', entry.says[answer]);
          says.hidden = false;
        }
        [].slice.call(question.querySelectorAll('input[type="radio"]')).forEach(function (one) {
          one.disabled = true;
        });
      });
      if (!answered) { status.textContent = words(status, 'data-review-blank'); return; }
      everything[id] = items;
      save(everything);
      status.textContent = fill(words(status, 'data-review-right'), {
        right: right, asked: shown.length
      });
      check.hidden = true;
      again.hidden = false;
    }

    check.addEventListener('click', grade);
    again.addEventListener('click', present);
    if (reset) {
      reset.addEventListener('click', function () {
        items = {};
        delete everything[id];
        save(everything);
        present();
      });
    }
    var offline = part(bank, 'offline');
    if (offline) { offline.hidden = true; }
    controls.hidden = false;
    present();
  }

  [].slice.call(document.querySelectorAll(BANK)).forEach(wire);
}());
