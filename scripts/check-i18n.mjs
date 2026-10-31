#!/usr/bin/env node
// Checks that the Romanian and English string files of both apps match (task D04).
// Node 22 built-ins only. Exits with code 1 on any problem.
//
// Usage: node scripts/check-i18n.mjs

import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const APPS = ["flare-webapp", "flare-mobile"];

// Key families built at runtime from server values, e.g. t(`severity.${issue.severity}`).
// TypeScript cannot check those keys, so a missing value would show a raw key on screen.
// A family is only checked in an app that already uses it.
const FAMILIES = {
  bounce: "BounceReason",
  "bounce.title": "BounceReason",
  category: "Category",
  severity: "Severity",
  team: "TeamId",
  role: "StaffRole",
  status: "IssueStatus",
  reviewReason: "QuarantineReason",
  history: "HistoryAction",
};

// Romanian uses ș and ț with a comma below; ş and ţ (cedilla) are look-alikes from old keyboards.
const CEDILLA = /[şţŞŢ]/;
const PLACEHOLDER = /\{(\w+)\}/g;

function readJson(path) {
  try {
    return JSON.parse(readFileSync(path, "utf8"));
  } catch (err) {
    return { error: `${path}: ${err.code === "ENOENT" ? "file not found" : err.message}` };
  }
}

// Enum values from contract/openapi.yaml, read with a regex so no YAML library is needed.
// Handles both `Name: { type: string, enum: [A, B] }` and `Name:` followed by an `enum: [A, B]` line.
function readContractEnums() {
  const lines = readFileSync(join(ROOT, "contract", "openapi.yaml"), "utf8").split("\n");
  const enums = {};
  let current = null;
  for (const line of lines) {
    const name = line.match(/^ {4}(\w+):/);
    if (name) current = name[1];
    const values = line.match(/enum:\s*\[([^\]]*)\]/);
    if (current && values && !enums[current]) {
      enums[current] = values[1].split(",").map((v) => v.trim()).filter(Boolean);
    }
  }
  return enums;
}

const placeholders = (text) => [...String(text).matchAll(PLACEHOLDER)].map((m) => m[1]).sort();

function checkApp(app, enums) {
  const dir = join(ROOT, "apps", app, "src", "i18n");
  const ro = readJson(join(dir, "ro.json"));
  const en = readJson(join(dir, "en.json"));
  const problems = [];
  if (ro.error || en.error) return [ro.error, en.error].filter(Boolean);

  for (const key of Object.keys(ro)) if (!(key in en)) problems.push(`missing in en.json: ${key}`);
  for (const key of Object.keys(en)) if (!(key in ro)) problems.push(`missing in ro.json: ${key}`);

  for (const [lang, file] of [["ro", ro], ["en", en]]) {
    for (const [key, value] of Object.entries(file)) {
      if (typeof value !== "string") problems.push(`${lang}.json: ${key} is not a string`);
      else if (value.trim() === "") problems.push(`${lang}.json: ${key} is empty`);
    }
  }

  for (const key of Object.keys(ro)) {
    if (!(key in en)) continue;
    const a = placeholders(ro[key]).join(", ");
    const b = placeholders(en[key]).join(", ");
    if (a !== b) problems.push(`placeholders differ in ${key}: ro {${a}} vs en {${b}}`);
  }

  for (const [key, value] of Object.entries(ro)) {
    if (CEDILLA.test(value)) problems.push(`ro.json: ${key} uses ş/ţ with a cedilla; use ș/ț with a comma below`);
  }

  for (const [prefix, enumName] of Object.entries(FAMILIES)) {
    const values = enums[enumName];
    if (!values) { problems.push(`contract has no enum ${enumName} (used for ${prefix}.*)`); continue; }
    const present = values.filter((v) => `${prefix}.${v}` in ro);
    if (present.length === 0) continue; // family not used by this app yet
    for (const v of values) if (!present.includes(v)) problems.push(`missing ${prefix}.${v} (every ${enumName} value needs a string)`);
  }

  return problems;
}

const enums = readContractEnums();
let total = 0;
for (const app of APPS) {
  const problems = checkApp(app, enums);
  total += problems.length;
  if (problems.length === 0) {
    console.log(`${app}: OK`);
  } else {
    console.log(`${app}: ${problems.length} problem(s)`);
    for (const p of problems) console.log(`  - ${p}`);
  }
}
process.exit(total === 0 ? 0 : 1);
