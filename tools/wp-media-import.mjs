#!/usr/bin/env node
/**
 * wp-media-import.mjs
 *
 * Descarga imágenes desde la WordPress REST API, las registra en la biblioteca
 * de medios de TrastCMS y reemplaza las URLs externas en los JSON de contenido.
 *
 * Uso:
 *   node tools/wp-media-import.mjs --wp-url https://wordpress.example.com --user admin --password secret
 *   node tools/wp-media-import.mjs --wp-url https://wordpress.example.com --dry-run
 *
 * Flags:
 *   --wp-url       URL base del sitio WordPress (requerido)
 *   --api-url      URL de TrastCMS (default: http://localhost:8080)
 *   --user         Usuario admin de TrastCMS
 *   --password     Contraseña del admin
 *   --folder       Carpeta en la biblioteca de medios (default: "WordPress Import")
 *   --dry-run      Solo mostrar qué haría sin descargar/modificar
 *   --skip-upload  Descargar pero no subir a TrastCMS
 *   --replace      Reemplazar URLs en los JSON de contenido después de subir
 */

import { existsSync, mkdirSync, readFileSync, writeFileSync, createReadStream, statSync } from 'node:fs';
import { join, basename, extname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const PROJECT_ROOT = resolve(__dirname, '..');
const CACHE_DIR = join(__dirname, '.wp-import-cache');

// ─── CLI Argument Parsing ───────────────────────────────────────────────────
function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    wpUrl: '',
    apiUrl: 'http://localhost:8080',
    user: '',
    password: '',
    folder: 'WordPress Import',
    dryRun: false,
    skipUpload: false,
    replace: false,
  };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--wp-url':     opts.wpUrl = args[++i]; break;
      case '--api-url':    opts.apiUrl = args[++i]; break;
      case '--user':       opts.user = args[++i]; break;
      case '--password':   opts.password = args[++i]; break;
      case '--folder':     opts.folder = args[++i]; break;
      case '--dry-run':    opts.dryRun = true; break;
      case '--skip-upload': opts.skipUpload = true; break;
      case '--replace':    opts.replace = true; break;
      default:
        console.error(`⚠️  Unknown argument: ${args[i]}`);
        process.exit(1);
    }
  }

  if (!opts.wpUrl) {
    console.error('❌ --wp-url is required. Example: --wp-url https://wordpress.example.com');
    process.exit(1);
  }

  opts.wpUrl = opts.wpUrl.replace(/\/+$/, '');
  opts.apiUrl = opts.apiUrl.replace(/\/+$/, '');
  return opts;
}

// ─── WordPress URL Normalizer ───────────────────────────────────────────────
/**
 * Strips WordPress image optimization suffixes to find the original full-size
 * file. Handles patterns like:
 *   -scaled.jpg → .jpg
 *   -rotated.jpg → .jpg
 *   -150x150.jpg → .jpg
 *   -1024x683.jpg → .jpg
 */
function normalizeWpUrl(url) {
  return url
    .replace(/-scaled(\.[a-z]+)$/i, '$1')
    .replace(/-rotated(\.[a-z]+)$/i, '$1')
    .replace(/-\d+x\d+(\.[a-z]+)$/i, '$1');
}

/**
 * Generates a safe local filename from a URL.
 */
function urlToLocalFilename(url) {
  const parsed = new URL(url);
  const parts = parsed.pathname.split('/');
  return parts[parts.length - 1]
    .replace(/[^a-zA-Z0-9._-]/g, '_')
    .toLowerCase();
}

