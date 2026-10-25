import fs from 'node:fs';
import path from 'node:path';
import React from 'react';
import { renderToString } from 'react-dom/server';
import App from './dist-ssr/App.js';

const distIndexPath = path.resolve('./dist/index.html');
const template = fs.readFileSync(distIndexPath, 'utf-8');

// Render the complete React component tree to static HTML string
const appHtml = renderToString(React.createElement(App));

console.log(`Rendered app HTML length: ${appHtml.length} characters`);

// Inject the pre-rendered HTML directly into <div id="root"></div>
const finalHtml = template.replace(
  '<div id="root"></div>',
  `<div id="root">${appHtml}</div>`
);

fs.writeFileSync(distIndexPath, finalHtml, 'utf-8');
console.log('Successfully injected pre-rendered HTML into dist/index.html');

// Copy build outputs to ../docs for GitHub Pages /docs folder deployment
const docsDir = path.resolve('../docs');
if (!fs.existsSync(docsDir)) {
  fs.mkdirSync(docsDir, { recursive: true });
}

// Copy dist/ to docs/
fs.cpSync(path.resolve('./dist'), docsDir, { recursive: true });
// Ensure .nojekyll is in docs/ so GitHub Pages bypasses Jekyll
fs.writeFileSync(path.join(docsDir, '.nojekyll'), '', 'utf-8');

console.log('Successfully copied complete website build to docs/ directory');
