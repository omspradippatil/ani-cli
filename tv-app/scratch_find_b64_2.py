import urllib.request
import re

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0'}

url = "https://cdn.mkissa.net/all/mk/_app/immutable/chunks/Dp_XixEf.js"
req = urllib.request.Request(url, headers=headers)
code = urllib.request.urlopen(req).read().decode('utf-8')

# Search for the string array definition gl() or the strings inside gl()
matches = re.findall(r'function gl\(\)\{const e=(\[[^\]]+\]);', code)
if matches:
    print("Found gl array strings:")
    print(matches[0][:500])
else:
    print("No gl array found directly by regex.")

# Find all base64-like strings in code:
b64s = re.findall(r'"([A-Za-z0-9+/=]{10,40})"', code)
print(f"Found {len(b64s)} b64 candidate strings:")
for b in set(b64s):
    if len(b) in [12, 16, 24, 32, 44]:
        print("  -", b)
