import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../utils/api';

export default function ProfileEdit() {
  const navigate = useNavigate();
  const { user, loading, setUser } = useAuth();
  const [form, setForm] = useState({
    nickname: '',
    email: '',
    currentPassword: '',
    newPassword: '',
    newPasswordConfirm: '',
    notificationOptIn: false,
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // 닉네임 중복 체크 상태
  const [nicknameCheck, setNicknameCheck] = useState({ status: 'idle', message: null });
  // 'idle' | 'checking' | 'available' | 'taken' | 'error'

  useEffect(() => {
    if (!loading && !user) {
      navigate('/');
    } else if (user) {
      setForm((f) => ({
        ...f,
        nickname: user.nickname || '',
        email: user.email || '',
        notificationOptIn: !!user.notificationOptIn,
      }));
    }
  }, [user, loading, navigate]);

  // 닉네임 변경 감지 및 안전한 디바운스 API 호출 처리 (React 실무 표준 패턴)
  useEffect(() => {
    const trimmed = form.nickname.trim();
    const originalNickname = (user?.nickname || '').trim();

    // 입력값이 비어있거나, 기존 자신의 닉네임과 같다면 체크 생략
    if (!trimmed || trimmed === originalNickname) {
      setNicknameCheck({ status: 'idle', message: null });
      return;
    }

    if (trimmed.length > 50) {
      setNicknameCheck({ status: 'error', message: '닉네임은 50자 이하여야 합니다.' });
      return;
    }

    setNicknameCheck({ status: 'checking', message: null });

    let isCurrent = true;
    const controller = new AbortController();

    const timer = setTimeout(async () => {
      try {
        await api.get(`/auth/check-nickname?nickname=${encodeURIComponent(trimmed)}`, {
          signal: controller.signal
        });
        if (isCurrent) {
          setNicknameCheck({ status: 'available', message: '사용 가능한 닉네임입니다.' });
        }
      } catch (err) {
        // AbortError인 경우 무시
        if (err.name === 'AbortError') return;
        
        if (isCurrent) {
          if (err.status === 409) {
            setNicknameCheck({ status: 'taken', message: '이미 사용 중인 닉네임입니다.' });
          } else {
            setNicknameCheck({ status: 'error', message: '확인 중 오류가 발생했습니다.' });
          }
        }
      }
    }, 500);

    return () => {
      isCurrent = false;
      clearTimeout(timer);
      controller.abort();
    };
  }, [form.nickname, user?.nickname]);

  const handleNicknameChange = (e) => {
    const value = e.target.value;
    setForm((f) => ({ ...f, nickname: value }));
  };

  const socialProvider = user?.kakaoId
    ? '카카오'
    : user?.naverId
      ? '네이버'
      : user?.googleId
        ? '구글'
        : null;
  const isSocialOnly = !!socialProvider && !user?.username;

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
      notificationOptIn: !!form.notificationOptIn,
    };
    if (form.newPassword) {
      body.currentPassword = form.currentPassword || null;
      body.newPassword = form.newPassword;
    }

    try {
      const updated = await api.put('/auth/me', body);
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
          <input
            type="text"
            value={user.username || (socialProvider ? `(${socialProvider} 로그인)` : '')}
            disabled
          />
        </div>

        <div className="profile-edit-row">
          <label>닉네임</label>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4, flex: 1 }}>
            <input
              type="text"
              value={form.nickname}
              onChange={handleNicknameChange}
              placeholder="닉네임 (50자 이하)"
              maxLength={50}
            />
            {nicknameCheck.status === 'checking' && (
              <span style={{ fontSize: 12, color: '#94a3b8' }}>확인 중…</span>
            )}
            {nicknameCheck.status === 'available' && (
              <span style={{ fontSize: 12, color: '#16a34a' }}>✅ {nicknameCheck.message}</span>
            )}
            {nicknameCheck.status === 'taken' && (
              <span style={{ fontSize: 12, color: '#dc2626' }}>⚠️ {nicknameCheck.message}</span>
            )}
            {nicknameCheck.status === 'error' && (
              <span style={{ fontSize: 12, color: '#dc2626' }}>{nicknameCheck.message}</span>
            )}
          </div>
        </div>

        <div className="profile-edit-row">
          <label>이메일</label>
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            placeholder="example@example.com"
            maxLength={200}
            disabled={isSocialOnly}
          />
        </div>

        <div className="profile-edit-row profile-edit-checkbox-row">
          <label>공지 메일</label>
          <label className="profile-edit-checkbox">
            <input
              type="checkbox"
              checked={form.notificationOptIn}
              onChange={(e) => setForm({ ...form, notificationOptIn: e.target.checked })}
            />
            <span>관리자가 보내는 공지 메일을 받을게요</span>
          </label>
        </div>

        {user?.kakaoId && (
          <div className="profile-edit-row profile-edit-checkbox-row">
            <label>카카오톡 알림</label>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <span style={{ color: '#475569', fontSize: 13 }}>
                매일 저녁 8시 주요 지수·종목 시세를 카카오톡 "나에게 보내기"로도 받으실 수 있어요.
              </span>
              <span style={{ color: '#94a3b8', fontSize: 12 }}>
                (공지 메일 수신 동의 + 카카오 로그인 시 talk_message 동의 시 자동 발송)
              </span>
            </div>
          </div>
        )}

        {!isSocialOnly && (
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

        {isSocialOnly && (
          <div className="profile-edit-info">
            {socialProvider} 로그인 계정은 이메일과 비밀번호를 변경할 수 없습니다.
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