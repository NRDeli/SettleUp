
import { useEffect, useState } from 'react';
import { MEMBERSHIP_BASE, EXPENSE_BASE, SETTLEMENT_BASE } from '../config';

type Check = { name: string; url: string; ok: boolean; error?: string };

export default function ApiStatusCard() {
  const [checks, setChecks] = useState<Check[]>([]);

  useEffect(() => {
    const endpoints = [
      { name: 'membership', url: `${MEMBERSHIP_BASE}/actuator/health` },
      { name: 'expense', url: `${EXPENSE_BASE}/actuator/health` },
      { name: 'settlement', url: `${SETTLEMENT_BASE}/actuator/health` },
      { name: 'membership-swagger', url: `${MEMBERSHIP_BASE}/v3/api-docs` },
    ];
    (async () => {
      const out: Check[] = [];
      for (const e of endpoints) {
        try {
          const r = await fetch(e.url);
          out.push({ name: e.name, url: e.url, ok: r.ok, error: r.ok ? undefined : `${r.status}` });
        } catch (err: any) {
          out.push({ name: e.name, url: e.url, ok: false, error: String(err) });
        }
      }
      setChecks(out);
    })();
  }, []);

  return (
    <div className="card">
      <h2 className="subtitle">Service status</h2>
      <ul className="space-y-2">
        {checks.map(c => (
          <li key={c.name} className="flex items-center justify-between">
            <span className="font-mono">{c.name}</span>
            <span className={c.ok ? 'text-green-700' : 'text-red-700'}>
              {c.ok ? 'OK' : `ERROR: ${c.error}`}
            </span>
          </li>
        ))}
      </ul>
      <p className="text-xs mt-3 text-gray-600">If these fail due to CORS in dev, use Vite dev server (npm run dev) so the proxy is applied.</p>
    </div>
  );
}
