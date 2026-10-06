/* The reader's reading mode: the first-visit question, the switch, and the choice kept.

   ⛔ WRITTEN ONLY FOR A CORPUS THAT DECLARES MODES, as its own file. The shared
   script never carries a line of it.

   ⚠️ IT IS LINKED IN THE HEAD, NOT AT THE FOOT OF THE BODY: a `<script>` at body
   level is a row of the page's grid, and the rail spans a fixed count of them.
   So this part waits for `DOMContentLoaded`, which comes after every deferred
   script, `page.js` among them, and reads the store's published name with no
   existence guard: a wrong order would fail loudly, as the theme's does.

   ⭐ WHAT THE MARKUP ALREADY DID. The page ships `data-mode="<default mode>"` on
   the root element and `modes.css` hides the sections of the other languages by
   it, so with this script off the page is the default mode. This script only
   changes the attribute; it shows and hides nothing itself.

   ⛔ THE STORE IS `study-progress.js`'s DISPLAY RECORD, as the theme's is. Every
   read and write goes through its published name, guarded by that store's own
   try/catch, and a refused store costs the page nothing it was showing. The
   head's boot reads a `sessionStorage` cache of the choice, kept by the store,
   so a later page of the same tab does not flash the default for a frame.

   ⭐ WHEN THE QUESTION IS ASKED. Once, on a first visit: when nothing usable is
   stored. Where the page is opened from a file the stored answer is not read at
   all and the question is asked on every load, because a file page has no
   origin to keep it for. Where the browser refuses to keep anything the question
   is not asked (an answer that cannot be remembered would be asked of the reader
   on every page), and the switch still changes the page being read.

   ⭐ The words are in `page.html`'s templates (R13): this file toggles `hidden`
   and `aria-pressed` and types nothing.

   ⭐ ENTRIES OF THE OTHER LANGUAGE. An index row, a rail row and a row of a module's list
   that belongs to some languages only carries `data-entry-lang`; the stylesheet greys it
   by the mode, with no script. What a script adds is what a stylesheet cannot do:
   under `data-outside="locked"` it takes the `href` off the link of a row the mode does not
   read (keeping it in `data-href`, `aria-disabled`, out of the tab order) and gives it back
   when a mode that reads it is chosen; it counts only the pages the mode reads (the groups'
   `of N read`, the progress line and strip, the Up next slip and the module's unit count);
   it shows, in the between-units bar, the first neighbour the mode reads; and it keeps a
   section a link points into on the page while that link is the target.

   ⛔ `page.js` is not changed for it: it ran before this file and counted every row. This file
   recounts after it, and after each change of mode. */

