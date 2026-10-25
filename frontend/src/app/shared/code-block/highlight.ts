import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import dockerfile from 'highlight.js/lib/languages/dockerfile';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import plaintext from 'highlight.js/lib/languages/plaintext';
import sql from 'highlight.js/lib/languages/sql';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';

/**
 * Only the languages the questions use, to keep the bundle small. Keep in sync with
 * questions/schema.json and the backend's QuestionFilesContentTest.
 */
const LANGUAGES = {
  bash,
  dockerfile,
  java,
  javascript,
  json,
  plaintext,
  sql,
  typescript,
  xml,
  yaml,
};
for (const [name, definition] of Object.entries(LANGUAGES)) {
  hljs.registerLanguage(name, definition);
}
hljs.registerAliases(['html'], { languageName: 'xml' });

/** Highlighted HTML for the source. hljs escapes the source, so the result is safe to bind. */
export function highlight(source: string, language: string): string {
  const known = hljs.getLanguage(language) ? language : 'plaintext';
  return hljs.highlight(source, { language: known }).value;
}
