import { NavLink, Route, Routes } from 'react-router-dom';
import NoticeList from './components/NoticeList';
import NoticeDetail from './components/NoticeDetail';
import NoticeCreate from './components/NoticeCreate';
import NoticeEdit from './components/NoticeEdit';
import BookSearch from './components/BookSearch';
import MapSearch from './components/MapSearch';
import './App.css';

export default function App() {
  return (
    <div className="container">
      <header className="page-header">
        <div>
          <h1>게시판</h1>
          <div className="subtitle">함께 나누는 공지와 이야기</div>
        </div>
        <nav className="top-nav">
          <NavLink to="/" end className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            공지사항
          </NavLink>
          <NavLink to="/books" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            책 검색
          </NavLink>
          <NavLink to="/map" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            지도
          </NavLink>
        </nav>
      </header>
      <Routes>
        <Route path="/" element={<NoticeList />} />
        <Route path="/notices/new" element={<NoticeCreate />} />
        <Route path="/notices/:id" element={<NoticeDetail />} />
        <Route path="/notices/:id/edit" element={<NoticeEdit />} />
        <Route path="/books" element={<BookSearch />} />
        <Route path="/map" element={<MapSearch />} />
      </Routes>
    </div>
  );
}