(function () {
  'use strict';

  function start() {
    var PREFERENCE = 'mode';
    var ATTRIBUTE = 'data-mode';
    var SWITCH = '[data-section="mode"]';
    var QUESTION = '[data-section="mode-question"]';
    var CHOICE = 'data-mode-choice';

    var store = window.studyforge.progress;
    var root = document.documentElement;

    var control = document.querySelector(SWITCH);
    var question = document.querySelector(QUESTION);
    if (!control || !question) { return; }

    var fallback = control.getAttribute('data-mode-default');
    var chosen = [].slice.call(control.querySelectorAll('[' + CHOICE + ']'));
    var offered = [].slice.call(question.querySelectorAll('[' + CHOICE + ']'));
    var known = chosen.map(function (button) { return button.getAttribute(CHOICE); });
    if (!known.length || known.indexOf(fallback) === -1) { return; }

    var fromFile = window.location.protocol === 'file:';

    /* A stored value this does not know is no answer at all. */
    function stored() {
      if (fromFile) { return null; }
      var held = store.preference(PREFERENCE);
      return known.indexOf(held) === -1 ? null : held;
    }

    var ENTRY = 'data-entry-lang';
    var locked = root.getAttribute('data-outside') === 'locked';
    var prose = {};
    chosen.forEach(function (button) {
      prose[button.getAttribute(CHOICE)] = button.getAttribute('data-mode-prose');
    });

    function tokens(node, name) {
      return (node.getAttribute(name) || '').split(' ');
    }

    /* Whether `node` belongs to languages the chosen mode does not read. */
    function outside(node, mode, name) {
      return node.hasAttribute(name) && tokens(node, name).indexOf(prose[mode]) === -1;
    }

    function own(row) {
      return [].slice.call(row.querySelectorAll('a')).filter(function (link) {
        return link.closest('[' + ENTRY + ']') === row;
      });
    }

    function lock(row, closed) {
      own(row).forEach(function (link) {
        if (closed) {
          if (link.hasAttribute('href')) {
            link.setAttribute('data-href', link.getAttribute('href'));
            link.removeAttribute('href');
          }
          link.setAttribute('aria-disabled', 'true');
          link.setAttribute('tabindex', '-1');
        } else {
          if (link.hasAttribute('data-href')) {
            link.setAttribute('href', link.getAttribute('data-href'));
            link.removeAttribute('data-href');
          }
          link.removeAttribute('aria-disabled');
          link.removeAttribute('tabindex');
        }
      });
      if (!row.hasAttribute('data-readable')) { return; }
      var opens = own(row).some(function (link) { return link.hasAttribute('href'); }) ||
        row.hasAttribute('aria-current');
      row.setAttribute('data-readable', opens ? 'true' : 'false');
    }

    var LISTS = 'nav[aria-label="Contents"] li[id], nav[aria-label="Units"] li[id]';

    /* A row the mode reads, that has a page, and that this mode can open. */
    function counted(row, mode) {
      return row.getAttribute('data-readable') === 'true' && !outside(row, mode, ENTRY);
    }

    function fill(holder, value) {
      var slot = holder && holder.querySelector('b, strong');
      if (slot) { slot.textContent = String(value); }
    }

    /* Writes a number into the text node that already holds one, keeping its words:
       `of 5 read` takes the number alone, `3 units` takes the plural with it. */
    function phrase(holder, total) {
      [].slice.call(holder.childNodes).forEach(function (node) {
        if (node.nodeType !== 3) { return; }
        if (/\d+ units?/.test(node.nodeValue)) {
          node.nodeValue = node.nodeValue.replace(/\d+ (unit)s?/, total + ' $1' + (total === 1 ? '' : 's'));
        } else if (/ of \d+ /.test(node.nodeValue)) {
          node.nodeValue = node.nodeValue.replace(/ of \d+ /, ' of ' + total + ' ');
        }
      });
    }

    function recount(mode) {
      var usable = store.supported();
      var marks = usable ? store.marks() : [];
      var rows = [].slice.call(document.querySelectorAll(LISTS));
      var live = rows.filter(function (row) { return counted(row, mode); });
      function read(row) { return marks.indexOf(row.id) !== -1; }
      function within(group) {
        return live.filter(function (row) { return group.contains(row); });
      }
      function done(list) { return list.filter(read).length; }

      [].slice.call(document.querySelectorAll('nav[aria-label="Contents"] summary > small')).forEach(
        function (tally) {
          var members = within(tally.parentNode.parentNode);
          fill(tally, done(members));
          phrase(tally, members.length);
          tally.hidden = !usable || !members.length;
        }
      );

      var next = live.filter(function (row) { return !read(row); })[0] || null;
      [].slice.call(document.querySelectorAll('[aria-current="step"]')).forEach(function (node) {
        node.removeAttribute('aria-current');
      });
      if (usable && next) { next.setAttribute('aria-current', 'step'); }

      var region = document.querySelector('section[aria-label="Progress"]');
      if (region) {
        var line = region.querySelector('p');
        fill(line, done(live));
        fill(line && line.querySelector('span'), live.length - done(live));
        if (line) { phrase(line, live.length); }
        [].slice.call(region.querySelectorAll('li > a[href^="#"]')).forEach(function (link) {
          var group = document.getElementById(link.getAttribute('href').slice(1));
          var members = group ? within(group) : [];
          var share = members.length ? Math.round((100 * done(members)) / members.length) : 0;
          link.style.setProperty('--read', share + '%');
          if (usable && next && group && group.contains(next)) {
            link.parentNode.setAttribute('aria-current', 'step');
          }
        });
      }

      var slip = document.querySelector('nav[aria-label="Up next"]');
      if (slip && live.length) {
        var lead = slip.querySelector('a');
        var finished = slip.querySelector('p');
        var target = next || (usable ? null : live[0]);
        var source = target && (target.querySelector('a[href]') || target.querySelector('a'));
        var address = source && (source.getAttribute('href') || source.getAttribute('data-href'));
        if (lead && address) {
          var title = source.cloneNode(true);
          [].slice.call(title.querySelectorAll('span, small')).forEach(function (node) {
            node.parentNode.removeChild(node);
          });
          lead.setAttribute('href', address);
          lead.lastChild.textContent = ' ' + title.textContent.trim();
          lead.hidden = false;
          if (finished) { finished.hidden = true; }
        } else if (lead && finished && usable) {
          lead.hidden = true;
          finished.hidden = false;
        }
      }

      /* A module's page says how many units it holds, in its masthead. */
      var meta = [].slice.call(document.querySelectorAll('body > header p')).filter(function (p) {
        return /^\d+ units?/.test(p.textContent);
      })[0];
      if (meta && rows.length) { phrase(meta, live.length); }
    }

    /* The between-units bar shows the first neighbour in each direction the mode can open. */
    function pager(mode) {
      ['previous', 'next'].forEach(function (side) {
        var chain = [].slice.call(document.querySelectorAll('nav[aria-label="Between units"] a[data-pager="' + side + '"]'));
        var shown = null;
        chain.forEach(function (link) {
          var opens = shown === null && !outside(link, mode, 'data-pager-lang');
          if (opens) { shown = link; }
          link.hidden = !opens;
          if (opens) { link.setAttribute('rel', side === 'previous' ? 'prev' : 'next'); }
          else { link.removeAttribute('rel'); }
        });
      });
    }

    /* A link into a section the mode hides shows that section, so it lands on something. */
    function arrive() {
      [].slice.call(document.querySelectorAll('section[data-linked]')).forEach(function (section) {
        section.removeAttribute('data-linked');
      });
      var id = window.location.hash.slice(1);
      var target = id ? document.getElementById(decodeURIComponent(id)) : null;
      var section = target && target.closest('section[data-lang]');
      if (section && window.getComputedStyle(section).display === 'none') {
        section.setAttribute('data-linked', '');
        target.scrollIntoView();
      }
    }

    function apply(mode) {
      [].slice.call(document.querySelectorAll('section[data-linked]')).forEach(function (section) {
        section.removeAttribute('data-linked');
      });
      [].slice.call(document.querySelectorAll('li[' + ENTRY + ']')).forEach(function (row) {
        lock(row, locked && outside(row, mode, ENTRY));
      });
      if (locked) { pager(mode); }
      recount(mode);
    }

    function show(mode) {
      root.setAttribute(ATTRIBUTE, mode);
      chosen.forEach(function (button) {
        button.setAttribute('aria-pressed', button.getAttribute(CHOICE) === mode ? 'true' : 'false');
      });
      apply(mode);
    }

    function close() {
      var open = !question.hidden;
      question.hidden = true;
      return open;
    }

    function choose(mode) {
      var kept = store.prefer(PREFERENCE, mode);
      /* The cache follows what the store kept, and holds nothing when it kept nothing. */
      store.cache(PREFERENCE, kept && !fromFile ? mode : null);
      show(mode);
      if (close()) {
        var pressed = control.querySelector('[aria-pressed="true"]');
        if (pressed) { pressed.focus(); }
      }
    }

    var held = stored();
    show(held === null ? fallback : held);
    control.hidden = false;
    arrive();
    window.addEventListener('hashchange', arrive);

    var noted = [].slice.call(document.querySelectorAll(
      'aside[data-section="mode-outside"] [' + CHOICE + ']'
    ));

    chosen.concat(offered, noted).forEach(function (button) {
      button.addEventListener('click', function () { choose(button.getAttribute(CHOICE)); });
    });

    question.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') { close(); }
    });

    if (held === null && (fromFile || store.supported())) {
      question.hidden = false;
      if (offered.length) { offered[0].focus(); }
    }
  }

  /* ⚠️ Not `readyState === 'loading'`: while a deferred script runs the state is
     already `interactive`, and `DOMContentLoaded` has not fired yet. */
  if (document.readyState === 'complete') {
    start();
  } else {
    document.addEventListener('DOMContentLoaded', start);
  }
}());

