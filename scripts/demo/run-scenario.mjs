#!/usr/bin/env node
// Plays demo scenarios from data/demo-scenarios/ against a Flare back end (task D06).
// Node 22 built-ins only. It sends exactly what the passenger web app sends, so the back end
// cannot tell the difference. All scenario content is simulated.
//
// Usage:
//   node scripts/demo/run-scenario.mjs <scenario|all> [--base-url URL]
//   node scripts/demo/run-scenario.mjs --list

import { randomUUID } from "node:crypto";
import { readFileSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const SCENARIO_DIR = join(ROOT, "data", "demo-scenarios");
const DEFAULT_BASE_URL = "http://localhost:8080";
const TIMEOUT_MS = 20_000;
// The bot check (ADR-0019) rejects pages submitted too fast; a real Passenger takes seconds.
const ELAPSED_MS = 5000;

function usage(exitCode) {
  console.log("Usage: node scripts/demo/run-scenario.mjs <scenario|all> [--base-url URL]");
  console.log("       node scripts/demo/run-scenario.mjs --list");
  process.exit(exitCode);
}

function parseArgs(argv) {
  const args = { target: null, baseUrl: DEFAULT_BASE_URL, list: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--list") args.list = true;
    else if (a === "--base-url") args.baseUrl = argv[++i];
    else if (a.startsWith("--base-url=")) args.baseUrl = a.slice("--base-url=".length);
    else if (a === "--help" || a === "-h") usage(0);
    else if (a.startsWith("--")) { console.error(`Unknown option: ${a}`); usage(1); }
    else args.target = a;
  }
  if (!args.baseUrl) { console.error("--base-url needs a value"); usage(1); }
  args.baseUrl = args.baseUrl.replace(/\/+$/, "");
  return args;
}

function loadScenarios() {
  return readdirSync(SCENARIO_DIR)
    .filter((f) => f.endsWith(".json"))
    .sort() // file-name order; the spam wave is numbered last on purpose
    .map((file) => ({ file, ...JSON.parse(readFileSync(join(SCENARIO_DIR, file), "utf8")) }));
}

function loadQrLabels() {
  const airport = JSON.parse(readFileSync(join(ROOT, "data", "airport", "airport.json"), "utf8"));
  return new Map(airport.locations.map((l) => [l.qrCodeId, l.label]));
}

// "bot-{n}" with repeat 20 becomes bot-1 ... bot-20, each a separate device.
function expandSteps(steps) {
  const out = [];
  for (const step of steps) {
    const times = step.repeat ?? 1;
    for (let n = 1; n <= times; n++) {
      out.push({ ...step, device: String(step.device).replaceAll("{n}", String(n)), text: step.text?.replaceAll("{n}", String(n)) });
    }
  }
  return out;
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function post(baseUrl, path, body) {
  let response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
  } catch (err) {
    return { networkError: err.name === "TimeoutError" ? `no answer within ${TIMEOUT_MS / 1000} s` : err.message };
  }
  let json = null;
  try { json = await response.json(); } catch { /* not JSON */ }
  return { status: response.status, ok: response.ok, json };
}

function describeResult(res) {
  if (res.networkError) return `NETWORK ERROR: ${res.networkError}`;
  if (!res.ok) {
    const code = res.json?.code ? ` ${res.json.code}` : "";
    const hint = res.status === 501 ? " (endpoint not built yet: it is a stub until A06)" : "";
    return `HTTP ${res.status}${code}${hint}`;
  }
  const r = res.json ?? {};
  const parts = [r.outcome ?? "(no outcome)"];
  if (r.answer) parts.push(`answer found=${r.answer.found}`);
  if (r.bounce) parts.push(`reason=${r.bounce.reason}`, `sendAnywayAvailable=${r.bounce.sendAnywayAvailable}`);
  return parts.join("  ");
}

const shorten = (text, max = 60) => (text.length > max ? `${text.slice(0, max - 1)}…` : text);

async function runScenario(scenario, baseUrl, qrLabels) {
  console.log(`\n=== ${scenario.name} (${scenario.file}) ===`);
  console.log(scenario.description);

  // Device labels are per scenario: "d1" here is a different phone from "d1" in another scenario,
  // so one fake phone never hits the per-device limit across scenarios.
  const devices = new Map();
  const deviceId = (label) => {
    if (!devices.has(label)) devices.set(label, randomUUID());
    return devices.get(label);
  };
  const lastQuestionId = new Map();
  let failures = 0;
  const steps = expandSteps(scenario.steps);

  for (const [i, step] of steps.entries()) {
    const prefix = `  ${String(i + 1).padStart(2)}. [${step.device}]`;
    let res;

    if (step.sendToStaff) {
      const questionId = lastQuestionId.get(step.device);
      if (!questionId) {
        console.log(`${prefix} send to staff: SKIPPED, no questionId from an earlier step for this device`);
        failures++;
        continue;
      }
      res = await post(baseUrl, `/api/public/questions/${encodeURIComponent(questionId)}/send-to-staff`, { deviceId: deviceId(step.device) });
      console.log(`${prefix} send to staff → ${describeResult(res)}`);
    } else {
      const where = qrLabels.get(step.qrCodeId) ?? "unknown QR code";
      res = await post(baseUrl, "/api/public/messages", {
        text: step.text,
        uiLanguage: step.uiLanguage,
        qrCodeId: step.qrCodeId ?? null,
        deviceId: deviceId(step.device),
        contact: null,
        sendAnyway: step.sendAnyway === true,
        website: "", // honeypot: always empty, like a real browser
        elapsedMs: ELAPSED_MS,
      });
      const flag = step.sendAnyway ? " (send anyway)" : "";
      console.log(`${prefix} ${where}: "${shorten(step.text)}"${flag} → ${describeResult(res)}`);
      if (res.json?.questionId) lastQuestionId.set(step.device, res.json.questionId);
    }

    if (res.networkError) {
      console.log(`  Stopping this scenario: cannot reach ${baseUrl}. Is the back end running?`);
      return failures + 1;
    }
    if (!res.ok) failures++;
    if (step.waitMs) await sleep(step.waitMs);
  }

  console.log(`  Expect: ${scenario.expect}`);
  console.log("  Check it in the Staff app; the script only sees what a Passenger sees.");
  return failures;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const scenarios = loadScenarios();

  if (args.list) {
    for (const s of scenarios) console.log(`${s.name.padEnd(30)} ${s.description}`);
    return;
  }
  if (!args.target) usage(1);

  const selected = args.target === "all"
    ? scenarios
    : scenarios.filter((s) => s.name === args.target || s.file === args.target || s.file === `${args.target}.json`);
  if (selected.length === 0) {
    console.error(`No scenario named "${args.target}". Use --list to see them.`);
    process.exit(1);
  }

  console.log(`Back end: ${args.baseUrl}`);
  console.log("SIMULATED demo data. This creates real Reports in the database: reset dev first (scripts/sql/reset-dev.sql).");
  const qrLabels = loadQrLabels();
  let failures = 0;
  for (const scenario of selected) failures += await runScenario(scenario, args.baseUrl, qrLabels);

  console.log(failures === 0
    ? "\nAll requests were accepted. Compare each 'Expect' line with the Staff app."
    : `\n${failures} request(s) failed (see above).`);
  process.exit(failures === 0 ? 0 : 1);
}

main();
