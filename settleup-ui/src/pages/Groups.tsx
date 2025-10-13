
import { useEffect, useState } from 'react';
import { MEMBERSHIP_BASE } from '../config';
import { api } from '../lib/api';

type Group = { id: number; name: string; baseCurrency: string };

export default function Groups() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [name, setName] = useState('My Group');
  const [currency, setCurrency] = useState('USD');
  const [updateId, setUpdateId] = useState<number | null>(null);

  async function refresh() {
    const g = await api<Group[]>(`${MEMBERSHIP_BASE}/groups`);
    setGroups(g);
  }
  useEffect(() => { refresh(); }, []);

  async function createGroup() {
    await api<Group>(`${MEMBERSHIP_BASE}/groups`, {
      method: 'POST', body: JSON.stringify({ name, baseCurrency: currency })
    });
    setName(''); setCurrency('USD'); await refresh();
  }

  async function updateGroup() {
    if (!updateId) return;
    await api<Group>(`${MEMBERSHIP_BASE}/groups/${updateId}`, {
      method: 'PUT', body: JSON.stringify({ name, baseCurrency: currency })
    });
    setUpdateId(null); setName(''); setCurrency('USD'); await refresh();
  }

  async function removeGroup(id: number) {
    await fetch(`${MEMBERSHIP_BASE}/groups/${id}`, { method: 'DELETE' });
    await refresh();
  }

  return (
    <div className="card">
      <h2 className="subtitle">Groups</h2>

      <div className="grid md:grid-cols-2 gap-6">
        <div className="backdrop">
          <h3 className="font-semibold mb-2">{updateId ? 'Update group' : 'Create group'}</h3>
          <div className="mb-2">
            <label className="label">Name</label>
            <input className="input" value={name} onChange={e=>setName(e.target.value)} />
          </div>
          <div className="mb-2">
            <label className="label">Base Currency</label>
            <input className="input" value={currency} onChange={e=>setCurrency(e.target.value)} />
          </div>
          <div className="flex gap-2">
            {!updateId ? (
              <button className="btn bg-white" onClick={createGroup}>Create</button>
            ) : (
              <button className="btn bg-white" onClick={updateGroup}>Save</button>
            )}
          </div>
        </div>

        <div className="backdrop">
          <h3 className="font-semibold mb-2">Existing groups</h3>
          <ul className="space-y-2">
            {groups.map(g => (
              <li key={g.id} className="flex items-center justify-between">
                <div>
                  <div className="font-medium">{g.name}</div>
                  <div className="text-xs text-gray-600">#{g.id} · {g.baseCurrency}</div>
                </div>
                <div className="flex gap-2">
                  <button className="btn bg-white" onClick={() => { setUpdateId(g.id); setName(g.name); setCurrency(g.baseCurrency); }}>Edit</button>
                  <button className="btn bg-white" onClick={() => removeGroup(g.id)}>Delete</button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
