/* Run, beside the code of an example that has a test.

   ⭐ **The strip is drawn by the build** (`p[data-example-run]`, `render/page/examplerun.py`) beside
   the code of an example tab that names the file its code is, where the corpus pairs that file
   with a test. It ships `hidden`: over `file://` and with no runner there is nothing to press.
   Served, with the corpus's runner up, this shows it and a press runs the test the file is
   paired with, in the copy of the corpus's code, streamed into the `pre` beside it.

   ⛔ **This file draws; it never talks to the API.** Everything it asks goes through
   `window.studyforge.run` (`available`, `runnable`, `codeTest`, `stop`), which the SERVING
   PROCESS adds to the page it answers, so a built text names no API, no origin and no client
   file (R8). ⛔ The file to run is the strip's own path: the server pairs it and picks the
   command, and nothing here is ever an argument.

   ⭐ The words are the ones a lesson's code example says (`code-links.js`). One run is live at a
   time and the server refuses a second, which the status says. */

(function () {
  'use strict';

  var STRIP = 'p[data-example-run]';
  var ACT = 'data-example-act';
  var PART = 'data-example-part';

  var strips = [].slice.call(document.querySelectorAll(STRIP));
  if (!strips.length) { return; }
  var run = window.studyforge && window.studyforge.run;
  if (!run || !run.available() || !run.codeTest) { return; }

  function part(strip, name) { return strip.querySelector('[' + PART + '="' + name + '"]'); }
  function act(strip, name) { return strip.querySelector('[' + ACT + '="' + name + '"]'); }

  function wire(strip) {
    var corpus = strip.getAttribute('data-corpus');
    var path = strip.getAttribute('data-example-run');
    var output = strip.nextElementSibling;
    var go = act(strip, 'run');
    var stop = act(strip, 'stop');
    var status = part(strip, 'status');
    go.addEventListener('click', function () {
      go.disabled = true;
      stop.hidden = false;
      output.textContent = '';
      output.hidden = false;
      status.textContent = 'Running the test…';
      run.codeTest(corpus, path, function (line) {
        output.textContent += line + '\n';
      }).then(function (verdict) {
        status.textContent = verdict === 0 ? 'Passed.' : verdict === 'stopped' ? 'Stopped.'
          : verdict === 'timeout' ? 'Timed out.' : 'Failed.';
      }, function () {
        status.textContent = 'The test could not be run.';
      }).then(function () {
        go.disabled = false;
        stop.hidden = true;
      });
    });
    stop.addEventListener('click', function () { run.stop(); });
    strip.hidden = false;
  }

  /* ⭐ Only a runner that is UP shows the strip. */
  var asked = run.runnable ? run.runnable(strips[0].getAttribute('data-corpus')) : Promise.resolve(true);
  asked.then(function (up) {
    if (up) { strips.forEach(wire); }
  }, function () { return null; });
}());
