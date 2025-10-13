
import { useEffect, useState } from 'react';
import { MEMBERSHIP_BASE, SETTLEMENT_BASE } from '../config';
import { api } from '../lib/api';

type Group = { id: number; name: string; baseCurrency: string };
type TransferDto = { fromMemberId: number; toMemberId: number; amount: number };
type SettlementPlan = { transfers: TransferDto[] };

export default function Settlement() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [plan, setPlan] = useState<SettlementPlan | null>(null);

  async function loadGroups() {
    const g = await api<Group[]>(`${MEMBERSHIP_BASE}/groups`);
    setGroups(g); if (g.length && selectedGroup==null) setSelectedGroup(g[0].id);
  }
  useEffect(() => { loadGroups(); }, []);

  async function compute() {
    if (selectedGroup==null) return;
    const g = groups.find(x => x.id===selectedGroup);
    const res = await api<SettlementPlan>(`${SETTLEMENT_BASE}/settlements/compute`, {
      method: 'POST',
      body: JSON.stringify({ groupId: selectedGroup, baseCurrency: g?.baseCurrency ?? 'USD' })
    });
    setPlan(res);
  }

  return (
    <div className="card">
      <h2 className="subtitle">Settlement</h2>
      <div className="mb-3">
        <label className="label">Group</label>
        <select className="input" value={selectedGroup ?? ''} onChange={e => setSelectedGroup(Number(e.target.value))}>
          {groups.map(g => <option key={g.id} value={g.id}>{g.name} (#{g.id})</option>)}
        </select>
      </div>
      <button className="btn bg-white mb-4" onClick={compute}>Compute plan</button>

      {plan && (
        <div className="backdrop">
          <h3 className="font-semibold mb-2">Transfers</h3>
          <ul className="space-y-2">
            {plan.transfers.map((t, i) => (
              <li key={i} className="flex items-center justify-between">
                <span>Member #{t.fromMemberId} → Member #{t.toMemberId}</span>
                <span className="font-medium">{t.amount}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
