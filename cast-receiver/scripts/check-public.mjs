import { readFileSync, readdirSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("..", import.meta.url));
const files = (folder) => readdirSync(folder).flatMap((entry) => {
  const path = join(folder, entry);
  return statSync(path).isDirectory() && entry !== "node_modules" && entry !== "dist" ? files(path) : [path];
});
const forbidden = [/(?:api[_-]?key|access[_-]?token|x-emby-token)\s*[:=]\s*["'][^"']{8,}/i, /https?:\/\/(?:10\.|192\.168\.|172\.(?:1[6-9]|2\d|3[01])\.)/i];
const matches = files(root).filter((file) => /\.(?:ts|html|css|json|mjs)$/.test(file)).filter((file) => {
  const source = readFileSync(file, "utf8");
  return forbidden.some((pattern) => pattern.test(source));
});
if (matches.length) throw new Error(`Ikkje-offentleg mottakarkonfigurasjon: ${matches.join(", ")}`);
