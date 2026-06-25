import { useAuth } from '../context/AuthContext';

export default function RequireAuth({ children, title, hint }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="card">
        <div className="book-empty">
          <div className="spinner" />
          <p>확인 중…</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="card">
        <div className="toolbar">
          <h2>{title}</h2>
          <span className="muted">로그인 후 이용 가능</span>
        </div>
        <div className="user-locked">
          <div className="user-locked-icon">🔒</div>
          <div className="user-locked-title">로그인이 필요해요</div>
          <div className="user-locked-desc">
            {hint || (
              <>우측 상단의 <b>회원가입</b> 또는 <b>로그인</b> 버튼을 눌러 계정을 만들어 주세요.</>
            )}
          </div>
        </div>
      </div>
    );
  }

  return children;
}
