
import { Routes, Route } from 'react-router-dom'
import Nav from './components/Nav'
import Home from './pages/Home'
import Groups from './pages/Groups'
import Members from './pages/Members'
import Categories from './pages/Categories'
import Expenses from './pages/Expenses'
import Settlement from './pages/Settlement'
import Status from './pages/Status'

export default function App() {
  return (
    <div className="max-w-6xl mx-auto p-4">
      <Nav />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/groups" element={<Groups />} />
        <Route path="/members" element={<Members />} />
        <Route path="/categories" element={<Categories />} />
        <Route path="/expenses" element={<Expenses />} />
        <Route path="/settlement" element={<Settlement />} />
        <Route path="/status" element={<Status />} />
      </Routes>
    </div>
  )
}
