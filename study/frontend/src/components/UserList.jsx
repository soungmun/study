import { useEffect, useState } from 'react';

const API = 'http://localhost:8080/api/users';

export default function UserList() {
  const [users, setUsers] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [unauthorized, setUnauthorized] = useState(false);

  const load = () => {
    setLoading(true);
    setError(null);
    setUnauthorized(false);
    fetch(API, { credentials: 'include' })
      .then(async (r) => {
        if (r.status === 401) {
          setUnauthorized(true);
          return null;
        }
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        if (data) setUsers(data);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const formatDate = (s) => {
    if (!s) return '—';
    const d = new Date(s);
    if (Number.isNaN(d.getTime())) return s;
    return d.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="card">
        <div className="toolbar">
          <h2>👥 회원 목록</h2>
        </div>
        <div className="book-empty">
          <div className="spinner" />
          <p>불러오는 중…</p>
        </div>
      </div>
    );
  }

  if (unauthorized) {
    return (
      <div className="card">
        <div className="toolbar">
          <h2>👥 회원 목록</h2>
          <span className="muted">가입자 전용 페이지</span>
        </div>
        <div className="user-locked">
          <div className="user-locked-icon">🔒</div>
          <div className="user-locked-title">로그인 후 확인할 수 있어요</div>
          <div className="user-locked-desc">
            우측 상단의 <b>회원가입</b> 또는 <b>로그인</b> 버튼을 눌러 계정을 만들어 주세요.<br />
            로그인 상태에서만 가입된 회원 목록을 볼 수 있어요.
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card">
        <div className="error">에러: {error}</div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="toolbar">
        <h2>👥 회원 목록</h2>
        <span className="muted">총 <strong>{users?.length ?? 0}</strong>명</span>
      </div>
      <div className="user-table-wrap">
        <table className="user-table">
          <thead>
            <tr>
              <th>#</th>
              <th>아이디</th>
              <th>닉네임</th>
              <th>이메일</th>
              <th>가입 경로</th>
              <th>가입일</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const isKakao = u.kakaoId != null;
              return (
                <tr key={u.id}>
                  <td className="user-id">{u.id}</td>
                  <td>{u.username || <span className="muted">—</span>}</td>
                  <td>{u.nickname || <span className="muted">—</span>}</td>
                  <td>{u.email || <span className="muted">—</span>}</td>
                  <td>
                    {isKakao ? (
                      <span className="user-badge user-badge-kakao">카카오</span>
                    ) : (
                      <span className="user-badge user-badge-local">일반</span>
                    )}
                  </td>
                  <td className="user-date">{formatDate(u.createdAt)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}