import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';

const API = 'http://localhost:8080/api/auth';

export default function AuthBar() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState(null);
  const [mode, setMode] = useState(null); // 'login' | 'signup' | null
  const [form, setForm] = useState({ username: '', password: '', nickname: '', email: '', notificationOptIn: false });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const wrapRef = useRef(null);

  useEffect(() => {
    const url = new URL(window.location.href);
    const err = url.searchParams.get('auth_error');
    const desc = url.searchParams.get('auth_error_description');
    if (err) {
      setAuthError(desc || err);
      url.searchParams.delete('auth_error');
      url.searchParams.delete('auth_error_description');
      window.history.replaceState({}, '', url.toString());
    }
    const refresh = () => {
      fetch(`${API}/me`, { credentials: 'include' })
        .then((r) => (r.ok ? r.json() : null))
        .then(setUser)
        .catch(() => setUser(null))
        .finally(() => setLoading(false));
    };
    refresh();
    const onAuth = () => refresh();
    window.addEventListener('auth-changed', onAuth);
    return () => window.removeEventListener('auth-changed', onAuth);
  }, []);

  useEffect(() => {
    if (!mode) return;
    const onClick = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) {
        setMode(null);
        setFormError(null);
      }
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, [mode]);

  const onKakaoLogin = () => {
    const returnTo = `${window.location.origin}/`;
    window.location.href = `${API}/kakao/login?returnTo=${encodeURIComponent(returnTo)}`;
  };

  const onLogout = async () => {
    await fetch(`${API}/logout`, { method: 'POST', credentials: 'include' });
    setUser(null);
    window.dispatchEvent(new Event('auth-changed'));
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);
    try {
      const url = mode === 'signup' ? `${API}/signup` : `${API}/login`;
      const body = mode === 'signup'
        ? {
            username: form.username,
            password: form.password,
            nickname: form.nickname || null,
            email: form.email?.trim() || null,
            notificationOptIn: !!form.notificationOptIn,
          }
        : { username: form.username, password: form.password };
      const r = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!r.ok) {
        const data = await r.json().catch(() => ({}));
        throw new Error(data.message || (mode === 'signup' ? '회원가입 실패' : '로그인 실패'));
      }
      const data = await r.json();
      setUser(data);
      setMode(null);
      setForm({ username: '', password: '', nickname: '', email: '', notificationOptIn: false });
      window.dispatchEvent(new Event('auth-changed'));
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const displayName = (u) => u?.username || u?.nickname || '사용자';

  if (loading) return <div className="auth-skeleton" aria-hidden="true" />;

  if (user) {
    return (
      <div className="auth-user">
        {user.profileImage ? (
          <img src={user.profileImage} alt="" className="auth-avatar" />
        ) : (
          <div className="auth-avatar auth-avatar-placeholder">👤</div>
        )}
        <div className="auth-user-info">
          <div className="auth-nickname">{displayName(user)}</div>
          {user.email && <div className="auth-email">{user.email}</div>}
        </div>
        <Link to="/me/edit" className="auth-edit">수정</Link>
        <button type="button" className="auth-logout" onClick={onLogout}>로그아웃</button>
      </div>
    );
  }

  return (
    <div className="auth-actions" ref={wrapRef}>
      {authError && (
        <span className="auth-error-pill" title={authError}>
          로그인 실패: {authError}
        </span>
      )}
      <button
        type="button"
        className={`auth-tab-btn ${mode === 'login' ? 'active' : ''}`}
        onClick={() => { setMode(mode === 'login' ? null : 'login'); setFormError(null); }}
      >
        로그인
      </button>
      <button
        type="button"
        className={`auth-tab-btn ${mode === 'signup' ? 'active' : ''}`}
        onClick={() => { setMode(mode === 'signup' ? null : 'signup'); setFormError(null); }}
      >
        회원가입
      </button>
      {!mode && (
        <button type="button" className="kakao-login-btn" onClick={onKakaoLogin}>
          <span className="kakao-login-icon" aria-hidden="true">
            <svg viewBox="0 0 24 22" width="18" height="16" fill="currentColor">
              <path d="M12 0C5.373 0 0 4.262 0 9.52c0 3.348 2.182 6.292 5.477 7.96l-1.142 4.18c-.103.376.32.677.652.466l4.91-3.244c.69.07 1.39.108 2.103.108 6.627 0 12-4.262 12-9.52C24 4.262 18.627 0 12 0z" />
            </svg>
          </span>
          카카오
        </button>
      )}

      {mode && (
        <form className="auth-popover" onSubmit={onSubmit}>
          <div className="auth-popover-title">
            {mode === 'signup' ? '회원가입' : '로그인'}
          </div>
          <div className="auth-popover-subtitle">
            {mode === 'signup'
              ? '아이디와 비밀번호로 새 계정을 만들어요'
              : '아이디 비밀번호로 로그인하세요'}
          </div>
          <input
            type="text"
            placeholder="아이디 (영문/숫자/_ 4~20자)"
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            autoFocus
            required
          />
          <input
            type="password"
            placeholder="비밀번호 (6자 이상)"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
          {mode === 'signup' && (
            <>
              <input
                type="text"
                placeholder="닉네임 (선택)"
                value={form.nickname}
                onChange={(e) => setForm({ ...form, nickname: e.target.value })}
              />
              <input
                type="email"
                placeholder="이메일 (필수)"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                maxLength={200}
                required
              />
              <label className="auth-popover-checkbox">
                <input
                  type="checkbox"
                  checked={form.notificationOptIn}
                  onChange={(e) => setForm({ ...form, notificationOptIn: e.target.checked })}
                />
                <span>공지 메일 수신 동의 (선택)</span>
              </label>
            </>
          )}
          {mode === 'login' && (
            <div className="auth-popover-extra">
              <Link to="/forgot" onClick={() => setMode(null)}>
                비밀번호를 잊으셨나요?
              </Link>
            </div>
          )}
          {formError && <div className="auth-popover-error">{formError}</div>}
          <button type="submit" className="auth-popover-submit" disabled={submitting}>
            {submitting ? '처리 중…' : (mode === 'signup' ? '가입하고 시작하기' : '로그인')}
          </button>
          <div className="auth-popover-divider">또는</div>
          <button
            type="button"
            className="kakao-login-btn auth-popover-kakao"
            onClick={onKakaoLogin}
          >
            <span className="kakao-login-icon" aria-hidden="true">
              <svg viewBox="0 0 24 22" width="18" height="16" fill="currentColor">
                <path d="M12 0C5.373 0 0 4.262 0 9.52c0 3.348 2.182 6.292 5.477 7.96l-1.142 4.18c-.103.376.32.677.652.466l4.91-3.244c.69.07 1.39.108 2.103.108 6.627 0 12-4.262 12-9.52C24 4.262 18.627 0 12 0z" />
              </svg>
            </span>
            카카오로 {mode === 'signup' ? '가입' : '로그인'}
          </button>
          <div className="auth-popover-switch">
            {mode === 'signup' ? '이미 계정이 있나요?' : '아직 계정이 없나요?'}{' '}
            <button
              type="button"
              onClick={() => {
                setMode(mode === 'signup' ? 'login' : 'signup');
                setFormError(null);
              }}
            >
              {mode === 'signup' ? '로그인' : '회원가입'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}