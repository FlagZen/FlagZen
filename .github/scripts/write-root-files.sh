#!/usr/bin/env bash
# Writes sitemap.xml and robots.txt at the root of the mike-managed gh-pages tree.
#
# mike builds one sitemap per version (/1.1/sitemap.xml, /dev/sitemap.xml), but
# nothing at the root points crawlers to them. This script adds a root sitemap
# index that references the sitemap of the version aliased "latest", plus a
# robots.txt announcing it.
#
# The index references the real version directory (e.g. /1.1/), not /latest/:
# mike renders each version with its own site_url, so the URLs inside
# /latest/sitemap.xml are /1.1/... and a sitemap may only list URLs under its
# own directory.
#
# Usage: write-root-files.sh <gh-pages-dir> <site-url>
set -euo pipefail

pages_dir="$1"
site_url="${2%/}"

version=$(jq -r '.[] | select(.aliases | index("latest")) | .version' "$pages_dir/versions.json")
if [[ -z "$version" ]]; then
  echo "No version carries the 'latest' alias; leaving root files unchanged." >&2
  exit 0
fi

cat > "$pages_dir/sitemap.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <sitemap>
    <loc>$site_url/$version/sitemap.xml</loc>
  </sitemap>
</sitemapindex>
EOF

cat > "$pages_dir/robots.txt" <<EOF
User-agent: *
Allow: /

Sitemap: $site_url/sitemap.xml
EOF

echo "Root sitemap now points to $site_url/$version/sitemap.xml"
