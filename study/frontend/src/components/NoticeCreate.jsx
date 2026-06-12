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

  // 업로드된 이미지 목록: [{ imageId, url, originalName, fileSize }]
  const [images, setImages] = useState([]);
  const [uploading, setUploading] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  // 파일 선택 시 바로 서버에 업로드
  const handleFileChange = async (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;
    e.target.value = ''; // 같은 파일 재선택 허용

    setUploading(true);
    for (const file of files) {
      const fd = new FormData();
      fd.append('file', file);
      try {
        const r = await fetch(`${BASE_URL}/images`, {
          method: 'POST',
          credentials: 'include',
          body: fd,
        });
        if (!r.ok) {
          alert(`업로드 실패 (${file.name}): ${await readError(r)}`);
          continue;
        }
        const data = await r.json();
        setImages((prev) => [...prev, data]);
      } catch (err) {
        alert(`업로드 실패 (${file.name}): ${err.message}`);
      }
    }
    setUploading(false);
  };

  const removeImage = (imageId) => {
    setImages((prev) => prev.filter((img) => img.imageId !== imageId));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    const r = await fetch(BASE_URL, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...form,
        imageIds: images.map((img) => img.imageId),
      }),
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

        {/* 이미지 업로드 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <span style={{ fontWeight: 600, fontSize: 14, color: '#374151' }}>이미지 첨부</span>
          <label
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              padding: '8px 16px',
              border: '1px dashed #94a3b8',
              borderRadius: 8,
              cursor: uploading ? 'not-allowed' : 'pointer',
              color: '#475569',
              fontSize: 14,
              width: 'fit-content',
              background: uploading ? '#f1f5f9' : '#fff',
            }}
          >
            <span>📎</span>
            <span>{uploading ? '업로드 중...' : '이미지 선택 (JPEG, PNG, GIF, WEBP · 최대 10MB)'}</span>
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              multiple
              onChange={handleFileChange}
              disabled={uploading || submitting}
              style={{ display: 'none' }}
            />
          </label>

          {/* 업로드된 이미지 미리보기 */}
          {images.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 4 }}>
              {images.map((img) => (
                <div
                  key={img.imageId}
                  style={{
                    position: 'relative',
                    width: 120,
                    borderRadius: 8,
                    overflow: 'hidden',
                    border: '1px solid #e2e8f0',
                    background: '#f8fafc',
                  }}
                >
                  <img
                    src={img.url}
                    alt={img.originalName}
                    style={{ width: '100%', height: 90, objectFit: 'cover', display: 'block' }}
                  />
                  <div style={{ padding: '4px 6px', fontSize: 11, color: '#64748b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {img.originalName}
                  </div>
                  <button
                    type="button"
                    onClick={() => removeImage(img.imageId)}
                    style={{
                      position: 'absolute', top: 4, right: 4,
                      width: 22, height: 22,
                      borderRadius: '50%',
                      background: 'rgba(0,0,0,0.55)',
                      color: '#fff',
                      border: 'none',
                      cursor: 'pointer',
                      fontSize: 13,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      lineHeight: 1,
                    }}
                    title="제거"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="actions">
          <button type="button" className="ghost" onClick={() => navigate(-1)} disabled={submitting || uploading}>취소</button>
          <button type="submit" className="primary" disabled={submitting || uploading}>등록</button>
        </div>
      </form>
    </div>
  );
}
