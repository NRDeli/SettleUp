
import { useEffect, useState } from 'react';
import { MEMBERSHIP_BASE } from '../config';
import { api } from '../lib/api';

type Category = { id: number; name: string };
type Group = { id: number; name: string };

export default function Categories() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [cats, setCats] = useState<Category[]>([]);
  const [name, setName] = useState('General');

  async function loadGroups() {
    const g = await api<Group[]>(`${MEMBERSHIP_BASE}/groups`);
    setGroups(g);
    if (g.length && selectedGroup == null) setSelectedGroup(g[0].id);
  }
  async function loadCats(groupId: number) {
    const c = await api<Category[]>(`${MEMBERSHIP_BASE}/groups/${groupId}/categories`);
    setCats(c);
  }
  useEffect(() => { loadGroups(); }, []);
  useEffect(() => { if (selectedGroup!=null) loadCats(selectedGroup); }, [selectedGroup]);

  async function addCat() {
    if (selectedGroup==null) return;
    await api<Category>(`${MEMBERSHIP_BASE}/groups/${selectedGroup}/categories`, {
      method: 'POST', body: JSON.stringify({ name })
    });
    setName(''); await loadCats(selectedGroup);
  }
  async function removeCat(id: number) {
    if (selectedGroup==null) return;
    await fetch(`${MEMBERSHIP_BASE}/groups/${selectedGroup}/categories/${id}`, { method: 'DELETE' });
    await loadCats(selectedGroup);
  }

  return (
    <div className="card">
      <h2 className="subtitle">Categories</h2>
      <div className="mb-3">
        <label className="label">Group</label>
        <select className="input" value={selectedGroup ?? ''} onChange={e => setSelectedGroup(Number(e.target.value))}>
          {groups.map(g => <option key={g.id} value={g.id}>{g.name} (#{g.id})</option>)}
        </select>
      </div>
      <div className="grid md:grid-cols-2 gap-6">
        <div className="backdrop">
          <h3 className="font-semibold mb-2">Add category</h3>
          <div className="mb-2">
            <label className="label">Name</label>
            <input className="input" value={name} onChange={e=>setName(e.target.value)} />
          </div>
          <button className="btn bg-white" onClick={addCat}>Add</button>
        </div>

        <div className="backdrop">
          <h3 className="font-semibold mb-2">Existing categories</h3>
          <ul className="space-y-2">
            {cats.map(c => (
              <li key={c.id} className="flex items-center justify-between">
                <div className="font-medium">{c.name}</div>
                <button className="btn bg-white" onClick={() => removeCat(c.id)}>Remove</button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
