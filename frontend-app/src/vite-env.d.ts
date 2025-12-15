/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_API_TARGET?: string;
  readonly VITE_USE_HTTPS?: string;
  readonly VITE_HTTPS_KEY_PATH?: string;
  readonly VITE_HTTPS_CERT_PATH?: string;
  readonly DEV?: boolean;
  readonly PROD?: boolean;
  readonly MODE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
