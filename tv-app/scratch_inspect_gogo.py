import urllib.request
import re
import ssl

ssl._create_default_https_context = ssl._create_unverified_context
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0'}

print("--- Inspecting Category Page ---")
url_cat = "https://gogoanimes.fi/category/naruto-shippuden"
req = urllib.request.Request(url_cat, headers=headers)
html_cat = urllib.request.urlopen(req).read().decode('utf-8')

# Search for episode elements, scripts, or ajax parameters
print("Episode regex searches:")
print("ep_end:", re.findall(r'ep_end\s*=\s*["\']?(\d+)["\']?', html_cat))
print("ep_start:", re.findall(r'ep_start\s*=\s*["\']?(\d+)["\']?', html_cat))
print("movie_id:", re.findall(r'id=["\']movie_id["\']\s*value=["\']?([^"\'>]+)["\']?', html_cat))
print("default_ep:", re.findall(r'id=["\']default_ep["\']\s*value=["\']?([^"\'>]+)["\']?', html_cat))
print("alias_anime:", re.findall(r'id=["\']alias_anime["\']\s*value=["\']?([^"\'>]+)["\']?', html_cat))

print("\n--- Inspecting Episode Page ---")
url_ep = "https://gogoanimes.fi/naruto-shippuden-episode-1"
req_ep = urllib.request.Request(url_ep, headers=headers)
html_ep = urllib.request.urlopen(req_ep).read().decode('utf-8')

iframes = re.findall(r'<iframe[^>]+src=["\']([^"\']+)["\']', html_ep)
print("Iframes found on episode page:")
for f in iframes:
    print("  -", f)

# Also check server links list (e.g. <li class="anime"> or servers-online)
servers = re.findall(r'<li class="[^"]*">\s*<a[^>]+data-video=["\']([^"\']+)["\'][^>]*>([^<]+)</a>', html_ep)
print("\nServers found:")
for s in servers:
    print("  -", s)

# Let's inspect embed page
if iframes:
    embed_url = iframes[0]
    if embed_url.startswith('//'): embed_url = 'https:' + embed_url
    print(f"\n--- Inspecting Embed Page: {embed_url} ---")
    try:
        req_emb = urllib.request.Request(embed_url, headers={'User-Agent': headers['User-Agent'], 'Referer': url_ep})
        html_emb = urllib.request.urlopen(req_emb).read().decode('utf-8')
        print(f"Embed length: {len(html_emb)}")
        print("Embed scripts / video sources:")
        sources = re.findall(r'sources:\s*(\[[^\]]+\])', html_emb)
        print("sources:", sources)
        file_matches = re.findall(r'file:\s*["\']([^"\']+)["\']', html_emb)
        print("file matches:", file_matches)
        jw_matches = re.findall(r'https?://[^\s"\']+\.m3u8[^\s"\']*', html_emb)
        print("m3u8 matches:", jw_matches)
    except Exception as e:
        print("Embed fetch error:", e)
