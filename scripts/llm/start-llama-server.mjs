// Starts the self-hosted LLM for the back end (ADR-0022, task S04). Node built-ins only.
//
//   node scripts/llm/start-llama-server.mjs
//
// Edit the settings below, or override one for a single run with an environment variable, e.g.
//   LLAMA_MODEL=models/Qwen3.5-9B-Q6_K.gguf node scripts/llm/start-llama-server.mjs
// Stop it with Ctrl+C. The back end finds it at FLARE_LLM_BASE_URL (default http://localhost:8081).

import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { dirname, isAbsolute, join } from 'node:path';
import { fileURLToPath } from 'node:url';

// ---------------------------------------------------------------------------------------------------------
// Settings. Every value can be overridden with the environment variable named next to it.
// ---------------------------------------------------------------------------------------------------------
const SETTINGS = {
  // The GGUF file, relative to the repo root (models/ is git-ignored) or absolute.      LLAMA_MODEL
  model: 'models/gemma-4-12b-it-Q5_K_M.gguf',
  // The name the back end asks for (flare.llm.tasks.*.model in application.yml).       LLAMA_ALIAS
  alias: 'flare-llm',
  // 127.0.0.1 keeps it on this machine. Anything else needs an API key (see below).    LLAMA_HOST
  host: '127.0.0.1',
  // The back end runs on 8080, so the LLM uses 8081.                                    LLAMA_PORT
  port: 8081,
  // Requests handled at the same time. 1 is fastest on one laptop: a second slot      LLAMA_SLOTS
  // halves each request's speed and can lose the cached prompt; queued requests then
  // wait their turn (measured in S04: a burst of 3 finished at 8, 17 and 26 s).
  slots: 1,
  // Tokens per slot. One classification needs about 6,500 (Knowledge base included).   LLAMA_CONTEXT_PER_SLOT
  contextPerSlot: 8192,
  // KV cache precision: f16 (more memory) or q8_0 (about half, needs flash attention). LLAMA_KV_CACHE_TYPE
  kvCacheType: 'q8_0',
  // Flash attention: on, off or auto.                                                   LLAMA_FLASH_ATTENTION
  flashAttention: 'on',
  // Keep the whole sliding-window cache. Not needed: with checkpoints (below), the      LLAMA_SWA_FULL
  // long system prompt is reused anyway (measured in S04). Costs memory on Gemma.
  swaFull: false,
  // Saved snapshots per slot that let a cached prompt be reused on sliding-window      LLAMA_CTX_CHECKPOINTS
  // models. About 170 MiB each on Gemma 4 12B; one per slot is all a request needs.
  // llama-server's default (32) could reach about 11 GB with 2 slots.
  ctxCheckpoints: 4,
  // Prompt cache in normal RAM, in MiB, where a slot's cached prompt is kept while     LLAMA_CACHE_RAM_MIB
  // another prompt (match, summarise) uses the slot. The classify state alone is about
  // 724 MiB; with 1024 it was evicted after every match call and passengers waited
  // 37 s instead of 9 (C03). 2048 keeps classify and match. llama-server's default is 8192.
  cacheRamMib: 2048,
  // Thinking mode: off, because we want short JSON answers, not reasoning.             LLAMA_REASONING
  reasoning: 'off',
  // Required whenever host is not 127.0.0.1. Share it privately; never commit it.      FLARE_LLM_API_KEY
  apiKey: '',
  // Browser origins allowed to call it. Only our back end calls it (server to server,   LLAMA_CORS_ORIGINS
  // no CORS involved), so 'localhost' stops any website open in a browser from using it.
  // llama-server's own default is '*' (every origin).
  corsOrigins: 'localhost',
  // The llama-server binary, if it isn't on PATH.                                       LLAMA_SERVER_BIN
  binary: 'llama-server',
  // Any other llama-server flags, space-separated, e.g. "--metrics".                    LLAMA_EXTRA_ARGS
  extraArgs: '',
};
// ---------------------------------------------------------------------------------------------------------

