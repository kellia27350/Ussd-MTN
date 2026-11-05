/**
 * Product Catalogue chat widget.
 *
 * Drop this + chat-widget.css into any page served by this Spring Boot app
 * (or any page that can reach it, since ChatController is @CrossOrigin) and
 * a floating chat bubble appears. It talks to POST /api/chat, which walks
 * the caller through the exact same category -> products -> detail menu
 * tree as the USSD endpoint (*140#), just over HTTP/JSON instead of a USSD
 * gateway.
 *
 * State lives only in memory for the life of the page (a plain JS variable
 * for the sessionId) - nothing is persisted to localStorage/cookies, so
 * refreshing the page always starts a brand-new chat, same as dialling a
 * USSD code fresh.
 */
(function () {
  "use strict";

  var CHAT_ENDPOINT = "/api/chat";

  var sessionId = null;
  var chatEnded = false;
  var sending = false;

  // ---- DOM setup -----------------------------------------------------

  var launcher = document.createElement("button");
  launcher.id = "pc-chat-launcher";
  launcher.setAttribute("aria-label", "Open chat");
  launcher.innerHTML =
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" ' +
    'stroke-linecap="round" stroke-linejoin="round">' +
    '<path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>' +
    "</svg>";

  var panel = document.createElement("div");
  panel.id = "pc-chat-panel";
  panel.innerHTML =
    '<div id="pc-chat-header">' +
    '  <div>' +
    '    <div class="pc-title">Product Catalogue</div>' +
    '    <div class="pc-subtitle">Usually replies instantly</div>' +
    "  </div>" +
    '  <button id="pc-chat-close" aria-label="Close chat">&times;</button>' +
    "</div>" +
    '<div id="pc-chat-messages"></div>' +
    '<div id="pc-chat-quick-replies"></div>' +
    '<button id="pc-chat-restart" style="display:none;">Start a new chat</button>' +
    '<div id="pc-chat-inputrow">' +
    '  <input id="pc-chat-input" type="text" placeholder="Type your reply…" autocomplete="off" />' +
    '  <button id="pc-chat-send">Send</button>' +
    "</div>";

  document.addEventListener("DOMContentLoaded", function () {
    document.body.appendChild(panel);
    document.body.appendChild(launcher);
    wireUpEvents();
  });

  // ---- Rendering helpers ----------------------------------------------

  function addMessage(text, from) {
    var messages = document.getElementById("pc-chat-messages");
    var bubble = document.createElement("div");
    bubble.className = "pc-msg " + (from === "user" ? "pc-msg-user" : "pc-msg-bot");
    bubble.textContent = text;
    messages.appendChild(bubble);
    messages.scrollTop = messages.scrollHeight;
  }

  // Turns lines like "0. Airtime" / "n. Next page" / "00. Exit" into
  // tappable quick-reply chips, so people don't have to type a bare
  // number - but they can still type freely in the input box too.
  function renderQuickReplies(messageText) {
    var container = document.getElementById("pc-chat-quick-replies");
    container.innerHTML = "";
    var lines = messageText.split("\n");
    var pattern = /^(\d{1,2}|n)\.\s+(.*)$/i;

    lines.forEach(function (line) {
      var match = line.match(pattern);
      if (!match) return;
      var value = match[1];
      var label = match[0];
      var chip = document.createElement("button");
      chip.className = "pc-quick-reply";
      chip.type = "button";
      chip.textContent = label;
      chip.addEventListener("click", function () {
        sendMessage(value);
      });
      container.appendChild(chip);
    });
  }

  function setSending(isSending) {
    sending = isSending;
    document.getElementById("pc-chat-send").disabled = isSending || chatEnded;
    document.getElementById("pc-chat-input").disabled = isSending || chatEnded;
  }

  function setEnded(ended) {
    chatEnded = ended;
    document.getElementById("pc-chat-restart").style.display = ended ? "block" : "none";
    document.getElementById("pc-chat-send").disabled = ended;
    document.getElementById("pc-chat-input").disabled = ended;
    if (ended) {
      document.getElementById("pc-chat-quick-replies").innerHTML = "";
    }
  }

  // ---- Networking -------------------------------------------------------

  function callApi(message) {
    return fetch(CHAT_ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sessionId: sessionId, message: message || "" }),
    }).then(function (res) {
      if (!res.ok) {
        throw new Error("Chat request failed with status " + res.status);
      }
      return res.json();
    });
  }

  function startChat() {
    document.getElementById("pc-chat-messages").innerHTML = "";
    sessionId = null;
    setEnded(false);
    setSending(true);
    callApi("")
      .then(function (data) {
        sessionId = data.sessionId;
        addMessage(data.message, "bot");
        renderQuickReplies(data.message);
        setEnded(data.ended);
      })
      .catch(function () {
        addMessage("Sorry, something went wrong connecting to the catalogue. Please try again.", "bot");
      })
      .finally(function () {
        setSending(false);
      });
  }

  function sendMessage(text) {
    if (!text || sending || chatEnded) return;
    addMessage(text, "user");
    document.getElementById("pc-chat-quick-replies").innerHTML = "";
    setSending(true);
    callApi(text)
      .then(function (data) {
        sessionId = data.sessionId;
        addMessage(data.message, "bot");
        renderQuickReplies(data.message);
        setEnded(data.ended);
      })
      .catch(function () {
        addMessage("Sorry, something went wrong. Please start a new chat.", "bot");
        setEnded(true);
      })
      .finally(function () {
        setSending(false);
      });
  }

  // ---- Wiring -------------------------------------------------------

  function wireUpEvents() {
    launcher.addEventListener("click", function () {
      var isOpen = panel.classList.toggle("pc-open");
      if (isOpen && !sessionId) {
        startChat();
      }
    });

    document.getElementById("pc-chat-close").addEventListener("click", function () {
      panel.classList.remove("pc-open");
    });

    document.getElementById("pc-chat-restart").addEventListener("click", startChat);

    document.getElementById("pc-chat-send").addEventListener("click", function () {
      var input = document.getElementById("pc-chat-input");
      var value = input.value.trim();
      if (!value) return;
      input.value = "";
      sendMessage(value);
    });

    document.getElementById("pc-chat-input").addEventListener("keydown", function (e) {
      if (e.key === "Enter") {
        document.getElementById("pc-chat-send").click();
      }
    });
  }
})();
