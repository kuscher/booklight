(function () {
  var $ = function (id) { return document.getElementById(id); };
  function controls(name, fn) {
    var box = document.querySelector('.controls[data-for="' + name + '"]');
    if (box) box.addEventListener("click", function (e) {
      var k = e.target.getAttribute && e.target.getAttribute("data-k"); if (!k || e.target.disabled) return;
      if (k === "theme") theme(document.getElementById(name), e.target); else fn(k);
    });
    return box;
  }
  function Timers() { var t = []; return { later: function (fn, ms) { t.push(setTimeout(fn, ms)); }, clear: function () { t.forEach(clearTimeout); t = []; } }; }
  function restart(el, cls) { el.classList.remove(cls); void el.offsetWidth; el.classList.add(cls); }
  // What stands under the field: one block at a time, and the height that follows it.
  function show(under, name, cut) {
    var h = 0;
    Array.prototype.forEach.call(under.children, function (b) {
      var on = b.getAttribute("data-b") === name;
      b.hidden = !on;
      if (on) { h = b.offsetHeight; Array.prototype.forEach.call(b.querySelectorAll("[data-i]"), function (p) { p.style.setProperty("--i", p.getAttribute("data-i")); restart(p, "risein"); }); }
    });
    under.classList.toggle("cut", !!cut);
    under.style.height = h + "px";
  }
  // The opening at Medium (design-system.md §12): at rest 420 ms after the key; what stands under the field may arrive when the glass is 85 % open, at 257 ms.
  var OPEN = 420, UNDER = 257;
  function unfold(panel) { restart(panel, "opening"); setTimeout(function () { panel.classList.remove("opening"); }, OPEN + 20); }
  // The outline's length in dp for the height the panel has now: the light takes 1 ms for each dp as the reflection, 1.5 ms while the model works.
  function outline(panel, h) {
    var e = panel.querySelector(".edge"); if (!e) return 0;
    var P = Math.round(2 * (720 + (h || panel.offsetHeight)) - 256 + 2 * Math.PI * 32);
    e.style.setProperty("--P", P); e.style.setProperty("--lap", P + "ms"); e.style.setProperty("--work", Math.round(P * 1.5) + "ms");
    Array.prototype.forEach.call(e.querySelectorAll("rect"), function (r) { r.setAttribute("pathLength", P); });
    return P;
  }
  // A drawing in the other theme: the white light shows on dark glass, and cannot be seen in light theme over a white window.
  function theme(panel, button) {
    var desk = panel.closest(".desk"), dark = desk.classList.toggle("th-dark");
    desk.classList.toggle("th-light", !dark); button.textContent = dark ? "Light" : "Dark";
  }
  // An answer, written in: the caption, the text it is about, then the words as they arrive.
  function write(panel, T, opts) {
    var row = panel.querySelector("[data-ans]"), cap = row.querySelector(".cap"), txt = row.querySelector(".txt");
    var ask = row.querySelector('[data-s="ask"]'), done = row.querySelector('[data-s="done"]');
    var f1 = panel.querySelector('[data-f="ask"]'), f2 = panel.querySelector('[data-f="done"]');
    var said = txt.getAttribute("data-said"), grows = row.hasAttribute("data-grows");
    cap.textContent = cap.getAttribute("data-a");
    txt.className = "txt own"; txt.textContent = txt.getAttribute("data-own") || "";
    if (grows) row.classList.remove("grown");
    if (ask) { ask.hidden = false; done.hidden = true; }
    if (f1) { f1.hidden = false; f2.hidden = true; }
    var u = panel.querySelector(".under");            // the height the panel is on its way to, not the one it has this frame
    T.later(function () { outline(panel, u && u.style.height ? 68 + parseFloat(u.style.height) : 0); panel.classList.add("think"); }, opts.light || 0);
    var words = said.split(" "), t0 = (opts.light || 0) + (opts.wait || 900);
    words.forEach(function (w, i) {
      T.later(function () {
        if (i === 0) { panel.classList.remove("think"); txt.className = "txt"; txt.textContent = ""; cap.textContent = cap.getAttribute("data-b"); }
        var s = document.createElement("span"); s.className = "w"; s.textContent = (i ? " " : "") + w; txt.appendChild(s);
        if (grows && txt.scrollHeight > 50) row.classList.add("grown");
        if (i === words.length - 1) { if (ask) { ask.hidden = true; done.hidden = false; restart(done, "fadein"); } if (f1) { f1.hidden = true; f2.hidden = false; } if (opts.end) opts.end(); }
      }, t0 + i * 70);
    });
  }
  function settle(panel) {            // the answered state, at once
    var row = panel.querySelector("[data-ans]"), cap = row.querySelector(".cap"), txt = row.querySelector(".txt");
    var ask = row.querySelector('[data-s="ask"]'), done = row.querySelector('[data-s="done"]');
    var f1 = panel.querySelector('[data-f="ask"]'), f2 = panel.querySelector('[data-f="done"]');
    panel.classList.remove("think"); cap.textContent = cap.getAttribute("data-b"); txt.className = "txt"; txt.textContent = txt.getAttribute("data-said");
    if (row.hasAttribute("data-grows")) row.classList.toggle("grown", row.getAttribute("data-grows") === "1");
    if (ask) { ask.hidden = true; done.hidden = false; }
    if (f1) { f1.hidden = true; f2.hidden = false; }
  }

  Array.prototype.forEach.call(document.querySelectorAll(".panel.lapstill"), function (p) { outline(p); });

  // ---- M1: open, and the copy is there; Tab; a letter
  (function () {
    var panel = $("c1"); if (!panel) return;
    var under = $("c1under"), g = $("c1g"), chip = $("c1chip"), typed = $("c1typed"), ph = $("c1ph"), esc = $("c1esc"), T = Timers(), state = "line";
    var box = controls("c1", key);
    function buttons() {
      Array.prototype.forEach.call(box.querySelectorAll("button"), function (b) {
        var k = b.getAttribute("data-k");
        b.disabled = (k === "tab" && (state === "list" || state === "typed")) || (k === "letter" && state !== "line" && state !== "empty") || (k === "back" && state !== "list" && state !== "typed");
        if (k === "theme") b.disabled = false;
      });
    }
    function field(mode) {
      g.hidden = mode === "chip"; chip.hidden = mode !== "chip"; typed.textContent = mode === "typed" ? "d" : "";
      ph.hidden = mode === "typed"; ph.textContent = mode === "chip" ? ph.getAttribute("data-chip") : ph.getAttribute("data-empty");
      esc.hidden = mode !== "empty";
    }
    function key(k) {
      T.clear(); panel.classList.remove("reflect");
      if (k === "key" || k === "key200") {                // the opening at the field's height; the line a beat later; the reflection in a quiet moment
        var hold = k === "key" ? 320 : 200;               // after the glass is 85 % open: the tip's hold, or the shorter one to compare
        field("empty"); show(under, "", true); state = "empty"; unfold(panel);
        T.later(function () { if (state === "empty") { show(under, "line"); state = "line"; buttons(); } }, UNDER + hold);
        var lap = outline(panel, 140);                    // 2.4 s after the panel has opened; once round, 1 ms for each dp
        T.later(function () { if (state === "line") restart(panel, "reflect"); }, OPEN + 2400);
        T.later(function () { panel.classList.remove("reflect"); }, OPEN + 2400 + lap + 60);
      } else if (k === "tab") {                           // the copy becomes the chip; the pill is on row one where the line stood
        field("chip"); restart(chip, "popin"); show(under, "list"); state = "list";
      } else if (k === "letter") {                        // typing owns the frame: the line is gone with the letter
        field("typed"); show(under, "typed"); state = "typed";
      } else if (k === "back") {
        if (state === "list") { field("empty"); show(under, "line"); state = "line"; }   // Backspace out of its own chip: the line is back
        else { field("empty"); show(under, ""); state = "empty"; }                        // the field emptied again: the line stays away
      }
      buttons();
    }
    show(under, "line", true); buttons();
    window.addEventListener("resize", function () { show(under, state === "empty" ? "" : state, true); });
  })();

  // ---- M2: a question about a picture, answered
  (function () {
    var panel = $("p4"); if (!panel) return;
    var T = Timers(), box = controls("p4", function (k) {
      T.clear(); panel.classList.remove("think");
      if (k === "ask") write(panel, T, { wait: 1400 }); else settle(panel);
    });
  })();

  // ---- M3: opened from the selection menu, the answer on its way
  (function () {
    var panel = $("s7"); if (!panel) return;
    var under = $("s7under"), T = Timers(), sel = $("s7sel"), done = $("s7done");
    var wrong = sel.textContent, right = sel.getAttribute("data-fixed");
    function reset() { T.clear(); panel.classList.remove("think"); sel.textContent = wrong; sel.className = "selx"; done.hidden = true; }
    controls("s7", function (k) {
      if (k === "play") {
        reset(); show(under, "", true); unfold(panel);
        T.later(function () { show(under, "row"); write(panel, T, { light: 60, wait: 1100 }); }, UNDER);   // under the field, nothing before the glass is 85 % open
      } else if (k === "enter") {
        T.clear(); settle(panel); show(under, "row", true); done.hidden = false; restart(done, "fadein"); sel.textContent = right;
      } else { reset(); settle(panel); show(under, "row", true); }
    });
    show(under, "row", true);
  })();
})();