// ─── Phase 1: WordPress Media Discovery ─────────────────────────────────────
async function fetchWpMedia(wpUrl) {
  const items = [];
  let page = 1;
  const perPage = 100;

  console.log('\n📡 Phase 1: Discovering WordPress media library...');

  while (true) {
    const url = `${wpUrl}/wp-json/wp/v2/media?per_page=${perPage}&page=${page}&media_type=image`;
    console.log(`   Fetching page ${page}...`);

    const response = await fetch(url);
    if (!response.ok) {
      if (page > 1) break; // No more pages
      throw new Error(`WordPress API error: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    if (!data.length) break;

    for (const item of data) {
      // Prefer the full-size original from media_details
      const fullUrl = item.media_details?.sizes?.full?.source_url || item.source_url;
      const originalUrl = normalizeWpUrl(fullUrl);

      items.push({
        wpId: item.id,
        title: item.title?.rendered || '',
        altText: item.alt_text || '',
        caption: (item.caption?.rendered || '').replace(/<[^>]*>/g, '').trim(),
        slug: item.slug || '',
        sourceUrl: item.source_url,
        originalUrl,
        mimeType: item.mime_type || 'image/jpeg',
        width: item.media_details?.width || null,
        height: item.media_details?.height || null,
        filesize: item.media_details?.filesize || 0,
      });
    }

    const totalPages = parseInt(response.headers.get('X-WP-TotalPages') || '1');
    if (page >= totalPages) break;
    page++;
  }

  console.log(`   ✅ Found ${items.length} media items`);
  return items;
}

// ─── Phase 2: Download Images ───────────────────────────────────────────────
async function downloadImage(url, localPath) {
  if (existsSync(localPath)) {
    const stat = statSync(localPath);
    if (stat.size > 1000) { // Skip if already downloaded and not empty/corrupt
      return { cached: true, size: stat.size };
    }
  }

  const response = await fetch(url);
  if (!response.ok) {
    // Try the original URL with -scaled variant if original fails
    const scaledUrl = url.replace(/(\.[a-z]+)$/i, '-scaled$1');
    if (scaledUrl !== url) {
      const retry = await fetch(scaledUrl);
      if (retry.ok) {
        const buffer = Buffer.from(await retry.arrayBuffer());
        writeFileSync(localPath, buffer);
        return { cached: false, size: buffer.length, fallback: scaledUrl };
      }
    }
    throw new Error(`HTTP ${response.status} for ${url}`);
  }

  const buffer = Buffer.from(await response.arrayBuffer());
  writeFileSync(localPath, buffer);
  return { cached: false, size: buffer.length };
}

async function downloadAll(items, opts) {
  console.log('\n📥 Phase 2: Downloading images...');

  if (!existsSync(CACHE_DIR)) mkdirSync(CACHE_DIR, { recursive: true });

  const concurrency = 3;
  const results = [];
  let completed = 0;

  // Deduplicate by normalized URL
  const uniqueMap = new Map();
  for (const item of items) {
    const key = normalizeWpUrl(item.originalUrl);
    if (!uniqueMap.has(key)) uniqueMap.set(key, item);
  }

  const uniqueItems = [...uniqueMap.values()];
  console.log(`   ${items.length} total → ${uniqueItems.length} unique images (deduplicated)`);

  // Process in batches
  for (let i = 0; i < uniqueItems.length; i += concurrency) {
    const batch = uniqueItems.slice(i, i + concurrency);
    const batchResults = await Promise.allSettled(
      batch.map(async (item) => {
        const filename = urlToLocalFilename(item.originalUrl);
        const localPath = join(CACHE_DIR, filename);

        if (opts.dryRun) {
          console.log(`   🔍 [DRY-RUN] Would download: ${item.originalUrl}`);
          return { item, localPath, dryRun: true };
        }

        try {
          const result = await downloadImage(item.originalUrl, localPath);
          completed++;
          const sizeKB = (result.size / 1024).toFixed(1);
          const status = result.cached ? '💾 cached' : '⬇️  downloaded';
          console.log(`   ${status} (${sizeKB} KB) [${completed}/${uniqueItems.length}] ${basename(localPath)}`);
          return { item, localPath, ...result };
        } catch (error) {
          completed++;
          console.log(`   ❌ FAILED [${completed}/${uniqueItems.length}] ${item.originalUrl}: ${error.message}`);
          return { item, localPath, error: error.message };
        }
      })
    );

    results.push(...batchResults.map(r => r.status === 'fulfilled' ? r.value : { error: r.reason?.message }));
  }

  const successful = results.filter(r => !r.error && !r.dryRun);
  const failed = results.filter(r => r.error);
  console.log(`   ✅ ${successful.length} downloaded, ${failed.length} failed`);

  return results;
}

// ─── Phase 3: Upload to TrastCMS ────────────────────────────────────────────
class TrastCmsClient {
  constructor(apiUrl) {
    this.apiUrl = apiUrl;
    this.cookies = '';
    this.csrfToken = '';
  }

  async login(username, password) {
    // Step 1: Get CSRF token
    console.log('   🔑 Getting CSRF token...');
    const csrfRes = await fetch(`${this.apiUrl}/api/auth/csrf`, {
      credentials: 'include',
      redirect: 'manual',
    });

    // Extract cookies
    const setCookies = csrfRes.headers.getSetCookie?.() || [];
    this.cookies = setCookies.map(c => c.split(';')[0]).join('; ');

    // Extract XSRF-TOKEN from cookies
    for (const cookie of setCookies) {
      const match = cookie.match(/XSRF-TOKEN=([^;]+)/);
      if (match) this.csrfToken = decodeURIComponent(match[1]);
    }

    // Step 2: Login with form credentials
    console.log('   🔐 Logging in...');
    const body = new URLSearchParams({ username, password });
    const loginRes = await fetch(`${this.apiUrl}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-XSRF-TOKEN': this.csrfToken,
        Cookie: this.cookies,
      },
      body,
      redirect: 'manual',
    });

    // Merge new session cookies
    const loginCookies = loginRes.headers.getSetCookie?.() || [];
    for (const cookie of loginCookies) {
      const [kv] = cookie.split(';');
      const [key] = kv.split('=');
      // Replace if exists, otherwise append
      const regex = new RegExp(`${key}=[^;]*`);
      if (this.cookies.match(regex)) {
        this.cookies = this.cookies.replace(regex, kv);
      } else {
        this.cookies += `; ${kv}`;
      }
      // Update CSRF if new token
      const csrfMatch = cookie.match(/XSRF-TOKEN=([^;]+)/);
      if (csrfMatch) this.csrfToken = decodeURIComponent(csrfMatch[1]);
    }

    if (loginRes.status !== 200) {
      const text = await loginRes.text().catch(() => '');
      throw new Error(`Login failed (${loginRes.status}): ${text}`);
    }

    console.log('   ✅ Authenticated successfully');
  }

  async uploadMedia(filePath, folder, title, altText) {
    const filename = basename(filePath);
    const ext = extname(filename).toLowerCase();
    const mimeMap = {
      '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg',
      '.png': 'image/png', '.webp': 'image/webp',
      '.gif': 'image/gif',
    };
    const contentType = mimeMap[ext] || 'image/jpeg';

    // Build multipart form data manually using Node.js native
    const fileData = readFileSync(filePath);
    const boundary = '----TrastImport' + Date.now();

    const parts = [];

    // File part
    parts.push(
      `--${boundary}\r\n`,
      `Content-Disposition: form-data; name="file"; filename="${filename}"\r\n`,
      `Content-Type: ${contentType}\r\n\r\n`,
    );
    const headerBuf = Buffer.from(parts.join(''));
    const tailBuf = Buffer.from(`\r\n--${boundary}--\r\n`);
    const body = Buffer.concat([headerBuf, fileData, tailBuf]);

    const url = folder
      ? `${this.apiUrl}/api/admin/media?folder=${encodeURIComponent(folder)}`
      : `${this.apiUrl}/api/admin/media`;

    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        'X-XSRF-TOKEN': this.csrfToken,
        Cookie: this.cookies,
      },
      body,
    });

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(`Upload failed (${res.status}): ${text}`);
    }

    const media = await res.json();

    // Update title and alt text if provided
    if (title || altText) {
      await fetch(`${this.apiUrl}/api/admin/media/${media.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': this.csrfToken,
          Cookie: this.cookies,
        },
        body: JSON.stringify({
          title: title || media.title,
          altText: altText || null,
          caption: null,
          description: null,
          folder: folder || 'General',
          width: media.width,
          height: media.height,
          durationSeconds: null,
        }),
      });
    }

    return media;
  }
}

async function uploadAll(downloadResults, opts) {
  console.log('\n📤 Phase 3: Uploading to TrastCMS...');

  if (opts.dryRun || opts.skipUpload) {
    console.log(`   ⏭️  Skipping upload (${opts.dryRun ? 'dry-run' : 'skip-upload'})`);
    return new Map();
  }

  if (!opts.user || !opts.password) {
    console.error('   ❌ --user and --password required to upload to TrastCMS');
    process.exit(1);
  }

  const client = new TrastCmsClient(opts.apiUrl);
  await client.login(opts.user, opts.password);

  /** @type {Map<string, string>} wpUrl → trastUrl */
  const urlMapping = new Map();

  // Load existing mappings if available
  const mappingPath = join(CACHE_DIR, 'url-mapping.json');
  if (existsSync(mappingPath)) {
    try {
      const existing = JSON.parse(readFileSync(mappingPath, 'utf8'));
      for (const [k, v] of Object.entries(existing)) urlMapping.set(k, v);
      console.log(`   📂 Loaded ${urlMapping.size} existing mappings`);
    } catch { /* ignore */ }
  }

  const uploadable = downloadResults.filter(r => r.localPath && !r.error && !r.dryRun);
  let uploaded = 0;

  for (const result of uploadable) {
    const wpUrl = result.item.originalUrl;

    // Skip if already mapped
    if (urlMapping.has(wpUrl)) {
      uploaded++;
      console.log(`   ⏭️  Already mapped [${uploaded}/${uploadable.length}] ${basename(result.localPath)}`);
      continue;
    }

    try {
      const media = await client.uploadMedia(
        result.localPath,
        opts.folder,
        result.item.title,
        result.item.altText
      );

      urlMapping.set(wpUrl, media.publicUrl);

      // Also map the scaled/rotated variants to the same local URL
      if (result.item.sourceUrl !== wpUrl) {
        urlMapping.set(result.item.sourceUrl, media.publicUrl);
      }

      uploaded++;
      console.log(`   ✅ [${uploaded}/${uploadable.length}] ${basename(result.localPath)} → ${media.publicUrl}`);
    } catch (error) {
      uploaded++;
      console.log(`   ❌ [${uploaded}/${uploadable.length}] ${basename(result.localPath)}: ${error.message}`);
    }

    // Save mapping after each upload (incremental safety)
    writeFileSync(mappingPath, JSON.stringify(Object.fromEntries(urlMapping), null, 2));
  }

  console.log(`   ✅ ${urlMapping.size} total mappings saved`);
  return urlMapping;
}

// ─── Phase 4: Replace URLs in JSON Files ────────────────────────────────────
function replaceUrlsInJsonFiles(urlMapping, opts) {
  console.log('\n🔄 Phase 4: Replacing URLs in content files...');

  if (opts.dryRun) {
    console.log('   🔍 [DRY-RUN] Would replace URLs in:');
    console.log('      - travel-demo.json');
    return;
  }

  if (!urlMapping.size) {
    console.log('   ⏭️  No URL mappings available, skipping replacement');
    return;
  }

  const files = [
    join(PROJECT_ROOT, 'plugins/trasttravel/src/main/resources/demo/travel-demo.json'),
  ];

  for (const filePath of files) {
    if (!existsSync(filePath)) {
      console.log(`   ⏭️  File not found: ${basename(filePath)}`);
      continue;
    }

    let content = readFileSync(filePath, 'utf8');
    let replacements = 0;

    for (const [wpUrl, trastUrl] of urlMapping) {
      // Count occurrences before replacing
      const regex = new RegExp(escapeRegex(wpUrl), 'g');
      const matches = content.match(regex);
      if (matches) {
        replacements += matches.length;
        content = content.replace(regex, trastUrl);
      }

      // Also try to match the -scaled variant
      const scaledUrl = wpUrl.replace(/(\.[a-z]+)$/i, '-scaled$1');
      if (scaledUrl !== wpUrl) {
        const scaledRegex = new RegExp(escapeRegex(scaledUrl), 'g');
        const scaledMatches = content.match(scaledRegex);
        if (scaledMatches) {
          replacements += scaledMatches.length;
          content = content.replace(scaledRegex, trastUrl);
        }
      }

      // And the -rotated variant
      const rotatedUrl = wpUrl.replace(/(\.[a-z]+)$/i, '-rotated$1');
      if (rotatedUrl !== wpUrl) {
        const rotatedRegex = new RegExp(escapeRegex(rotatedUrl), 'g');
        const rotatedMatches = content.match(rotatedRegex);
        if (rotatedMatches) {
          replacements += rotatedMatches.length;
          content = content.replace(rotatedRegex, trastUrl);
        }
      }
    }

    if (replacements > 0) {
      writeFileSync(filePath, content, 'utf8');
      console.log(`   ✅ ${basename(filePath)}: ${replacements} URLs replaced`);
    } else {
      console.log(`   ℹ️  ${basename(filePath)}: no matching URLs found`);
    }
  }
}

function escapeRegex(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// ─── Phase 5: Report ────────────────────────────────────────────────────────
function generateReport(wpItems, downloadResults, urlMapping, opts) {
  console.log('\n' + '═'.repeat(60));
  console.log('📊 IMPORT REPORT');
  console.log('═'.repeat(60));
  console.log(`   WordPress site:     ${opts.wpUrl}`);
  console.log(`   TrastCMS server:    ${opts.apiUrl}`);
  console.log(`   Media folder:       ${opts.folder}`);
  console.log(`   Mode:               ${opts.dryRun ? '🔍 DRY-RUN' : '🚀 LIVE'}`);
  console.log('─'.repeat(60));
  console.log(`   WP media found:     ${wpItems.length}`);

  const downloaded = downloadResults.filter(r => !r.error && !r.dryRun);
  const cached = downloadResults.filter(r => r.cached);
  const failed = downloadResults.filter(r => r.error);

  console.log(`   Downloaded:         ${downloaded.length - cached.length}`);
  console.log(`   From cache:         ${cached.length}`);
  console.log(`   Failed:             ${failed.length}`);
  console.log(`   URL mappings:       ${urlMapping.size}`);
  console.log('═'.repeat(60));

  if (failed.length) {
    console.log('\n⚠️  Failed downloads:');
    for (const f of failed) {
      console.log(`   - ${f.item?.originalUrl || 'unknown'}: ${f.error}`);
    }
  }

  // Save full report
  const reportPath = join(CACHE_DIR, 'import-report.json');
  const report = {
    timestamp: new Date().toISOString(),
    wpUrl: opts.wpUrl,
    apiUrl: opts.apiUrl,
    folder: opts.folder,
    dryRun: opts.dryRun,
    totalWpMedia: wpItems.length,
    downloaded: downloaded.length,
    cached: cached.length,
    failed: failed.length,
    mappings: Object.fromEntries(urlMapping),
    failedItems: failed.map(f => ({
      url: f.item?.originalUrl,
      error: f.error
    })),
  };
  writeFileSync(reportPath, JSON.stringify(report, null, 2));
  console.log(`\n📄 Full report saved: ${reportPath}`);
}

// ─── Main ───────────────────────────────────────────────────────────────────
async function main() {
  console.log('╔══════════════════════════════════════════════════════════╗');
  console.log('║     TrastCMS — WordPress Media Import Tool              ║');
  console.log('╚══════════════════════════════════════════════════════════╝');

  const opts = parseArgs();

  // Phase 1: Discover
  const wpItems = await fetchWpMedia(opts.wpUrl);

  if (!wpItems.length) {
    console.log('No media items found. Exiting.');
    return;
  }

  // Phase 2: Download
  const downloadResults = await downloadAll(wpItems, opts);

  // Phase 3: Upload to TrastCMS
  const urlMapping = await uploadAll(downloadResults, opts);

  // Phase 4: Replace URLs (only with --replace flag)
  if (opts.replace) {
    replaceUrlsInJsonFiles(urlMapping, opts);
  } else if (urlMapping.size) {
    console.log('\n💡 Tip: Run again with --replace to update JSON content files.');
  }

  // Phase 5: Report
  generateReport(wpItems, downloadResults, urlMapping, opts);
}

main().catch(error => {
  console.error('\n💥 Fatal error:', error.message);
  process.exit(1);
});
