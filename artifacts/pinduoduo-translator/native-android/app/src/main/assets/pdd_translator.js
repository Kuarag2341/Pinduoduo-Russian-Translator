(function () {
  "use strict";

  if (window.__pddRussianTranslatorInstalled) return;
  window.__pddRussianTranslatorInstalled = true;

  const chinese = /[\u3400-\u9fff]/;
  const skippedAncestors =
    "script,style,noscript,template,textarea,input,select,option,svg,canvas,math,[contenteditable='true'],[translate='no'],[hidden],[aria-hidden='true']";
  const skippedAttributeAncestors =
    "script,style,noscript,template,svg,canvas,math,[contenteditable='true'],[translate='no'],[hidden],[aria-hidden='true']";
  const buttonValueSelector = "input[type='button'],input[type='submit'],input[type='reset']";
  const attributeSelector =
    "input[placeholder]:not([type='password']),textarea[placeholder],[title],[aria-label]," +
    "input[type='button'][value],input[type='submit'][value],input[type='reset'][value]";
  const visibilityAttributes = new Set([
    "class",
    "style",
    "hidden",
    "aria-hidden",
    "aria-expanded",
    "open",
  ]);
  const textAttributes = new Set(["placeholder", "title", "aria-label", "value"]);
  const maxTextLength = 1200;
  const pendingById = new Map();
  const lastProcessedText = new WeakMap();
  const inFlightText = new WeakMap();
  const lastProcessedAttributes = new WeakMap();
  const inFlightAttributes = new WeakMap();
  const retryAfterText = new WeakMap();
  const retryAfterAttributes = new WeakMap();
  const rescanRoots = new Set();
  let nextId = 1;
  let queue = [];
  let flushTimer = 0;
  let rescanTimer = 0;
  let viewportScanTimer = 0;

  function isRendered(element) {
    if (!element || !element.isConnected) return false;
    for (let current = element; current && current.nodeType === Node.ELEMENT_NODE; current = current.parentElement) {
      const style = window.getComputedStyle(current);
      if (
        style.display === "none" ||
        style.visibility === "hidden" ||
        style.contentVisibility === "hidden"
      ) return false;
    }
    return true;
  }

  function isInViewport(element) {
    if (!isRendered(element)) return false;
    const rect = element.getBoundingClientRect();
    return rect.bottom >= 0 && rect.right >= 0 &&
      rect.top <= window.innerHeight && rect.left <= window.innerWidth;
  }

  function textNodeInViewport(node) {
    if (!node.parentElement || !isRendered(node.parentElement)) return false;
    const range = document.createRange();
    range.selectNode(node);
    const rects = range.getClientRects();
    for (let index = 0; index < rects.length; index += 1) {
      const rect = rects[index];
      if (rect.bottom >= 0 && rect.right >= 0 &&
        rect.top <= window.innerHeight && rect.left <= window.innerWidth) return true;
    }
    return false;
  }

  function shouldSkipText(node, text) {
    const parent = node.parentElement;
    return !parent || !text || text.length > maxTextLength ||
      !chinese.test(text) || parent.closest(skippedAncestors) !== null || !isRendered(parent);
  }

  function shouldSkipAttribute(element, text) {
    return !text || text.length > maxTextLength ||
      !chinese.test(text) || !isRendered(element) ||
      element.closest(skippedAttributeAncestors) !== null;
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
    const retry = retryAfterText.get(node);
    if (retry && retry.source === source && Date.now() < retry.until) return;
    if (retry && retry.source !== source) retryAfterText.delete(node);
    inFlightText.set(node, true);
    addPending({ kind: "text", node, source });
  }

  function queueAttribute(element, attribute) {
    const source = element.getAttribute(attribute) || "";
    if (!textAttributes.has(attribute)) return;
    if (attribute === "value" && !element.matches(buttonValueSelector)) return;
    if (shouldSkipAttribute(element, source)) return;

    const processed = mapFor(lastProcessedAttributes, element);
    const inFlight = mapFor(inFlightAttributes, element);
    if (processed.get(attribute) === source || inFlight.has(attribute)) return;
    const retries = mapFor(retryAfterAttributes, element);
    const retry = retries.get(attribute);
    if (retry && retry.source === source && Date.now() < retry.until) return;
    if (retry && retry.source !== source) retries.delete(attribute);
    inFlight.set(attribute, true);
    addPending({ kind: "attribute", node: element, attribute, source });
  }

  function attributeNames(element) {
    const names = ["placeholder", "title", "aria-label"].filter((name) => element.hasAttribute(name));
    if (element.matches(buttonValueSelector) && element.hasAttribute("value")) names.push("value");
    return names;
  }

  function inspectAttributes(root) {
    if (root.nodeType !== Node.ELEMENT_NODE && root.nodeType !== Node.DOCUMENT_NODE) return;
    if (root.nodeType === Node.ELEMENT_NODE && root.matches(attributeSelector)) {
      attributeNames(root).forEach((attribute) => queueAttribute(root, attribute));
    }
    root.querySelectorAll(attributeSelector).forEach((element) =>
      attributeNames(element).forEach((attribute) => queueAttribute(element, attribute))
    );
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

  function inspectVisibleViewport() {
    if (!document.body) return;
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    let node;
    while ((node = walker.nextNode())) {
      if (textNodeInViewport(node)) queueTextNode(node);
    }
    document.querySelectorAll(attributeSelector).forEach((element) => {
      if (isInViewport(element)) {
        attributeNames(element).forEach((attribute) => queueAttribute(element, attribute));
      }
    });
  }

  function scheduleViewportScan() {
    if (viewportScanTimer) window.clearTimeout(viewportScanTimer);
    viewportScanTimer = window.setTimeout(() => {
      viewportScanTimer = 0;
      inspectVisibleViewport();
    }, 200);
  }

  function scheduleSubtreeRescan(root) {
    rescanRoots.add(root);
    if (rescanTimer) return;
    rescanTimer = window.setTimeout(() => {
      rescanTimer = 0;
      const roots = Array.from(rescanRoots);
      rescanRoots.clear();
      roots.forEach(inspect);
    }, 120);
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

  function clearPending(pending, processedValue, markProcessed = true) {
    pendingById.delete(pending.id);
    clearInFlight(pending);
    if (!markProcessed) return;
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
        const markProcessed = pending.node.isConnected && unchanged && !item.error;
        clearPending(pending, pending.source, markProcessed);
        if (item.error && pending.node.isConnected && unchanged) {
          const retry = { source: pending.source, until: Date.now() + 30000 };
          if (pending.kind === "text") retryAfterText.set(pending.node, retry);
          else mapFor(retryAfterAttributes, pending.node).set(pending.attribute, retry);
        }
        if (pending.node.isConnected && !unchanged) {
          if (pending.kind === "text") queueTextNode(pending.node);
          else queueAttribute(pending.node, pending.attribute);
        }
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
          const attribute = record.attributeName;
          if (!attribute) return;
          if (visibilityAttributes.has(attribute)) {
            scheduleSubtreeRescan(record.target);
          } else if (textAttributes.has(attribute)) {
            queueAttribute(record.target, attribute);
          }
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
      attributeFilter: [
        "placeholder",
        "title",
        "aria-label",
        "value",
        "class",
        "style",
        "hidden",
        "aria-hidden",
        "aria-expanded",
        "open",
      ],
    });
    window.addEventListener("scroll", scheduleViewportScan, { passive: true, capture: true });
    document.addEventListener("visibilitychange", scheduleViewportScan);
    window.__pddRussianTranslatorObserver = observer;
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", install, { once: true });
  } else {
    install();
  }
})();
