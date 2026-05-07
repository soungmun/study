import { useState } from 'react';
import { Link } from 'react-router-dom';

const API = 'http://localhost:8080/api/auth';

export default function PasswordForgot() {
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    setSubmitting(true);
    try {
      const r = await fetch(`${API}/password/forgot`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(data.message || '요청 실패');
      setMessage(data.message || '메일을 발송했습니다.');
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <div className="toolbar">
        <h2>🔑 비밀번호 찾기</h2>
        <span className="muted">가입한 이메일로 재설정 링크를 보내드려요</span>
      </div>
      <form className="profile-edit-form" onSubmit={onSubmit}>
        <div className="profile-edit-row">
          <label>이메일</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="가입 시 등록한 이메일"
            required
          />
        </div>
        {message && <div className="profile-edit-success">✅ {message}</div>}
        {error && <div className="error">⚠️ {error}</div>}
        <div className="profile-edit-actions">
          <Link to="/" className="ghost" style={{ textDecoration: 'none' }}>취소</Link>
          <button type="submit" className="primary" disabled={submitting}>
            {submitting ? '전송 중…' : '재설정 메일 보내기'}
          </button>
        </div>
      </form>
    </div>
  );
}