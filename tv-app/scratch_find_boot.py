import urllib.request
import re

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0'}

req1 = urllib.request.Request("https://mkissa.to", headers=headers)
html1 = urllib.request.urlopen(req1).read().decode('utf-8')

app_url = re.search(r'(https://[^"]+/entry/app\.[^"]+\.js)', html1).group(1)
print(f"app_url: {app_url}")

req2 = urllib.request.Request(app_url, headers=headers)
app_js = urllib.request.urlopen(req2).read().decode('utf-8')

chunks = re.findall(r'"(\.\./chunks/[^"]+\.js)"', app_js)
print(f"Found {len(chunks)} chunks")

for i, chunk in enumerate(chunks):
    chunk_url = "https://cdn.mkissa.net/all/mk/_app/immutable/" + chunk.replace("../", "")
    chunk_js = urllib.request.urlopen(urllib.request.Request(chunk_url, headers=headers)).read().decode('utf-8')
    if "aa-boot" in chunk_js or "x-aa-boot" in chunk_js or "bootstrap" in chunk_js:
        print(f"Chunk {i} ({chunk}): MATCH!")
        matches = re.findall(r'.{0,100}(?:aa-boot|x-aa-boot|bootstrap).{0,100}', chunk_js)
        for m in matches[:10]:
            print("  ->", m)
