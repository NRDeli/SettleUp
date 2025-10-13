
import { useEffect, useState } from 'react';
import { MEMBERSHIP_BASE } from '../config';
import { api } from '../lib/api';

type Member = { id: number; email: string; role: string };
type Group = { id: number; name: string; baseCurrency: string };

export default function Members() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [email, setEmail] = useState('alice@example.com');
  const [role, setRole] = useState('MEMBER');

  async function loadGroups() {
    const g = await api<Group[]>(`${MEMBERSHIP_BASE}/groups`);
    setGroups(g);
    if (g.length && selectedGroup == null) setSelectedGroup(g[0].id);
  }
  async function loadMembers(groupId: number) {
    const m = await api<Member[]>(`${MEMBERSHIP_BASE}/groups/${groupId}/members`);
    setMembers(m);
  }
  useEffect(() => { loadGroups(); }, []);
  useEffect(() => { if (selectedGroup!=null) loadMembers(selectedGroup); }, [selectedGroup]);

  async function addMember() {
    if (selectedGroup==null) return;
    await api<Member>(`${MEMBERSHIP_BASE}/groups/${selectedGroup}/members`, {
      method: 'POST',
      body: JSON.stringify({ email, role })
    });
    setEmail(''); setRole('MEMBER'); await loadMembers(selectedGroup);
  }

  async function removeMember(memberId: number) {
    if (selectedGroup==null) return;
    await fetch(`${MEMBERSHIP_BASE}/groups/${selectedGroup}/members/${memberId}`, { method: 'DELETE' });
    await loadMembers(selectedGroup);
  }

  return (
    <div className="card">
      <h2 className="subtitle">Members</h2>
      <div className="mb-3">
        <label className="label">Group</label>
        <select className="input" value={selectedGroup ?? ''} onChange={e => setSelectedGroup(Number(e.target.value))}>
          {groups.map(g => <option key={g.id} value={g.id}>{g.name} (#{g.id})</option>)}
        </select>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <div className="backdrop">
          <h3 className="font-semibold mb-2">Add member</h3>
          <div className="mb-2">
            <label className="label">Email</label>
            <input className="input" value={email} onChange={e=>setEmail(e.target.value)} />
          </div>
          <div className="mb-2">
            <label className="label">Role</label>
            <input className="input" value={role} onChange={e=>setRole(e.target.value)} />
          </div>
          <button className="btn bg-white" onClick={addMember}>Add</button>
        </div>

        <div className="backdrop">
          <h3 className="font-semibold mb-2">Existing members</h3>
          <ul className="space-y-2">
            {members.map(m => (
              <li key={m.id} className="flex items-center justify-between">
                <div>
                  <div className="font-medium">{m.email}</div>
                  <div className="text-xs text-gray-600">#{m.id} · {m.role}</div>
                </div>
                <button className="btn bg-white" onClick={() => removeMember(m.id)}>Remove</button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
