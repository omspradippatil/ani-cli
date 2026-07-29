import urllib.request
import json
import ssl

ssl._create_default_https_context = ssl._create_unverified_context
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0',
    'Referer': 'https://mkissa.to',
    'Origin': 'https://mkissa.to'
}

# GraphQL search query
gql_query = """query( $search: SearchInput $limit: Int $page: Int $translationType: VaildTranslationTypeEnumType $countryOrigin: VaildCountryOriginEnumType ) { shows( search: $search limit: $limit page: $page translationType: $translationType countryOrigin: $countryOrigin ) { edges { _id name englishName thumbnail availableEpisodes __typename } }}"""

data = {
    "variables": {
        "search": {
            "allowAdult": False,
            "allowUnknown": False,
            "query": "naruto"
        },
        "limit": 10,
        "page": 1,
        "translationType": "sub",
        "countryOrigin": "ALL"
    },
    "query": gql_query
}

req_data = json.dumps(data).encode('utf-8')
url = "https://api.mkissa.net/api"

print(f"Sending POST to {url}...")
try:
    req = urllib.request.Request(url, data=req_data, headers={**headers, 'Content-Type': 'application/json'})
    res = urllib.request.urlopen(req, timeout=10)
    resp_text = res.read().decode('utf-8')
    print(f"AllAnime Search Response Status: {res.status}")
    print(f"Response length: {len(resp_text)}")
    json_data = json.loads(resp_text)
    shows = json_data.get('data', {}).get('shows', {}).get('edges', [])
    print(f"Found {len(shows)} shows:")
    for s in shows[:3]:
        print(" -", s.get('name'), "| ID:", s.get('_id'))
except Exception as e:
    print("AllAnime Search ERROR:", e)
