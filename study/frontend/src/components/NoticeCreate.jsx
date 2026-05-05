import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const BASE_URL = 'http://localhost:8080/api/notices';

async function readError(res) {
  const text = await res.text();
  try { return JSON.parse(text).message || text; } catch { return text || `HTTP ${res.status}`; }
}

export default function NoticeCreate() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ author: '', title: '', content: '' });
  const [submitting, setSubmitting] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    const r = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form),
    });
    setSubmitting(false);
    if (!r.ok) {
      alert(await readError(r));
      return;
    }
    alert('등록되었습니다.');
    navigate('/');
  };

  return (
    <div className="card">
      <div className="toolbar">
        <h2>새 글 작성</h2>
      </div>
      <form onSubmit={handleSubmit} className="form">
        <label>
          작성자
          <input name="author" value={form.author} onChange={onChange} placeholder="이름을 입력하세요" />
        </label>
        <label>
          제목
          <input name="title" value={form.title} onChange={onChange} placeholder="제목을 입력하세요" />
        </label>
        <label>
          내용
          <textarea name="content" value={form.content} onChange={onChange} rows={10} placeholder="내용을 입력하세요" />
        </label>
        <div className="actions">
          <button type="button" className="ghost" onClick={() => navigate(-1)} disabled={submitting}>취소</button>
          <button type="submit" className="primary" disabled={submitting}>등록</button>
        </div>
      </form>
    </div>
  );
}