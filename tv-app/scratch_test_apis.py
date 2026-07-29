import urllib.request
import json
import ssl

ssl._create_default_https_context = ssl._create_unverified_context
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}

apis = [
    "https://api.consumet.org/anime/gogoanime/naruto",
    "https://consumet-api-clone.vercel.app/anime/gogoanime/naruto",
    "https://api-consumet-org.vercel.app/anime/gogoanime/naruto",
    "https://api.hi-anime.site/anime/search?q=naruto",
    "https://aniwatch-api-v1.vercel.app/anime/search?q=naruto"
]

for api in apis:
    try:
        req = urllib.request.Request(api, headers=headers)
        res = urllib.request.urlopen(req, timeout=5)
        data = json.loads(res.read().decode('utf-8'))
        print(f"API SUCCESS {api}:")
        print(f"  Result count/keys: {list(data.keys()) if isinstance(data, dict) else len(data)}")
    except Exception as e:
        print(f"API FAIL {api}: {e}")
