import urllib.request
import re

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0'}
url = "https://cdn.mkissa.net/all/mk/_app/immutable/chunks/Dp_XixEf.js"
req = urllib.request.Request(url, headers=headers)
code = urllib.request.urlopen(req).read().decode('utf-8')

# Extract function gl() array
gl_match = re.search(r'function gl\(\)\{const e=(\[[^\]]+\]);return gl', code)
if gl_match:
    import json
    gl_array = json.loads(gl_match.group(1))
    print(f"gl array length: {len(gl_array)}")
    
    def pc(e):
        return gl_array[e - 220]
    
    def mr(e):
        return pc(e - 12)
    
    def Rn(r, n):
        return pc(n - 458)
        
    print("ad element 0:", mr(318) + mr(406))
    print("ad element 1:", Rn(392, 465) + mr(357))
    print("ad element 2:", Rn(381, 437) + Rn(373, 335))
    print("ad element 3:", mr(316) + mr(412))
