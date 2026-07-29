import urllib.request
import re
import ssl
import json
import base64
from Crypto.Cipher import AES

ssl._create_default_https_context = ssl._create_unverified_context
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0'}

domain = 'gogoanimes.fi'
search_url = f'https://{domain}/search.html?keyword=naruto'
print(f"1. Searching {search_url}...")
req = urllib.request.Request(search_url, headers=headers)
html = urllib.request.urlopen(req).read().decode('utf-8')

# parse search results
items = re.findall(r'<li>\s*<div class="img">\s*<a href="/category/([^"]+)" title="([^"]+)">\s*<img src="([^"]+)"', html)
print(f"Found {len(items)} items:")
for slug, title, img in items[:3]:
    print(f"  - {title} (ID/slug: {slug})")

if not items:
    # try alternate regex
    items = re.findall(r'href="/category/([^"]+)"', html)
    print(f"Fallback href regex found: {items[:3]}")

if items:
    target_slug = items[0][0] if isinstance(items[0], tuple) else items[0]
    detail_url = f'https://{domain}/category/{target_slug}'
    print(f"\n2. Fetching details from {detail_url}...")
    req_det = urllib.request.Request(detail_url, headers=headers)
    html_det = urllib.request.urlopen(req_det).read().decode('utf-8')
    
    ep_end = re.findall(r'ep_end=["\'](\d+)["\']', html_det)
    print(f"Episode max end: {ep_end}")
    
    ep_url = f'https://{domain}/{target_slug}-episode-1'
    print(f"\n3. Fetching episode 1 from {ep_url}...")
    req_ep = urllib.request.Request(ep_url, headers=headers)
    html_ep = urllib.request.urlopen(req_ep).read().decode('utf-8')
    
    # Extract iframe / embed URL
    embed_match = re.search(r'<iframe[^>]+src=["\']([^"\']+)["\']', html_ep)
    if embed_match:
        embed_url = embed_match.group(1)
        if embed_url.startswith('//'): embed_url = 'https:' + embed_url
        print(f"Found embed URL: {embed_url}")
    else:
        print("No iframe found in episode page!")
