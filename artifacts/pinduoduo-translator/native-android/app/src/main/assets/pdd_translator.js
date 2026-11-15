(function () {
  "use strict";

  if (window.__pddRussianTranslatorInstalled) return;
  window.__pddRussianTranslatorInstalled = true;

  const chinese = /[\u3400-\u9fff]/;
  const skippedAncestors =
    "script,style,noscript,template,textarea,input,select,option,svg,canvas,math,[contenteditable='true'],[translate='no'],[hidden],[aria-hidden='true']";
  const skippedAttributeAncestors =
    "script,style,noscript,template,svg,canvas,math,[contenteditable='true'],[translate='no'],[hidden],[aria-hidden='true']";
  const attributeSelector = "input[placeholder]:not([type='password']),textarea[placeholder],[title],[aria-label]";
  const pendingById = new Map();
  const lastProcessedText = new WeakMap();
  const inFlightText = new WeakMap();
  const lastProcessedAttributes = new WeakMap();
  const inFlightAttributes = new WeakMap();
  let nextId = 1;
  let queue = [];
  let flushTimer = 0;

  function shouldSkipText(node, text) {
    const parent = node.parentElement;
    if (!parent || !text || text.length > 400 || !chinese.test(text)) return true;
    if (parent.closest(skippedAncestors)) return true;

    const style = window.getComputedStyle(parent);
    return style.display === "none" || style.visibility === "hidden";
  }

  function shouldSkipAttribute(element, text) {
    if (!text || text.length > 400 || !chinese.test(text)) return true;
    if (element.closest(skippedAttributeAncestors)) return true;

    const style = window.getComputedStyle(element);
    return style.display === "none" || style.visibility === "hidden";
  }

  function mapFor(weakMap, element) {
    let map = weakMap.get(element);
    if (!map) {
      map = new Map();
      weakMap.set(element, map);
    }
    return map;
  }

  function addPending(target) {
    const id = String(nextId++);
    const pending = { id, ...target };
    pendingById.set(id, pending);
    queue.push({ id, text: target.source });
    if (!flushTimer) flushTimer = window.setTimeout(flush, 180);
  }

  function queueTextNode(node) {
    const source = node.nodeValue || "";
    if (shouldSkipText(node, source) || lastProcessedText.get(node) === source || inFlightText.has(node)) return;
    inFlightText.set(node, true);
    addPending({ kind: "text", node, source });
  }

  function queueAttribute(element, attribute) {
    const source = element.getAttribute(attribute) || "";
    if (shouldSkipAttribute(element, source)) return;

    const processed = mapFor(lastProcessedAttributes, element);
    const inFlight = mapFor(inFlightAttributes, element);
    if (processed.get(attribute) === source || inFlight.has(attribute)) return;
    inFlight.set(attribute, true);
    addPending({ kind: "attribute", node: element, attribute, source });
  }

  function inspectAttributes(root) {
    if (root.nodeType !== Node.ELEMENT_NODE && root.nodeType !== Node.DOCUMENT_NODE) return;
    if (root.nodeType === Node.ELEMENT_NODE && root.matches(attributeSelector)) {
      ["placeholder", "title", "aria-label"].forEach((attribute) => {
        if (root.hasAttribute(attribute)) queueAttribute(root, attribute);
      });
    }
    root.querySelectorAll(attributeSelector).forEach((element) => {
      ["placeholder", "title", "aria-label"].forEach((attribute) => {
        if (element.hasAttribute(attribute)) queueAttribute(element, attribute);
      });
    });
  }

  function inspect(root) {
    if (!root) return;
    if (root.nodeType === Node.TEXT_NODE) {
      queueTextNode(root);
      return;
    }
    if (root.nodeType !== Node.ELEMENT_NODE && root.nodeType !== Node.DOCUMENT_NODE) return;

    inspectAttributes(root);
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let node;
    while ((node = walker.nextNode())) queueTextNode(node);
  }

  function flush() {
    flushTimer = 0;
    const bridge = window.PinduoduoRussianBridge;
    if (!bridge || typeof bridge.postMessage !== "function" || !queue.length) return;

    const batch = queue.splice(0, 18);
    try {
      bridge.postMessage(JSON.stringify({ items: batch }));
    } catch (_) {
      batch.forEach((item) => {
        const pending = pendingById.get(item.id);
        if (pending) clearPending(pending, pending.source);
      });
    }
    if (queue.length) flushTimer = window.setTimeout(flush, 180);
  }

  function currentValue(pending) {
    return pending.kind === "text"
      ? pending.node.nodeValue || ""
      : pending.node.getAttribute(pending.attribute) || "";
  }

  function setValue(pending, value) {
    if (pending.kind === "text") {
      lastProcessedText.set(pending.node, value);
      pending.node.nodeValue = value;
      return;
    }
    mapFor(lastProcessedAttributes, pending.node).set(pending.attribute, value);
    pending.node.setAttribute(pending.attribute, value);
  }

  function clearInFlight(pending) {
    if (pending.kind === "text") {
      inFlightText.delete(pending.node);
    } else {
      mapFor(inFlightAttributes, pending.node).delete(pending.attribute);
    }
  }

  function clearPending(pending, processedValue) {
    pendingById.delete(pending.id);
    clearInFlight(pending);
    if (pending.kind === "text") {
      lastProcessedText.set(pending.node, processedValue);
    } else {
      mapFor(lastProcessedAttributes, pending.node).set(pending.attribute, processedValue);
    }
  }

  function receive(event) {
    let response;
    try {
      response = JSON.parse(event.data);
    } catch (_) {
      return;
    }

    (response.items || []).forEach((item) => {
      const pending = pendingById.get(String(item.id));
      if (!pending) return;
      const translated = typeof item.text === "string" ? item.text : "";
      const unchanged = currentValue(pending) === pending.source;
      if (!item.error && translated && pending.node.isConnected && unchanged) {
        clearPending(pending, translated);
        if (pending.kind === "text") pending.node.nodeValue = translated;
        else pending.node.setAttribute(pending.attribute, translated);
      } else {
        clearPending(pending, pending.source);
      }
    });
  }

  function install() {
    const bridge = window.PinduoduoRussianBridge;
    if (bridge) bridge.onmessage = receive;

    inspect(document.body);
    const observer = new MutationObserver((records) => {
      records.forEach((record) => {
        if (record.type === "characterData") {
          queueTextNode(record.target);
        } else if (record.type === "attributes") {
          if (record.attributeName) queueAttribute(record.target, record.attributeName);
        } else {
          record.addedNodes.forEach(inspect);
        }
      });
    });
    observer.observe(document.documentElement, {
      subtree: true,
      childList: true,
      characterData: true,
      attributes: true,
      attributeFilter: ["placeholder", "title", "aria-label"],
    });
    window.__pddRussianTranslatorObserver = observer;
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", install, { once: true });
  } else {
    install();
  }
})();
