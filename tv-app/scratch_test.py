import urllib.request
import re
import base64

req = urllib.request.Request("https://mkissa.to", headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
html = urllib.request.urlopen(req).read().decode('utf-8')
epoch = re.search(r'"epoch":([0-9]+)', html).group(1)
partB = re.search(r'"partB":"([^"]*)"', html).group(1)

print(f"epoch: {epoch}")
print(f"partB: {partB}")

partB_bytes = base64.b64decode(partB)
print(f"partB length: {len(partB_bytes)}")
