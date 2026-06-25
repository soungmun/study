import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../utils/api';

export default function PasswordReset() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const navigate = useNavigate();
  const [pw, setPw] = useState('');
  const [pw2, setPw2] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    
    if (!token) {
      setError('재설정 토큰이 없습니다. 메일의 링크로 다시 접속해 주세요.');
      return;
    }
    if (pw.length < 6) {
      setError('비밀번호는 6자 이상이어야 합니다.');
      return;
    }
    if (pw !== pw2) {
      setError('새 비밀번호 확인이 일치하지 않습니다.');
      return;
    }

    setSubmitting(true);
    try {
      const data = await api.post('/auth/password/reset', { token, newPassword: pw });
      setMessage(data.message || '비밀번호가 변경되었습니다.');
      setTimeout(() => navigate('/'), 1500);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <div className="toolbar">
        <h2>🔐 비밀번호 재설정</h2>
        <span className="muted">새 비밀번호를 입력하세요</span>
      </div>
      <form className="profile-edit-form" onSubmit={onSubmit}>
        <div className="profile-edit-row">
          <label>새 비밀번호</label>
          <input
            type="password"
            value={pw}
            onChange={(e) => setPw(e.target.value)}
            placeholder="6자 이상"
            required
            autoComplete="new-password"
          />
        </div>
        <div className="profile-edit-row">
          <label>비밀번호 확인</label>
          <input
            type="password"
            value={pw2}
            onChange={(e) => setPw2(e.target.value)}
            placeholder="다시 입력"
            required
            autoComplete="new-password"
          />
        </div>
        {message && <div className="profile-edit-success">✅ {message}</div>}
        {error && <div className="error">⚠️ {error}</div>}
        <div className="profile-edit-actions">
          <Link to="/" className="ghost" style={{ textDecoration: 'none' }}>취소</Link>
          <button type="submit" className="primary" disabled={submitting}>
            {submitting ? '변경 중…' : '비밀번호 변경'}
          </button>
        </div>
      </form>
    </div>
  );
}