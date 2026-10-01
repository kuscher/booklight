(function () {
  var $ = function (id) { return document.getElementById(id); };
  function place(hl, el, how) {            // put a highlight on a row; how: "down" | "up" | "rigid" | ""
    hl.className = hl.className.replace(/\b(down|up|rigid)\b/g, "").trim() + (how ? " " + how : "");
    hl.style.setProperty("--t", el.offsetTop + "px");
    hl.style.setProperty("--b", (el.offsetTop + el.offsetHeight) + "px");
  }
  function controls(name, fn) {
    var box = document.querySelector('.controls[data-for="' + name + '"]');
    if (box) box.addEventListener("click", function (e) { var k = e.target.getAttribute && e.target.getAttribute("data-k"); if (k) fn(k); });
  }

  // ---- a row and its other actions
  (function () {
    var panel = $("opanel"), hl = $("ohl"), parent = $("oparent"), block = $("oblock"), others = $("oothers"), chev = $("ochev"), more = $("omore"), foot = $("ofoot");
    var subs = Array.prototype.slice.call(block.querySelectorAll(".sub"));
    var open = false, sel = -1, othersH = others.offsetHeight;
    others.style.height = othersH + "px";
    function paint(how) {
      subs.forEach(function (s, i) { s.classList.toggle("sel", i === sel); });
      panel.classList.toggle("isopen", open);
      panel.classList.toggle("inlist", open && sel >= 0);
      chev.classList.toggle("arm", !(open && sel >= 0));
      more.textContent = open ? "Less" : "More";
      hl.classList.toggle("danger", sel === 7);
      place(hl, sel >= 0 ? subs[sel] : parent, how);
      foot.innerHTML = open ? '<span class="fk">←</span>Less<span class="fk">esc</span>Close' : '<span class="fk">tab</span>Actions<span class="fk">esc</span>Close';
    }
    function setOpen(v) {
      open = v;
      block.style.height = v ? block.scrollHeight + "px" : "0px";
      others.style.opacity = v ? "0" : "1";
      others.style.height = v ? "0px" : othersH + "px";
      if (v) { sel = 0; paint("down"); } else { sel = -1; paint("rigid"); }
    }
    function key(k) {
      if (k === "enter") { if (!open) setOpen(true); else if (sel < 0) setOpen(false); }
      else if (k === "left") { if (open) setOpen(false); }
      else if (k === "down") { if (!open) return; if (sel < subs.length - 1) { sel++; paint("down"); } }
      else if (k === "up") { if (!open) return; if (sel >= 0) { sel--; paint("up"); } }
    }
    chev.addEventListener("click", function () { setOpen(!open); });
    subs.forEach(function (s, i) { s.addEventListener("click", function () { if (open) { var d = i > sel ? "down" : "up"; sel = i; paint(d); } }); });
    controls("row", key);
    place(hl, parent, "");
    window.addEventListener("resize", function () { paint(""); });
  })();

  // ---- tasks: the check that draws itself
  (function () {
    var rows = $("trows"), done = $("tdone");
    if (!rows) return;
    var hl = rows.querySelector(".hl"), tasks = Array.prototype.slice.call(rows.querySelectorAll(".task")), at = 0, timer;
    function select(i, how) { at = i; tasks.forEach(function (t, j) { t.classList.toggle("on", j === i); }); place(hl, tasks[i], how); }
    function tick() {
      var t = tasks[at], on = !t.classList.contains("ticked");
      t.classList.toggle("ticked", on);
      t.querySelector(".tdo").textContent = on ? "Undo" : "Done";
      done.innerHTML = on ? '<svg class="i" viewBox="0 0 24 24" style="width:16px;height:16px"><path d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z"/></svg>Done' : "";
      done.classList.toggle("show", on);
      clearTimeout(timer); timer = setTimeout(function () { done.classList.remove("show"); }, 1600);
    }
    tasks.forEach(function (t, i) { t.addEventListener("click", function () { if (i === at) tick(); else select(i, i > at ? "down" : "up"); }); });
    controls("todo", function (k) { if (k === "tick") tick(); else select((at + 1) % tasks.length, at + 1 < tasks.length ? "down" : "up"); });
    select(0, "");
  })();

  // ---- a tip, and Booklight typing
  (function () {
    var typed = $("tiptyped"), ph = $("tipph"), chip = $("tipchip"), g = $("tipg"), card = $("tipcard"), rows = $("tiprows"), foot = $("tipfoot"), tryit = $("tiptry");
    if (!typed) return;
    var text = "timer 10m tea", busy = false, timers = [];
    function later(fn, ms) { timers.push(setTimeout(fn, ms)); }
    function reset() {
      timers.forEach(clearTimeout); timers = []; busy = false;
      typed.textContent = ""; ph.hidden = false; chip.hidden = true; g.hidden = false; tryit.classList.remove("arm");
      card.style.height = ""; card.style.opacity = ""; card.hidden = false; rows.hidden = true; foot.hidden = true;
    }
    function run() {
      if (busy) return; reset(); busy = true;
      tryit.classList.add("arm");                                   // Tab arms it
      later(function () {                                           // Enter: Booklight types, 37 ms a letter
        ph.hidden = true;
        text.split("").forEach(function (c, i) {
          later(function () {
            if (i === 5) { chip.hidden = false; chip.classList.add("risein"); g.hidden = true; typed.textContent = ""; }   // the space lands: "timer" is the chip
            else typed.textContent += c;
            if (i === text.length - 1) later(function () {            // the last letter: one search, the card goes, the row arrives
              card.style.opacity = "0"; card.style.height = "0px";
              rows.hidden = false; rows.firstElementChild.classList.add("risein"); foot.hidden = false; busy = false;
            }, 60);
          }, i * 37);
        });
      }, 520);
    }
    controls("tip", function (k) { if (k === "try") run(); else reset(); });
  })();

  // ---- an answer from the device's own model
  (function () {
    var panel = $("aipanel"), ans = $("aians"), strip = $("aistrip");
    if (!panel) return;
    var words = "Guten Morgen, bleibt es beim Mittagessen?".split(" "), timers = [];
    function reset() { timers.forEach(clearTimeout); timers = []; panel.classList.remove("think"); ans.innerHTML = ""; strip.style.opacity = ".35"; }
    function ask() {
      reset(); panel.classList.add("think");
      words.forEach(function (w, i) {
        timers.push(setTimeout(function () {
          if (i === 0) panel.classList.remove("think");
          var s = document.createElement("span"); s.textContent = (i ? " " : "") + w; ans.appendChild(s);
          if (i === words.length - 1) strip.style.opacity = "1";
        }, 900 + i * 110));
      });
    }
    controls("ai", function (k) { if (k === "ask") ask(); else reset(); });
    timers.push(setTimeout(function () { ans.innerHTML = "<span>Guten Morgen, bleibt es beim Mittagessen?</span>"; strip.style.opacity = "1"; }, 0));   // complete at rest
  })();

  // ---- the window: the column's highlight travels; the page follows from the side it came from
  (function () {
    var nav = document.querySelector(".nav"); if (!nav) return;
    var hl = $("navhl"), at = 0, buttons = Array.prototype.slice.call(nav.querySelectorAll("button"));
    function go(i, move) {
      var b = buttons[i], down = i > at;
      hl.className = "navhl" + (move ? (down ? " down" : " up") : "");
      nav.style.setProperty("--t", b.offsetTop + "px"); nav.style.setProperty("--b", (b.offsetTop + b.offsetHeight) + "px");
      hl.style.setProperty("--t", b.offsetTop + "px"); hl.style.setProperty("--b", (b.offsetTop + b.offsetHeight) + "px");
      buttons.forEach(function (x) { x.setAttribute("aria-current", x === b ? "true" : "false"); });
      document.querySelectorAll(".pg").forEach(function (p) {
        var show = p.getAttribute("data-page") === b.getAttribute("data-go");
        if (show && move) { p.classList.toggle("fromtop", !down); Array.prototype.forEach.call(p.children, function (c, n) { c.style.setProperty("--i", Math.min(n, 6)); }); }
        p.hidden = !show;
      });
      at = i;
    }
    buttons.forEach(function (b, i) { b.addEventListener("click", function () { if (i !== at) go(i, true); }); });
    go(0, false);
  })();

  // ---- the pinned countdown: only the digits that changed move
  (function () {
    var cd = $("cd"); if (!cd) return;
    var left = 7 * 60 + 42;
    if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    setInterval(function () {
      left = left > 0 ? left - 1 : 7 * 60 + 42;
      var s = Math.floor(left / 60) + ":" + ("0" + left % 60).slice(-2), old = cd.textContent, html = "";
      for (var i = 0; i < s.length; i++) html += s[i] === ":" ? ":" : '<span' + (old[i] !== s[i] ? ' class="tick"' : "") + ">" + s[i] + "</span>";
      cd.innerHTML = html;
    }, 1000);
  })();
})();
