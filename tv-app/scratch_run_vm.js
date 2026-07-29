const https = require('https');
const vm = require('vm');

const url = 'https://cdn.mkissa.net/all/mk/_app/immutable/chunks/Dp_XixEf.js';

https.get(url, (res) => {
    let code = '';
    res.on('data', chunk => code += chunk);
    res.on('end', () => {
        // Strip export statements
        code = code.replace(/export\s*\{[^}]*\};?/g, '');
        code = code.replace(/export\s+default\s+[^;]+;/g, '');
        code = code.replace(/import\s*\([\s\S]*?\)/g, 'Promise.resolve({})');
        code = code.replace(/import\s[\s\S]*?from\s*['"][^'"]+['"];?/g, '');
        code = code.replace(/import\s*['"][^'"]+['"];?/g, '');
        const sandbox = {
            window: {},
            globalThis: {},
            console: console,
            TextEncoder: TextEncoder,
            TextDecoder: TextDecoder,
            crypto: require('crypto').webcrypto,
            fetch: () => {},
            location: { href: 'https://mkissa.to/', hostname: 'mkissa.to' }
        };
        sandbox.globalThis = sandbox;
        sandbox.window = sandbox;

        const context = vm.createContext(sandbox);
        try {
            vm.runInContext(code, context);
            console.log("Code executed in VM!");
            console.log("ad array:", context.ad);
            console.log("buildId (wf/sr):", context.sr || context.wf);
        } catch (e) {
            console.log("VM Error:", e.message);
            // Let's inspect context properties
            console.log("Context keys:", Object.keys(sandbox));
        }
    });
});
