import urllib.request
import json
import ssl

ssl._create_default_https_context = ssl._create_unverified_context
headers = {
    'User-Agent': 'Mozilla/5.0',
    'Origin': 'https://mkissa.to',
    'Referer': 'https://mkissa.to'
}
api = 'https://api.mkissa.net/api'

# Search
query_vars = json.dumps({"search":{"allowAdult":False,"allowUnknown":False,"query":"gintama"},"limit":40,"page":1,"translationType":"sub","countryOrigin":"ALL"})
query_ext = json.dumps({"persistedQuery":{"version":1,"sha256Hash":"9d7439e76313b52bd3b13af8b91dbf3df4b971aef42f06b6eb2b5ddf908e0cb2"}})
search_url = f'{api}?variables={urllib.parse.quote(query_vars)}&extensions={urllib.parse.quote(query_ext)}'
print(f"1. Searching {api}...")
req = urllib.request.Request(search_url, headers=headers)
resp = urllib.request.urlopen(req).read().decode('utf-8')
data = json.loads(resp)
shows = data.get('data', {}).get('shows', {}).get('edges', [])
print(f"Found {len(shows)} shows")
if shows:
    print(f"First show: {shows[0].get('name')} (ID: {shows[0].get('_id')})")

