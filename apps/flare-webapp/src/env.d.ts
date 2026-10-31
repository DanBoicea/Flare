// Vite client types come from tsconfig ("types": ["vite/client"]); this only adds our variables.
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_MOCK?: string
  readonly VITE_DEMO_LABEL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
