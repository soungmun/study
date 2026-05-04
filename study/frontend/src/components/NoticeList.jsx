function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  return d.toLocaleString();
}

export default function NoticeList({ notices, onSelect, onNew }) {
  return (
    <div className="card">
      <div className="toolbar">
        <h2>공지사항</h2>
        <button onClick={onNew}>글쓰기</button>
      </div>
      <table className="notice-table">
        <thead>
          <tr>
            <th style={{ width: 60 }}>번호</th>
            <th>제목</th>
            <th style={{ width: 120 }}>작성자</th>
            <th style={{ width: 180 }}>작성일</th>
          </tr>
        </thead>
        <tbody>
          {notices.length === 0 && (
            <tr>
              <td colSpan="4" className="empty">등록된 글이 없습니다.</td>
            </tr>
          )}
          {notices.map((n) => (
            <tr key={n.id} onClick={() => onSelect(n.id)} className="row">
              <td>{n.id}</td>
              <td className="title">{n.title}</td>
              <td>{n.author}</td>
              <td>{formatDate(n.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}