import urllib.request
import json
import ssl

ssl._create_default_https_context = ssl._create_unverified_context
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0',
    'Referer': 'https://mkissa.to',
    'Origin': 'https://mkissa.to'
}

show_id = "2oXgpDPd3xKWdgnoz" # ROAD OF NARUTO

# 1. Details
details_gql = """query ($showId: String!) { show( _id: $showId ) { _id name englishName thumbnail availableEpisodesDetail }}"""
data_det = {
    "variables": {"showId": show_id},
    "query": details_gql
}

req_det = urllib.request.Request("https://api.mkissa.net/api", data=json.dumps(data_det).encode('utf-8'), headers={**headers, 'Content-Type': 'application/json'})
res_det = urllib.request.urlopen(req_det, timeout=10)
det_json = json.loads(res_det.read().decode('utf-8'))
show = det_json.get('data', {}).get('show', {})
sub_eps = show.get('availableEpisodesDetail', {}).get('sub', [])
print(f"Show title: {show.get('name')}")
print(f"Available sub episodes ({len(sub_eps)}): {sub_eps[:5]}")

if sub_eps:
    ep_num = sub_eps[0]
    print(f"\n2. Fetching episode watch info for Ep {ep_num}...")
    
    # Check GET api endpoint for watch info
    var_str = json.dumps({"showId": show_id, "translationType": "sub", "episodeString": ep_num})
    ext_str = json.dumps({"persistedQuery": {"version": 1, "sha256Hash": "f4662f4b7510b26795dd53ef824a0bf1740fbbc5d1273fab18222ac831bca8d0"}})
    
    import urllib.parse
    watch_url = f"https://api.mkissa.net/api?variables={urllib.parse.quote(var_str)}&extensions={urllib.parse.quote(ext_str)}"
    
    print(f"GET {watch_url}")
    try:
        req_w = urllib.request.Request(watch_url, headers=headers)
        res_w = urllib.request.urlopen(req_w, timeout=10)
        w_json = json.loads(res_w.read().decode('utf-8'))
        print("Watch response data:", json.dumps(w_json, indent=2)[:500])
    except Exception as e:
        print("Watch GET error:", e)
