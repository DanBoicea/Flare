// Starts the self-hosted LLM for the back end (ADR-0022, task S04). Node built-ins only.
//
//   node scripts/llm/start-llama-server.mjs                       use the local model file (models/, the default)
//   node scripts/llm/start-llama-server.mjs --model <path.gguf>   use a model file somewhere else
//   node scripts/llm/start-llama-server.mjs --download            let llama-server download the model from Hugging Face
//
// If the local file is missing, the script asks whether to let llama-server download it or to stop so you can
// download it yourself. Edit the settings below, or override one for a single run with an environment variable, e.g.
//   LLAMA_MODEL=models/Qwen3.5-9B-Q6_K.gguf node scripts/llm/start-llama-server.mjs
// Stop it with Ctrl+C. The back end finds it at FLARE_LLM_BASE_URL (default http://localhost:8081).

import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { dirname, isAbsolute, join, relative } from 'node:path';
import { createInterface } from 'node:readline/promises';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

// ---------------------------------------------------------------------------------------------------------
// Settings. Every value can be overridden with the environment variable named next to it.
// ---------------------------------------------------------------------------------------------------------
const SETTINGS = {
  // The GGUF file, relative to the repo root (models/ is git-ignored) or absolute.      LLAMA_MODEL
  model: 'models/gemma-4-12b-it-Q5_K_M.gguf',
  // Where --download gets the same file: the Hugging Face repo and file (S04, ADR-0016). LLAMA_HF_REPO
  hfRepo: 'unsloth/gemma-4-12b-it-GGUF',                                             // LLAMA_HF_FILE
  hfFile: 'gemma-4-12b-it-Q5_K_M.gguf',
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

const USAGE = `Usage: node scripts/llm/start-llama-server.mjs [--model <path.gguf>] [--download]

  --model <path>   The model file to load (default: ${SETTINGS.model}, relative to the repo root).
  --download       Let llama-server download ${SETTINGS.hfFile} from Hugging Face (about 8.4 GB, once;
                   it is kept in llama.cpp's own cache and reused on the next --download run).
  --help           Show this help.`;

let options;
try {
  ({ values: options } = parseArgs({
    options: { model: { type: 'string' }, download: { type: 'boolean' }, help: { type: 'boolean' } },
  }));
} catch (error) {
  console.error(`${error.message}\n\n${USAGE}`);
  process.exit(1);
}
if (options.help) {
  console.log(USAGE);
  process.exit(0);
}
if (options.model && options.download) {
  console.error(`Use either --model or --download, not both.\n\n${USAGE}`);
  process.exit(1);
}

const env = process.env;
const settings = {
  model: options.model ?? env.LLAMA_MODEL ?? SETTINGS.model,
  hfRepo: env.LLAMA_HF_REPO ?? SETTINGS.hfRepo,
  hfFile: env.LLAMA_HF_FILE ?? SETTINGS.hfFile,
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
const downloadUrl = `https://huggingface.co/${settings.hfRepo}/resolve/main/${settings.hfFile}`;

let download = Boolean(options.download);
if (!download && !existsSync(modelPath)) {
  download = await askToDownload();
}
const local = settings.host === '127.0.0.1' || settings.host === 'localhost';
if (!local && !settings.apiKey) {
  console.error(`Refusing to listen on ${settings.host} without an API key: set FLARE_LLM_API_KEY.`);
  process.exit(1);
}

// llama-server splits the total context evenly across the slots.
// --no-mmproj: the repo also has an image projector, which Flare never uses; skipping it saves memory.
const modelArgs = download
  ? ['-hf', settings.hfRepo, '--hf-file', settings.hfFile, '--no-mmproj']
  : ['-m', modelPath];
const args = [
  ...modelArgs,
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
console.log(`Ready when http://${local ? '127.0.0.1' : settings.host}:${settings.port}/health returns {"status":"ok"}.`);
if (download) console.log('The first --download run fetches about 8.4 GB before the server is ready; later runs reuse the cache.');
console.log('');

const server = spawn(settings.binary, args, { stdio: 'inherit' });
server.on('error', (error) => {
  console.error(`Could not start ${settings.binary}: ${error.message}. Is llama.cpp installed and on PATH?`);
  process.exit(1);
});
server.on('exit', (code, signal) => process.exit(code ?? (signal ? 1 : 0)));
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => server.kill(signal));
}

/** The local model is missing: warn, then let the user pick llama-server's download or their own. */
async function askToDownload() {
  const shownPath = relative(process.cwd(), modelPath) || modelPath;
  const ownDownload = [
    'To download it yourself (about 8.4 GB), run from the repo root:',
    '',
    `  macOS / Linux:       mkdir -p ${dirname(settings.model)} && curl -L -o ${settings.model} ${downloadUrl}`,
    `  Windows PowerShell:  mkdir -Force ${dirname(settings.model)}; curl.exe -L -o ${settings.model} ${downloadUrl}`,
    '',
    `or open that link in a browser and save the file as ${settings.model}.`,
    `Then start the LLM again with: node scripts/llm/start-llama-server.mjs${options.model ? ` --model ${options.model}` : ''}`,
  ].join('\n');

  console.warn(`\nWARNING: model file not found: ${shownPath}\n`);
  if (!process.stdin.isTTY) {
    console.warn(`${ownDownload}\n\nOr let llama-server download it: node scripts/llm/start-llama-server.mjs --download`);
    process.exit(1);
  }

  console.log('How do you want to continue?');
  console.log(`  1) Let llama-server download it now (about 8.4 GB from Hugging Face, kept in llama.cpp's cache)`);
  console.log('  2) I will download it myself and start the LLM later');
  const prompt = createInterface({ input: process.stdin, output: process.stdout });
  let answer = '';
  while (answer !== '1' && answer !== '2') {
    answer = (await prompt.question('Choose 1 or 2: ')).trim();
  }
  prompt.close();

  if (answer === '1') {
    console.log('\nNext time, start it with --download to reuse the downloaded copy instead of looking in models/.');
    return true;
  }
  console.log(`\n${ownDownload}`);
  process.exit(0);
}
