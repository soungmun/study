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

  // 이메일 인증 상태
  const [emailVer, setEmailVer] = useState({
    sent: false,            // 인증번호 발송됨?
    verifiedEmail: null,    // 인증 완료된 이메일 (form.email 과 비교용)
    code: '',               // 사용자가 입력한 코드
    sending: false,         // 발송 중
    verifying: false,       // 검증 중
    cooldownSec: 0,         // 재발송 쿨다운
    msg: null,              // "인증번호를 발송했어요" 같은 안내
    err: null,              // 인증 관련 에러
  });

  // 쿨다운 타이머
  useEffect(() => {
    if (emailVer.cooldownSec <= 0) return;
    const t = setTimeout(() => setEmailVer((p) => ({ ...p, cooldownSec: p.cooldownSec - 1 })), 1000);
    return () => clearTimeout(t);
  }, [emailVer.cooldownSec]);

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

  const requestEmailCode = async () => {
    const email = form.email?.trim();
    if (!email) {
      setEmailVer((p) => ({ ...p, err: '이메일을 입력해 주세요.', msg: null }));
      return;
    }
    setEmailVer((p) => ({ ...p, sending: true, err: null, msg: null }));
    try {
      const r = await fetch(`${API}/email/send-code`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(data.message || '발송 실패');
      setEmailVer((p) => ({
        ...p,
        sending: false,
        sent: true,
        msg: `인증번호를 발송했어요. 메일함을 확인해 주세요. (${Math.round((data.expiresInSeconds ?? 300) / 60)}분 안에 입력)`,
        cooldownSec: 60,
        verifiedEmail: null,
      }));
    } catch (err) {
      setEmailVer((p) => ({ ...p, sending: false, err: err.message }));
    }
  };

  const verifyEmailCode = async () => {
    const email = form.email?.trim();
    const code = emailVer.code?.trim();
    if (!email || !code) {
      setEmailVer((p) => ({ ...p, err: '이메일과 인증번호를 모두 입력해 주세요.' }));
      return;
    }
    setEmailVer((p) => ({ ...p, verifying: true, err: null }));
    try {
      const r = await fetch(`${API}/email/verify-code`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, code }),
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(data.message || '인증 실패');
      setEmailVer((p) => ({ ...p, verifying: false, verifiedEmail: email, msg: '✅ 인증되었습니다.', err: null }));
    } catch (err) {
      setEmailVer((p) => ({ ...p, verifying: false, err: err.message }));
    }
  };

  const resetEmailVer = () => setEmailVer({
    sent: false, verifiedEmail: null, code: '', sending: false, verifying: false, cooldownSec: 0, msg: null, err: null,
  });

  const onLogout = async () => {
    await fetch(`${API}/logout`, { method: 'POST', credentials: 'include' });
    setUser(null);
    window.dispatchEvent(new Event('auth-changed'));
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setFormError(null);
    if (mode === 'signup') {
      const email = form.email?.trim();
      if (!email || emailVer.verifiedEmail !== email) {
        setFormError('이메일 인증을 먼저 완료해 주세요.');
        return;
      }
    }
    setSubmitting(true);
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
      resetEmailVer();
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
              <div style={{ display: 'flex', gap: 6 }}>
                <input
                  type="email"
                  placeholder="이메일 (필수)"
                  value={form.email}
                  onChange={(e) => {
                    setForm({ ...form, email: e.target.value });
                    if (emailVer.verifiedEmail || emailVer.sent) resetEmailVer();
                  }}
                  maxLength={200}
                  required
                  disabled={emailVer.verifiedEmail === form.email?.trim() && !!form.email}
                  style={{ flex: 1 }}
                />
                <button
                  type="button"
                  onClick={requestEmailCode}
                  disabled={emailVer.sending || emailVer.cooldownSec > 0 || !form.email?.trim() || emailVer.verifiedEmail === form.email?.trim()}
                  style={{
                    padding: '0 12px', fontSize: 12, fontWeight: 600,
                    borderRadius: 8, border: '1px solid #cbd5e1',
                    background: emailVer.verifiedEmail === form.email?.trim() ? '#22c55e' : '#fff',
                    color: emailVer.verifiedEmail === form.email?.trim() ? '#fff' : '#475569',
                    cursor: 'pointer', whiteSpace: 'nowrap',
                  }}
                >
                  {emailVer.verifiedEmail === form.email?.trim() && form.email
                    ? '✓ 인증완료'
                    : emailVer.sending
                      ? '발송 중…'
                      : emailVer.cooldownSec > 0
                        ? `${emailVer.cooldownSec}s`
                        : (emailVer.sent ? '재발송' : '인증번호 받기')}
                </button>
              </div>

              {emailVer.sent && emailVer.verifiedEmail !== form.email?.trim() && (
                <div style={{ display: 'flex', gap: 6 }}>
                  <input
                    type="text"
                    placeholder="6자리 인증번호"
                    value={emailVer.code}
                    onChange={(e) => setEmailVer((p) => ({ ...p, code: e.target.value.replace(/\D/g, '').slice(0, 6) }))}
                    maxLength={6}
                    inputMode="numeric"
                    style={{ flex: 1, letterSpacing: 4, fontFamily: 'monospace', textAlign: 'center' }}
                  />
                  <button
                    type="button"
                    onClick={verifyEmailCode}
                    disabled={emailVer.verifying || emailVer.code.length !== 6}
                    style={{
                      padding: '0 14px', fontSize: 12, fontWeight: 600,
                      borderRadius: 8, border: '1px solid #6366f1',
                      background: '#6366f1', color: '#fff', cursor: 'pointer', whiteSpace: 'nowrap',
                    }}
                  >
                    {emailVer.verifying ? '확인…' : '확인'}
                  </button>
                </div>
              )}

              {emailVer.msg && <div style={{ fontSize: 12, color: emailVer.verifiedEmail ? '#16a34a' : '#475569' }}>{emailVer.msg}</div>}
              {emailVer.err && <div style={{ fontSize: 12, color: '#ef4444' }}>{emailVer.err}</div>}

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