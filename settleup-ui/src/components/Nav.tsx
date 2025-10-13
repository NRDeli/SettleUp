
import { NavLink } from 'react-router-dom'

export default function Nav() {
  const link = (to: string, label: string) => (
    <NavLink
      to={to}
      className={({ isActive }) => `btn ${isActive ? 'bg-black text-white' : 'bg-white/80'}`}
    >
      {label}
    </NavLink>
  );
  return (
    <div className="flex flex-wrap gap-3 mb-6">
      {link('/', 'Home')}
      {link('/groups', 'Groups')}
      {link('/members', 'Members')}
      {link('/categories', 'Categories')}
      {link('/expenses', 'Expenses')}
      {link('/settlement', 'Settlement')}
      {link('/status', 'Status')}
    </div>
  )
}
