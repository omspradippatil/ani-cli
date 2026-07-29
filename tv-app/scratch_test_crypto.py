import urllib.request
import re
import json
import base64
import hmac
import hashlib

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0'}

# 1. Fetch homepage
req1 = urllib.request.Request("https://mkissa.to", headers=headers)
html1 = urllib.request.urlopen(req1).read().decode('utf-8')

app_url = re.search(r'(https://[^"]+/entry/app\.[^"]+\.js)', html1).group(1)
print(f"app_url: {app_url}")

req2 = urllib.request.Request(app_url, headers=headers)
app_js = urllib.request.urlopen(req2).read().decode('utf-8')

chunks = re.findall(r'"(\.\./chunks/[^"]+\.js)"', app_js)
print(f"Found {len(chunks)} chunks")

js = ""
for chunk in chunks[:5]:
    chunk_url = "https://cdn.mkissa.net/all/mk/_app/immutable/" + chunk.replace("../", "")
    chunk_js = urllib.request.urlopen(urllib.request.Request(chunk_url, headers=headers)).read().decode('utf-8')
    js += chunk_js + "\n"

build_id = re.findall(r'!==\s*"string"\s*\?\s*"([0-9]+)"', js.replace(" ", ""))[-1]
lane = re.findall(r'=\s*"(k[0-9]+)"', js)[-1]
print(f"build_id: {build_id}, lane: {lane}")

# mask generation
b64s = ["10d13/213hI=", "XdTA6FaAQSo=", "KRCUT9lZh1o=", "Aj3ASCIDMnw="]
mask_hex = ""
mask_bytes = bytearray()

for block, b64 in enumerate(b64s):
    decoded = base64.b64decode(b64)
    for byte_idx, b in enumerate(decoded):
        index = block * 8 + byte_idx
        c = build_id[index % len(build_id)]
        build_mask_byte = ord(c) ^ (((index * 17) + 31) & 255)
        tweak = ((block * 41) + (byte_idx * 7)) & 255
        val = b ^ build_mask_byte ^ tweak
        mask_bytes.append(val)
        mask_hex += f"{val:02x}"

print(f"Mask hex ({len(mask_bytes)} bytes): {mask_hex}")

# Epoch
import time
now_ms = int(time.time() * 1000)
epoch = int(now_ms // 259200000)
if now_ms - (epoch * 259200000) < 86400000 and epoch > 0:
    epoch -= 1

print(f"Epoch: {epoch}")

# HMAC Step 1: h = HMAC_SHA256(key=mask_bytes, msg="aa-boot:t:"+build_id)
h1 = hmac.new(mask_bytes, f"aa-boot:t:{build_id}".encode('utf-8'), hashlib.sha256).digest()

# HMAC Step 2: payload = buildId:keyGroup:hostname:epoch:lane
payload = f"{build_id}:mkissa:mkissa.to:{epoch}:{lane}"
y = hmac.new(h1, payload.encode('utf-8'), hashlib.sha256).digest()
aa_boot = y.hex()

print(f"aa_boot token: {aa_boot}")

# Test bootstrap endpoint
boot_url = f"https://api.mkissa.net/client-crypto/v1/bootstrap?buildId={build_id}&k={lane}"
req_boot = urllib.request.Request(boot_url, headers={
    **headers,
    'x-build-id': build_id,
    'x-aa-boot': aa_boot,
    'Referer': 'https://mkissa.to',
    'Origin': 'https://mkissa.to'
})

try:
    res_boot = urllib.request.urlopen(req_boot)
    boot_text = res_boot.read().decode('utf-8')
    print(f"BOOTSTRAP SUCCESS! Status {res_boot.status}")
    print("Bootstrap response:", boot_text)
except Exception as e:
    print("BOOTSTRAP FAILED:", e)
    if hasattr(e, 'read'):
        print("Error body:", e.read().decode('utf-8'))
