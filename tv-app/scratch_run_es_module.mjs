import fs from 'fs';
import https from 'https';
import crypto from 'node:crypto';

const url = 'https://cdn.mkissa.net/all/mk/_app/immutable/chunks/Dp_XixEf.js';

https.get(url, (res) => {
    let data = '';
    res.on('data', c => data += c);
    res.on('end', async () => {
        data = data.replace(/import\s*\{[^}]*\}\s*from\s*["'][^"']+["'];?/g, 'const {} = {};');
        data = 'globalThis.gt = globalThis.wt = globalThis.Pt = globalThis.Vt = globalThis.Kt = globalThis.Gt = globalThis.Xt = globalThis.Jt = () => ({});\n' + data + '\nexport { PS as testPS, qS as testEpoch, sr as testBuildId };\n';
        fs.writeFileSync('scratch_temp_chunk.mjs', data);
        console.log('Saved scratch_temp_chunk.mjs');
        
        try {
            globalThis.window = globalThis;
            globalThis.location = { href: 'https://mkissa.to/', hostname: 'mkissa.to' };
            Object.defineProperty(globalThis, 'navigator', {
                value: { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0' },
                writable: true,
                configurable: true
            });
            Object.defineProperty(globalThis, 'crypto', {
                value: crypto.webcrypto,
                writable: true,
                configurable: true
            });

            console.log("Before importing mod...");
            const mod = await import(`./scratch_temp_chunk.mjs?v=${Date.now()}`);
            console.log("Module loaded!");
            
            const buildId = mod.testBuildId ? mod.testBuildId() : "75";
            const epoch = mod.testEpoch ? mod.testEpoch() : 6887;
            console.log(`buildId: ${buildId}, epoch: ${epoch}`);
            
            console.log("Calling testPS...");
            const token = await mod.testPS({ buildId: buildId, epoch: epoch, keyGroup: "mkissa", refererHost: "mkissa.to", contentLane: "k2" });
            console.log("Generated token:", token);

            // Now test bootstrap request
            const bootUrl = `https://api.mkissa.net/client-crypto/v1/bootstrap?buildId=${buildId}&k=k2`;
            const req = https.request(bootUrl, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0',
                    'x-build-id': buildId,
                    'x-aa-boot': token,
                    'Referer': 'https://mkissa.to',
                    'Origin': 'https://mkissa.to'
                }
            }, (resBoot) => {
                let bootBody = '';
                resBoot.on('data', chunk => bootBody += chunk);
                resBoot.on('end', () => {
                    console.log(`Bootstrap Status: ${resBoot.statusCode}`);
                    console.log(`Bootstrap Response: ${bootBody}`);
                });
            });
            req.end();
        } catch (e) {
            console.log("Import error:", e);
        }
    });
});
