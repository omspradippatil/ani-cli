import urllib.request
import re

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0'}

url = "https://cdn.mkissa.net/all/mk/_app/immutable/chunks/Dp_XixEf.js"
req = urllib.request.Request(url, headers=headers)
content = urllib.request.urlopen(req).read().decode('utf-8')

# Find base64 array in JS:
b64_arrays = re.findall(r'\[\s*"[A-Za-z0-9+/=]{8,16}"\s*,\s*"[A-Za-z0-9+/=]{8,16}"\s*,\s*"[A-Za-z0-9+/=]{8,16}"\s*,\s*"[A-Za-z0-9+/=]{8,16}"\s*\]', content)
print("Base64 arrays found:", b64_arrays)

# Also search for any variable assigned a list of 4 strings
str_arrays = re.findall(r'\["[^"]{8,20}","[^"]{8,20}","[^"]{8,20}","[^"]{8,20}"\]', content)
print("String arrays found:", str_arrays)
