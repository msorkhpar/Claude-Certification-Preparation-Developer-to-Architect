/* Flashcards: flip a card, mark it known or to see again, and keep the marks in this browser.

   ⭐ **The build draws every card with both sides** (`section[data-deck]`, `render/page/deck.py`),
   so with no script the reader reads front then back, in order. This script then hides each back
   behind a Show-the-back button and adds the two marks. ⛔ **Nothing leaves the page**: no request,
   no server, no account. The marks are kept in the reader's own browser under `studyforge.deck.v1`,
   per deck, behind a guard that tolerates a refused store: a reader whose browser refuses it still
   turns and marks cards, and simply starts again on reload.

   ⭐ Every word this file says is read off the markup, where Python put it, as the quiz's are. A
   second copy of this script on one page does nothing to a deck the first already wired. */

(function () {
  'use strict';

  var DECK = 'section[data-deck]';
  var CARD = 'li[data-deck-card]';
  var PART = 'data-deck-part';
  var ACT = 'data-deck-act';
  var STATE = 'data-deck-state';
  var STORE_KEY = 'studyforge.deck.v1';
  var WIRED = 'deckWired';
  var KNOWN = 'known';
  var AGAIN = 'again';

  function part(root, name) { return root.querySelector('[' + PART + '="' + name + '"]'); }
  function words(element, name) { return (element && element.getAttribute(name)) || ''; }
  function fill(template, values) {
    return template.replace(/\{(\w+)\}/g, function (all, name) {
      return Object.prototype.hasOwnProperty.call(values, name) ? String(values[name]) : all;
    });
  }

  /* ⛔ Every read and write of the store is guarded: a private window, cleared site data or a
     blocked store throws, and the deck must still work without it. */
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

  function button(label, act) {
    var made = document.createElement('button');
    made.type = 'button';
    made.setAttribute(ACT, act);
    made.textContent = label;
    return made;
  }

  function wire(deck) {
    if (deck.dataset[WIRED]) { return; }
    deck.dataset[WIRED] = 'true';
    var key = deck.getAttribute('data-practice-quiz');
    var cards = [].slice.call(deck.querySelectorAll(CARD));
    var everything = load();
    var marks = (everything[key] && typeof everything[key] === 'object') ? everything[key] : {};
    var count = part(deck, 'count');
    var status = part(deck, 'status');
    var controls = part(deck, 'controls');
    var filter = deck.querySelector('[' + ACT + '="filter"]');
    var reset = deck.querySelector('[' + ACT + '="reset"]');
    var progress = part(deck, 'progress');
    var narrowed = false;

    function mark(card) { return marks[card.getAttribute('data-deck-card')] || ''; }

    function paint(card) {
      var state = mark(card);
      if (state) { card.setAttribute(STATE, state); } else { card.removeAttribute(STATE); }
      var shown = part(card, 'mark');
      if (shown) {
        shown.textContent = state === KNOWN ? 'Known' : state === AGAIN ? 'To see again' : '';
      }
      card.hidden = narrowed && state === KNOWN;
    }

    function tally() {
      var known = cards.filter(function (card) { return mark(card) === KNOWN; }).length;
      count.textContent = fill(words(progress, 'data-deck-count'), {
        known: known, total: cards.length
      });
      status.textContent = known === cards.length ? words(status, 'data-deck-complete')
        : narrowed && !cards.some(function (card) { return mark(card) !== KNOWN; })
          ? words(status, 'data-deck-empty') : '';
      cards.forEach(paint);
    }

    function set(card, state) {
      marks[card.getAttribute('data-deck-card')] = state;
      everything[key] = marks;
      save(everything);
      tally();
    }

    cards.forEach(function (card) {
      var back = part(card, 'back');
      var actions = document.createElement('p');
      actions.setAttribute(PART, 'actions');
      var flip = button('Show the back', 'flip');
      flip.setAttribute('aria-expanded', 'false');
      var known = button('I knew it', KNOWN);
      var again = button('See again', AGAIN);
      var shown = document.createElement('span');
      shown.setAttribute(PART, 'mark');
      known.hidden = again.hidden = true;
      back.hidden = true;
      flip.addEventListener('click', function () {
        var open = back.hidden;
        back.hidden = !open;
        known.hidden = again.hidden = !open;
        flip.setAttribute('aria-expanded', open ? 'true' : 'false');
        flip.textContent = open ? 'Hide the back' : 'Show the back';
      });
      known.addEventListener('click', function () { set(card, KNOWN); });
      again.addEventListener('click', function () { set(card, AGAIN); });
      [flip, known, again, shown].forEach(function (one) { actions.appendChild(one); });
      card.appendChild(actions);
    });

    if (filter) {
      filter.addEventListener('click', function () {
        narrowed = !narrowed;
        filter.setAttribute('aria-pressed', narrowed ? 'true' : 'false');
        filter.textContent = words(filter, narrowed ? 'data-deck-all' : 'data-deck-open');
        tally();
      });
    }
    if (reset) {
      reset.addEventListener('click', function () {
        marks = {};
        delete everything[key];
        save(everything);
        narrowed = false;
        if (filter) {
          filter.setAttribute('aria-pressed', 'false');
          filter.textContent = words(filter, 'data-deck-open');
        }
        tally();
      });
    }
    var offline = part(deck, 'offline');
    if (offline) { offline.hidden = true; }
    if (controls) { controls.hidden = false; }
    tally();
  }

  [].slice.call(document.querySelectorAll(DECK)).forEach(wire);
}());
