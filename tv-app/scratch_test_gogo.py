import urllib.request
import re
import ssl

ssl._create_default_https_context = ssl._create_unverified_context

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0'}

domains = ['gogoanime.by', 'gogoanime3.co', 'anitaku.pe', 'gogoanime.gg', 'anitaku.to', 'gogoanime.la', 'gogoanime.cl', 'anitaku.so', 'gogoanimes.fi']
for d in domains:
    try:
        url = f'https://{d}/search.html?keyword=naruto'
        req = urllib.request.Request(url, headers=headers)
        res = urllib.request.urlopen(req, timeout=5)
        html = res.read().decode('utf-8', errors='ignore')
        print(f'{d}: status {res.status}, length {len(html)}')
        matches = re.findall(r'href=["\']/category/([^"\']+)["\']', html)
        print(f'  Matches: {matches[:3]}')
    except Exception as e:
        print(f'{d} ERROR: {e}')
