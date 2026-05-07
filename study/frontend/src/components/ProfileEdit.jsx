import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const API = 'http://localhost:8080/api/auth';

export default function ProfileEdit() {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({
    nickname: '',
    email: '',
    currentPassword: '',
    newPassword: '',
    newPasswordConfirm: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    fetch(`${API}/me`, { credentials: 'include' })
      .then((r) => (r.ok ? r.json() : null))
      .then((u) => {
        if (!u) {
          navigate('/');
          return;
        }
        setUser(u);
        setForm((f) => ({
          ...f,
          nickname: u.nickname || '',
          email: u.email || '',
        }));
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  const isKakaoOnly = user?.kakaoId && !user?.username;

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (form.newPassword || form.newPasswordConfirm || form.currentPassword) {
      if (form.newPassword !== form.newPasswordConfirm) {
        setError('새 비밀번호 확인이 일치하지 않습니다.');
        return;
      }
      if (form.newPassword.length < 6) {
        setError('새 비밀번호는 6자 이상이어야 합니다.');
        return;
      }
    }

    setSubmitting(true);
    const body = {
      nickname: form.nickname?.trim() || null,
      email: form.email?.trim() || null,
    };
    if (form.newPassword) {
      body.currentPassword = form.currentPassword || null;
      body.newPassword = form.newPassword;
    }

    try {
      const r = await fetch(`${API}/me`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!r.ok) {
        const data = await r.json().catch(() => ({}));
        throw new Error(data.message || '수정에 실패했습니다.');
      }
      const updated = await r.json();
      setUser(updated);
      setForm((f) => ({
        ...f,
        currentPassword: '',
        newPassword: '',
        newPasswordConfirm: '',
      }));
      const passwordChanged = !!body.newPassword;
      setSuccess(
        passwordChanged
          ? '비밀번호가 변경되었습니다. 안내 메일을 보냈어요. 잠시 후 홈으로 이동합니다.'
          : '회원정보가 수정되었습니다. 잠시 후 홈으로 이동합니다.'
      );
      window.dispatchEvent(new Event('auth-changed'));
      setTimeout(() => navigate('/'), 1200);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="card">
        <div className="book-empty">
          <div className="spinner" />
          <p>불러오는 중…</p>
        </div>
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="card">
      <div className="toolbar">
        <h2>👤 회원정보 수정</h2>
        <span className="muted">닉네임 · 이메일 · 비밀번호 변경</span>
      </div>

      <form className="profile-edit-form" onSubmit={onSubmit}>
        <div className="profile-edit-row">
          <label>아이디</label>
          <input type="text" value={user.username || '(카카오 로그인)'} disabled />
        </div>

        <div className="profile-edit-row">
          <label>닉네임</label>
          <input
            type="text"
            value={form.nickname}
            onChange={(e) => setForm({ ...form, nickname: e.target.value })}
            placeholder="닉네임 (50자 이하)"
            maxLength={50}
          />
        </div>

        <div className="profile-edit-row">
          <label>이메일</label>
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            placeholder="example@example.com"
            maxLength={200}
          />
        </div>

        {!isKakaoOnly && (
          <fieldset className="profile-edit-pwd">
            <legend>비밀번호 변경 (선택)</legend>
            <div className="profile-edit-row">
              <label>현재 비밀번호</label>
              <input
                type="password"
                value={form.currentPassword}
                onChange={(e) => setForm({ ...form, currentPassword: e.target.value })}
                placeholder="현재 비밀번호"
                autoComplete="current-password"
              />
            </div>
            <div className="profile-edit-row">
              <label>새 비밀번호</label>
              <input
                type="password"
                value={form.newPassword}
                onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
                placeholder="6자 이상"
                autoComplete="new-password"
              />
            </div>
            <div className="profile-edit-row">
              <label>새 비밀번호 확인</label>
              <input
                type="password"
                value={form.newPasswordConfirm}
                onChange={(e) => setForm({ ...form, newPasswordConfirm: e.target.value })}
                placeholder="다시 입력"
                autoComplete="new-password"
              />
            </div>
          </fieldset>
        )}

        {isKakaoOnly && (
          <div className="profile-edit-info">
            카카오 로그인 계정은 비밀번호를 변경할 수 없습니다.
          </div>
        )}

        {error && <div className="error">⚠️ {error}</div>}
        {success && <div className="profile-edit-success">✅ {success}</div>}

        <div className="profile-edit-actions">
          <button type="button" className="ghost" onClick={() => navigate(-1)}>
            취소
          </button>
          <button type="submit" className="primary" disabled={submitting}>
            {submitting ? '저장 중…' : '저장'}
          </button>
        </div>
      </form>
    </div>
  );
}