import { useEffect, useState } from 'react';

const ME = 'http://localhost:8080/api/auth/me';

export default function RequireAuth({ children, title, hint }) {
  const [state, setState] = useState('loading');

  useEffect(() => {
    let cancelled = false;
    const check = () => {
      setState('loading');
      fetch(ME, { credentials: 'include' })
        .then((r) => {
          if (cancelled) return;
          setState(r.ok ? 'authed' : 'guest');
        })
        .catch(() => {
          if (!cancelled) setState('guest');
        });
    };
    check();
    window.addEventListener('auth-changed', check);
    return () => {
      cancelled = true;
      window.removeEventListener('auth-changed', check);
    };
  }, []);

  if (state === 'loading') {
    return (
      <div className="card">
        <div className="book-empty">
          <div className="spinner" />
          <p>확인 중…</p>
        </div>
      </div>
    );
  }

  if (state === 'guest') {
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
