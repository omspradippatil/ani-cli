import urllib.request
import re
import ssl

ssl._create_default_https_context = ssl._create_unverified_context
headers = {'User-Agent': 'Mozilla/5.0'}

url = 'https://anikoto.cz/search?keyword=naruto'
req = urllib.request.Request(url, headers=headers)
html = urllib.request.urlopen(req).read().decode('utf-8')

# Search for the list of anime
items = re.findall(r'<a href="([^"]+/watch/[^"]+)"[^>]*title="([^"]+)"', html)
print(items[:10])
