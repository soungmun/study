import { useEffect, useState } from 'react';
import { NavLink, Route, Routes } from 'react-router-dom';
import NoticeList from './components/NoticeList';
import NoticeDetail from './components/NoticeDetail';
import NoticeCreate from './components/NoticeCreate';
import NoticeEdit from './components/NoticeEdit';
import BookSearch from './components/BookSearch';
import MapSearch from './components/MapSearch';
import StockTopList from './components/StockTopList';
import UserList from './components/UserList';
import AuthBar from './components/AuthBar';
import RequireAuth from './components/RequireAuth';
import ProfileEdit from './components/ProfileEdit';
import PasswordForgot from './components/PasswordForgot';
import PasswordReset from './components/PasswordReset';
import AdminBroadcast from './components/AdminBroadcast';
import KakaoPay from './components/KakaoPay';
import KakaoPayHistory from './components/KakaoPayHistory';
import { KakaoPaySuccess, KakaoPayFail } from './components/KakaoPayResult';
import './App.css';

function MaintenanceBanner() {
  const [enabled, setEnabled] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const fetchStatus = () => {
      fetch('http://localhost:8080/api/admin/maintenance/status', { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : null))
        .then((data) => { if (!cancelled && data) setEnabled(!!data.enabled); })
        .catch(() => {});
    };
    fetchStatus();
    const id = setInterval(fetchStatus, 30000);
    const onAuth = () => fetchStatus();
    window.addEventListener('auth-changed', onAuth);
    return () => {
      cancelled = true;
      clearInterval(id);
      window.removeEventListener('auth-changed', onAuth);
    };
  }, []);

  if (!enabled) return null;
  return (
    <div style={{
      padding: '12px 16px',
      background: 'linear-gradient(135deg,#f59e0b,#ef4444)',
      color: '#fff',
      fontWeight: 700,
      textAlign: 'center',
      borderRadius: 10,
      margin: '0 0 12px 0',
      boxShadow: '0 4px 12px rgba(239,68,68,0.25)',
    }}>
      🛠️ 현재 서버 점검 중입니다. 관리자 외에는 서비스 이용이 제한됩니다.
    </div>
  );
}

export default function App() {
  return (
    <div className="container">
      <MaintenanceBanner />
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
          <NavLink to="/stocks" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            주식
          </NavLink>
          <NavLink to="/pay" end className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            결제
          </NavLink>
          <NavLink to="/pay/history" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            결제내역
          </NavLink>
          <NavLink to="/admin/broadcast" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            관리자
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
        <Route path="/stocks" element={<RequireAuth title="📈 주식"><StockTopList /></RequireAuth>} />
        <Route path="/users" element={<UserList />} />
        <Route path="/me/edit" element={<RequireAuth title="👤 회원정보 수정"><ProfileEdit /></RequireAuth>} />
        <Route path="/forgot" element={<PasswordForgot />} />
        <Route path="/reset" element={<PasswordReset />} />
        <Route path="/admin/broadcast" element={<RequireAuth title="🛠️ 서버 점검 모드"><AdminBroadcast /></RequireAuth>} />
        <Route path="/pay" element={<RequireAuth title="💳 카카오페이 결제"><KakaoPay /></RequireAuth>} />
        <Route path="/pay/history" element={<RequireAuth title="🧾 결제 내역"><KakaoPayHistory /></RequireAuth>} />
        <Route path="/pay/success" element={<RequireAuth title="💳 결제 결과"><KakaoPaySuccess /></RequireAuth>} />
        <Route path="/pay/fail" element={<RequireAuth title="💳 결제 결과"><KakaoPayFail /></RequireAuth>} />
      </Routes>
    </div>
  );
}