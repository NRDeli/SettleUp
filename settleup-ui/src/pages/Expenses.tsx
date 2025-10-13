
import { useEffect, useState } from 'react';
import { EXPENSE_BASE, MEMBERSHIP_BASE } from '../config';
import { api } from '../lib/api';

type Group = { id: number; name: string };
type Member = { id: number; email: string };
type Expense = {
  id: number;
  groupId: number;
  payerMemberId: number;
  currency: string;
  totalAmount: number;
  splits: { memberId: number; shareAmount: number }[];
};

export default function Expenses() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const [payer, setPayer] = useState<number | null>(null);
  const [currency, setCurrency] = useState('USD');
  const [total, setTotal] = useState<number>(0);
  const [splits, setSplits] = useState<{ memberId: number; shareAmount: number }[]>([]);
  const [groupExpenses, setGroupExpenses] = useState<Expense[]>([]);

  async function loadGroups() {
    const g = await api<Group[]>(`${MEMBERSHIP_BASE}/groups`);
    setGroups(g);
    if (g.length && selectedGroup==null) setSelectedGroup(g[0].id);
  }
  async function loadMembers(groupId: number) {
    const m = await api<Member[]>(`${MEMBERSHIP_BASE}/groups/${groupId}/members`);
    setMembers(m);
    if (m.length) setPayer(m[0].id);
    setSplits(m.map(mm => ({ memberId: mm.id, shareAmount: 0 })));
  }
  async function loadGroupExpenses(groupId: number) {
    const list = await api<Expense[]>(`${EXPENSE_BASE}/groups/${groupId}/expenses`);
    setGroupExpenses(list);
  }

  useEffect(() => { loadGroups(); }, []);
  useEffect(() => { if (selectedGroup!=null) { loadMembers(selectedGroup); loadGroupExpenses(selectedGroup); } }, [selectedGroup]);

  function updateSplit(idx: number, amt: number) {
    const next = splits.slice();
    next[idx] = { ...next[idx], shareAmount: amt };
    setSplits(next);
  }

  async function createExpense() {
    if (selectedGroup==null || payer==null) return;
    await api<Expense>(`${EXPENSE_BASE}/expenses`, {
      method: 'POST',
      body: JSON.stringify({
        groupId: selectedGroup,
        payerMemberId: payer,
        currency,
        totalAmount: total,
        splits
      })
    });
    setTotal(0);
    await loadGroupExpenses(selectedGroup);
  }

  async function deleteExpense(id: number) {
    await fetch(`${EXPENSE_BASE}/expenses/${id}`, { method: 'DELETE' });
    if (selectedGroup!=null) await loadGroupExpenses(selectedGroup);
  }

  return (
    <div className="card">
      <h2 className="subtitle">Expenses</h2>
      <div className="mb-3">
        <label className="label">Group</label>
        <select className="input" value={selectedGroup ?? ''} onChange={e => setSelectedGroup(Number(e.target.value))}>
          {groups.map(g => <option key={g.id} value={g.id}>{g.name} (#{g.id})</option>)}
        </select>
      </div>

      <div className="grid lg:grid-cols-2 gap-6">
        <div className="backdrop">
          <h3 className="font-semibold mb-2">Create expense</h3>
          <div className="mb-2">
            <label className="label">Payer</label>
            <select className="input" value={payer ?? ''} onChange={e => setPayer(Number(e.target.value))}>
              {members.map(m => <option key={m.id} value={m.id}>{m.email} (#{m.id})</option>)}
            </select>
          </div>
          <div className="mb-2">
            <label className="label">Currency</label>
            <input className="input" value={currency} onChange={e=>setCurrency(e.target.value)} />
          </div>
          <div className="mb-2">
            <label className="label">Total Amount</label>
            <input className="input" type="number" value={total} onChange={e=>setTotal(Number(e.target.value))} />
          </div>
          <div className="mb-2">
            <label className="label">Splits</label>
            <div className="space-y-2">
              {splits.map((s, idx) => (
                <div key={s.memberId} className="flex items-center gap-2">
                  <span className="w-56 text-sm">{members.find(m=>m.id===s.memberId)?.email}</span>
                  <input className="input" type="number" value={s.shareAmount} onChange={e=>updateSplit(idx, Number(e.target.value))}/>
                </div>
              ))}
            </div>
          </div>
          <button className="btn bg-white" onClick={createExpense}>Save expense</button>
        </div>

        <div className="backdrop">
          <h3 className="font-semibold mb-2">Group expenses</h3>
          <ul className="space-y-2">
            {groupExpenses.map(ex => (
              <li key={ex.id} className="flex items-center justify-between">
                <div>
                  <div className="font-medium">#{ex.id} · {ex.currency} {ex.totalAmount}</div>
                  <div className="text-xs text-gray-600">Payer #{ex.payerMemberId} · Splits: {ex.splits.length}</div>
                </div>
                <button className="btn bg-white" onClick={() => deleteExpense(ex.id)}>Delete</button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