/* A two-language example: the tabs of one block, opened by the reading mode.

   ⛔ WRITTEN ONLY FOR A CORPUS THAT DECLARES MODES, composed into `modes.js`
   (`render/example_tabs.py`). The shared script never carries a line of it.

   ⭐ WHAT THE MARKUP ALREADY DID. Every panel is present under a visible label in the
   default mode's order, and `modes.css` hides what the mode does not list. This
   script only upgrades it: it shows the tab bar when a block has two tabs to move
   between, opens the first tab the mode lists, and keeps the keys of the WAI-ARIA
   tabs pattern: Left and Right wrap, Home and End, the selected tab the one in the
   tab order, the panel focusable.

   ⭐ A click changes that block only and is not remembered; the remembered choice is
   the mode. A change of mode re-opens each block on the first tab that mode lists. */

(function () {
  'use strict';

  var MODE_TABS = {"java": ["java", "python", "typescript", "kotlin"], "kotlin": ["kotlin", "python", "typescript", "java"], "python": ["python", "typescript", "java", "kotlin"], "typescript": ["typescript", "python", "java", "kotlin"]};
  var ATTRIBUTE = 'data-mode';

  function blocks() {
    return [].slice.call(document.querySelectorAll('div[data-example]'));
  }

  function tabsOf(block) {
    return [].slice.call(block.querySelectorAll('[role="tab"]'));
  }

  function panelOf(tab) {
    return document.getElementById(tab.getAttribute('aria-controls'));
  }

  /* The tabs the mode lists, in the mode's order. */
  function listed(block) {
    var order = MODE_TABS[document.documentElement.getAttribute(ATTRIBUTE)] || [];
    return tabsOf(block)
      .filter(function (tab) { return order.indexOf(tab.getAttribute('data-lang')) !== -1; })
      .sort(function (a, b) {
        return order.indexOf(a.getAttribute('data-lang')) - order.indexOf(b.getAttribute('data-lang'));
      });
  }

  function select(block, tab, focus) {
    tabsOf(block).forEach(function (other) {
      var on = other === tab;
      other.setAttribute('aria-selected', on ? 'true' : 'false');
      other.setAttribute('tabindex', on ? '0' : '-1');
      var panel = panelOf(other);
      if (panel) { panel.hidden = !on; }
    });
    if (focus) { tab.focus(); }
  }

  function upgrade(block) {
    var bar = block.querySelector('[role="tablist"]');
    var shown = listed(block);
    if (!bar) { return; }
    /* Reading order follows the mode: tabs in the bar, panels after it. */
    shown.slice().reverse().forEach(function (tab) {
      bar.insertBefore(tab, bar.firstChild);
    });
    var anchor = bar;
    shown.forEach(function (tab) {
      var panel = panelOf(tab);
      if (panel) { anchor.parentNode.insertBefore(panel, anchor.nextSibling); anchor = panel; }
    });
    block.setAttribute('data-tabs-on', '');
    bar.hidden = shown.length < 2;
    if (shown.length) { select(block, shown[0], false); }
  }

  function key(block, event) {
    var tab = event.target;
    if (tab.getAttribute('role') !== 'tab') { return; }
    var shown = listed(block);
    var at = shown.indexOf(tab);
    var to = null;
    if (event.key === 'ArrowRight') { to = shown[(at + 1) % shown.length]; }
    else if (event.key === 'ArrowLeft') { to = shown[(at - 1 + shown.length) % shown.length]; }
    else if (event.key === 'Home') { to = shown[0]; }
    else if (event.key === 'End') { to = shown[shown.length - 1]; }
    if (to) {
      event.preventDefault();
      select(block, to, true);
    }
  }

  function start() {
    var all = blocks();
    all.forEach(function (block) {
      var bar = block.querySelector('[role="tablist"]');
      if (!bar) { return; }
      bar.addEventListener('click', function (event) {
        var tab = event.target.closest ? event.target.closest('[role="tab"]') : null;
        if (tab) { select(block, tab, false); }
      });
      bar.addEventListener('keydown', function (event) { key(block, event); });
    });
    all.forEach(upgrade);
    new MutationObserver(function () { blocks().forEach(upgrade); })
      .observe(document.documentElement, { attributes: true, attributeFilter: [ATTRIBUTE] });
  }

  if (document.readyState === 'complete') {
    start();
  } else {
    document.addEventListener('DOMContentLoaded', start);
  }
}());