const env = process.env;
const settings = {
  model: env.LLAMA_MODEL ?? SETTINGS.model,
  alias: env.LLAMA_ALIAS ?? SETTINGS.alias,
  host: env.LLAMA_HOST ?? SETTINGS.host,
  port: Number(env.LLAMA_PORT ?? SETTINGS.port),
  slots: Number(env.LLAMA_SLOTS ?? SETTINGS.slots),
  contextPerSlot: Number(env.LLAMA_CONTEXT_PER_SLOT ?? SETTINGS.contextPerSlot),
  kvCacheType: env.LLAMA_KV_CACHE_TYPE ?? SETTINGS.kvCacheType,
  flashAttention: env.LLAMA_FLASH_ATTENTION ?? SETTINGS.flashAttention,
  swaFull: (env.LLAMA_SWA_FULL ?? String(SETTINGS.swaFull)) === 'true',
  ctxCheckpoints: Number(env.LLAMA_CTX_CHECKPOINTS ?? SETTINGS.ctxCheckpoints),
  cacheRamMib: Number(env.LLAMA_CACHE_RAM_MIB ?? SETTINGS.cacheRamMib),
  reasoning: env.LLAMA_REASONING ?? SETTINGS.reasoning,
  apiKey: env.FLARE_LLM_API_KEY ?? SETTINGS.apiKey,
  corsOrigins: env.LLAMA_CORS_ORIGINS ?? SETTINGS.corsOrigins,
  binary: env.LLAMA_SERVER_BIN ?? SETTINGS.binary,
  extraArgs: env.LLAMA_EXTRA_ARGS ?? SETTINGS.extraArgs,
};

const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const modelPath = isAbsolute(settings.model) ? settings.model : join(repoRoot, settings.model);

if (!existsSync(modelPath)) {
  console.error(`Model file not found: ${modelPath}\nDownload it first (see docs/tasks/S04-llama-server-and-model.md).`);
  process.exit(1);
}
const local = settings.host === '127.0.0.1' || settings.host === 'localhost';
if (!local && !settings.apiKey) {
  console.error(`Refusing to listen on ${settings.host} without an API key: set FLARE_LLM_API_KEY.`);
  process.exit(1);
}

// llama-server splits the total context evenly across the slots.
const args = [
  '-m', modelPath,
  '--alias', settings.alias,
  '--host', settings.host,
  '--port', String(settings.port),
  '-np', String(settings.slots),
  '-c', String(settings.slots * settings.contextPerSlot),
  '-fa', settings.flashAttention,
  '-ctk', settings.kvCacheType,
  '-ctv', settings.kvCacheType,
  '--reasoning', settings.reasoning,
  '--cors-origins', settings.corsOrigins,
  '--ctx-checkpoints', String(settings.ctxCheckpoints),
  '--cache-ram', String(settings.cacheRamMib),
  ...(settings.swaFull ? ['--swa-full'] : []),
  ...(settings.apiKey ? ['--api-key', settings.apiKey] : []),
  ...settings.extraArgs.split(' ').filter(Boolean),
];

const shown = args.map((arg, i) => (args[i - 1] === '--api-key' ? '<hidden>' : arg));
console.log(`Starting: ${settings.binary} ${shown.join(' ')}`);
console.log(`Ready when http://${local ? '127.0.0.1' : settings.host}:${settings.port}/health returns {"status":"ok"}.\n`);

const server = spawn(settings.binary, args, { stdio: 'inherit' });
server.on('error', (error) => {
  console.error(`Could not start ${settings.binary}: ${error.message}. Is llama.cpp installed and on PATH?`);
  process.exit(1);
});
server.on('exit', (code, signal) => process.exit(code ?? (signal ? 1 : 0)));
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => server.kill(signal));
}
