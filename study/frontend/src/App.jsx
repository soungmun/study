import { NavLink, Route, Routes } from 'react-router-dom';
import NoticeList from './components/NoticeList';
import NoticeDetail from './components/NoticeDetail';
import NoticeCreate from './components/NoticeCreate';
import NoticeEdit from './components/NoticeEdit';
import BookSearch from './components/BookSearch';
import MapSearch from './components/MapSearch';
import UserList from './components/UserList';
import AuthBar from './components/AuthBar';
import RequireAuth from './components/RequireAuth';
import ProfileEdit from './components/ProfileEdit';
import PasswordForgot from './components/PasswordForgot';
import PasswordReset from './components/PasswordReset';
import AdminBroadcast from './components/AdminBroadcast';
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
          <AuthBar />
        </nav>
      </header>
      <Routes>
        <Route path="/" element={<RequireAuth title="📋 공지사항"><NoticeList /></RequireAuth>} />
        <Route path="/notices/new" element={<RequireAuth title="✏️ 새 공지 작성"><NoticeCreate /></RequireAuth>} />
        <Route path="/notices/:id" element={<RequireAuth title="📋 공지사항"><NoticeDetail /></RequireAuth>} />
        <Route path="/notices/:id/edit" element={<RequireAuth title="✏️ 공지 수정"><NoticeEdit /></RequireAuth>} />
        <Route path="/books" element={<RequireAuth title="📚 책 검색"><BookSearch /></RequireAuth>} />
        <Route path="/map" element={<RequireAuth title="📍 장소 검색"><MapSearch /></RequireAuth>} />
        <Route path="/users" element={<UserList />} />
        <Route path="/me/edit" element={<RequireAuth title="👤 회원정보 수정"><ProfileEdit /></RequireAuth>} />
        <Route path="/forgot" element={<PasswordForgot />} />
        <Route path="/reset" element={<PasswordReset />} />
        <Route path="/admin/broadcast" element={<RequireAuth title="📣 공지 메일 발송"><AdminBroadcast /></RequireAuth>} />
      </Routes>
    </div>
  );
}