/* A language a block or a practice lacks, greyed: what a stylesheet cannot do.

   ⛔ WRITTEN ONLY FOR A CORPUS THAT DECLARES `absent_language: grey`, appended to
   `modes.js` after the tabs' script (`render/absent_language.py`), so the shared script and
   every other corpus's `modes.js` keep their bytes.

   ⭐ WHAT THE MARKUP AND THE STYLESHEET ALREADY DID. A tab for a language the block lacks is
   there, `aria-disabled`, and the sentence that names the carriers is shown by the mode's
   rules; a practice card that is outside the mode shows its own sentence. This file makes the
   disabled tab behave as one: a click does nothing, the arrow keys, Home and End still reach it
   (it stays focusable, so its reason can be read), a block opens on the first tab the mode
   lists that exists, and the bar is shown when the mode's one tab is a missing one.

   ⭐ A greyed practice does not open: its link says `aria-disabled` and ignores a click, and
   Previous and Next in the workspace pass over it. Whether a card is greyed is read from the
   page itself, as whether its sentence is shown, so this file holds no list of languages. */

(function () {
  'use strict';

  var ATTRIBUTE = 'data-mode';

  function shown(node) {
    return window.getComputedStyle(node).display !== 'none';
  }

  function enabled(tab) {
    return tab.getAttribute('aria-disabled') !== 'true';
  }

  function visibleTabs(block) {
    return [].slice.call(block.querySelectorAll('[role="tab"]')).filter(shown);
  }

  /* ⭐ Every example block with a tab it lacks is read again after each change of mode, once
     the tabs' own script has put its tabs in the mode's order. */
  function upgrade(block) {
    var bar = block.querySelector('[role="tablist"]');
    if (!bar) { return; }
    var tabs = visibleTabs(block);
    if (!tabs.length) { return; }
    var open = tabs.filter(enabled);
    tabs.forEach(function (tab) {
      if (!enabled(tab)) { tab.setAttribute('aria-selected', 'false'); }
    });
    if (open.length && !open.some(function (tab) { return tab.getAttribute('aria-selected') === 'true'; })) {
      open[0].click();
    }
    bar.hidden = tabs.length < 2 && enabled(tabs[0]);
    var on = tabs.filter(function (tab) { return tab.getAttribute('aria-selected') === 'true' && enabled(tab); })[0];
    tabs.forEach(function (tab) {
      tab.setAttribute('tabindex', (on ? tab === on : tab === tabs[0]) ? '0' : '-1');
    });
  }

  function blocks() {
    return [].slice.call(document.querySelectorAll('div[data-example][data-missing]'));
  }

  function key(event) {
    var tab = event.target;
    if (!tab.closest || tab.getAttribute('role') !== 'tab') { return; }
    var block = tab.closest('div[data-example][data-missing]');
    if (!block) { return; }
    var tabs = visibleTabs(block);
    var at = tabs.indexOf(tab);
    var to = null;
    if (event.key === 'ArrowRight') { to = tabs[(at + 1) % tabs.length]; }
    else if (event.key === 'ArrowLeft') { to = tabs[(at - 1 + tabs.length) % tabs.length]; }
    else if (event.key === 'Home') { to = tabs[0]; }
    else if (event.key === 'End') { to = tabs[tabs.length - 1]; }
    if (!to) { return; }
    event.preventDefault();
    event.stopImmediatePropagation();
    if (enabled(to)) { to.click(); }
    tabs.forEach(function (one) { one.setAttribute('tabindex', one === to ? '0' : '-1'); });
    to.focus();
  }

  function click(event) {
    var tab = event.target.closest ? event.target.closest('[role="tab"][aria-disabled="true"]') : null;
    if (!tab) { return; }
    event.preventDefault();
    event.stopImmediatePropagation();
    tab.focus();
  }

  /* --- practices ---------------------------------------------------------- */
  var LINK = 'a[data-practices-part="open"]';
  var CARD = 'li[data-practice-card]';

  function greyed(card) {
    var note = card.querySelector('[data-practice-carriers]');
    return !!note && shown(note);
  }

  function cards() {
    return [].slice.call(document.querySelectorAll(CARD));
  }

  function mark() {
    cards().forEach(function (card) {
      var link = card.querySelector(LINK);
      if (!link) { return; }
      if (greyed(card)) { link.setAttribute('aria-disabled', 'true'); }
      else { link.removeAttribute('aria-disabled'); }
    });
  }

  function declined(event) {
    var link = event.target.closest ? event.target.closest(LINK) : null;
    if (!link || link.getAttribute('aria-disabled') !== 'true') { return; }
    event.preventDefault();
    event.stopImmediatePropagation();
  }

  /* The card whose section the address names, which is the practice that is open. */
  function openCard() {
    var id = window.location.hash.slice(1);
    /* ⭐ A practice of several language editions is open when any one of its editions is. */
    return cards().filter(function (card) {
      return card.getAttribute('data-practice-card') === id ||
        !!card.querySelector('[data-edition-section="' + id + '"]');
    })[0] || null;
  }

  function readable(from, step) {
    var list = cards();
    for (var at = list.indexOf(from) + step; at >= 0 && at < list.length; at += step) {
      if (!greyed(list[at])) { return list[at]; }
    }
    return null;
  }

  /* Previous and Next show only where a practice the mode lists lies that way. */
  function ends() {
    var here = openCard();
    [['previous', -1], ['next', 1]].forEach(function (side) {
      var button = document.querySelector('[data-workspace-act="' + side[0] + '"]');
      if (button && here) { button.hidden = readable(here, side[1]) === null; }
    });
  }

  function step(event) {
    var button = event.target.closest ? event.target.closest('[data-workspace-act]') : null;
    var way = button && button.getAttribute('data-workspace-act');
    if (way !== 'previous' && way !== 'next') { return; }
    var here = openCard();
    if (!here) { return; }
    event.preventDefault();
    event.stopImmediatePropagation();
    var to = readable(here, way === 'next' ? 1 : -1);
    var link = to && to.querySelector(LINK);
    if (link) { link.click(); }
  }

  function start() {
    document.addEventListener('click', click, true);
    document.addEventListener('click', declined, true);
    document.addEventListener('click', step, true);
    document.addEventListener('keydown', key, true);
    cards().forEach(function (card) {
      var link = card.querySelector(LINK);
      if (link) { link.addEventListener('click', function () { window.setTimeout(ends, 0); }); }
    });
    function all() {
      blocks().forEach(upgrade);
      mark();
      ends();
    }
    all();
    new MutationObserver(all)
      .observe(document.documentElement, { attributes: true, attributeFilter: [ATTRIBUTE] });
  }

  if (document.readyState === 'complete') {
    start();
  } else {
    document.addEventListener('DOMContentLoaded', start);
  }
}());
