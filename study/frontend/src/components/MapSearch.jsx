import { useEffect, useRef, useState } from 'react';

const BASE_URL = 'http://localhost:8080/api/places/search';
const PAGE_SIZE = 15;
const DEFAULT_CENTER = { lat: 37.566826, lng: 126.9786567 }; // 서울 시청

function loadKakao() {
  return new Promise((resolve, reject) => {
    if (window.kakao?.maps?.Map) {
      resolve(window.kakao);
      return;
    }
    if (!window.kakao?.maps?.load) {
      reject(new Error('Kakao SDK가 로드되지 않았습니다.'));
      return;
    }
    window.kakao.maps.load(() => resolve(window.kakao));
  });
}

export default function MapSearch() {
  const mapRef = useRef(null);
  const mapInstance = useRef(null);
  const markersRef = useRef([]);
  const infoWindowRef = useRef(null);

  const [kakaoReady, setKakaoReady] = useState(false);
  const [kakaoError, setKakaoError] = useState(null);
  const [query, setQuery] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [page, setPage] = useState(1);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedId, setSelectedId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    loadKakao()
      .then((kakao) => {
        if (cancelled) return;
        const map = new kakao.maps.Map(mapRef.current, {
          center: new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng),
          level: 5,
        });
        mapInstance.current = map;
        infoWindowRef.current = new kakao.maps.InfoWindow({ removable: true });
        setKakaoReady(true);
      })
      .catch((e) => setKakaoError(e.message));
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!submitted.trim()) {
      setResult(null);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({
      query: submitted.trim(),
      page: String(page),
      size: String(PAGE_SIZE),
    });
    fetch(`${BASE_URL}?${params.toString()}`, { signal: controller.signal })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setResult(data);
        setLoading(false);
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setError(e.message);
        setLoading(false);
      });
    return () => controller.abort();
  }, [submitted, page]);

  useEffect(() => {
    if (!kakaoReady || !mapInstance.current) return;
    const kakao = window.kakao;
    const map = mapInstance.current;

    markersRef.current.forEach((m) => m.setMap(null));
    markersRef.current = [];
    infoWindowRef.current?.close();

    const docs = result?.documents ?? [];
    if (docs.length === 0) return;

    const bounds = new kakao.maps.LatLngBounds();
    docs.forEach((p) => {
      const pos = new kakao.maps.LatLng(parseFloat(p.y), parseFloat(p.x));
      const marker = new kakao.maps.Marker({ position: pos, map });
      kakao.maps.event.addListener(marker, 'click', () => {
        focusPlace(p);
      });
      markersRef.current.push(marker);
      bounds.extend(pos);
    });
    map.setBounds(bounds);
  }, [result, kakaoReady]);

  const focusPlace = (place) => {
    if (!kakaoReady) return;
    const kakao = window.kakao;
    const map = mapInstance.current;
    const pos = new kakao.maps.LatLng(parseFloat(place.y), parseFloat(place.x));
    map.panTo(pos);
    setSelectedId(place.id);

    const html = `
      <div style="padding:8px 10px;font-size:13px;line-height:1.5;min-width:180px;max-width:240px;">
        <div style="font-weight:700;color:#1e1b4b;margin-bottom:4px;">${place.place_name}</div>
        ${place.road_address_name ? `<div style="color:#475569;">${place.road_address_name}</div>` : ''}
        ${place.address_name && place.address_name !== place.road_address_name ? `<div style="color:#94a3b8;font-size:12px;">${place.address_name}</div>` : ''}
        ${place.phone ? `<div style="color:#0891b2;margin-top:4px;">📞 ${place.phone}</div>` : ''}
        <a href="${place.place_url}" target="_blank" rel="noopener" style="display:inline-block;margin-top:6px;color:#a855f7;text-decoration:none;font-weight:600;">카카오맵에서 보기 →</a>
      </div>
    `;
    const tempMarker = markersRef.current.find((m) => {
      const p = m.getPosition();
      return p.getLat() === pos.getLat() && p.getLng() === pos.getLng();
    });
    if (tempMarker && infoWindowRef.current) {
      infoWindowRef.current.setContent(html);
      infoWindowRef.current.open(map, tempMarker);
    }
  };

  const onSearch = (e) => {
    e.preventDefault();
    setPage(1);
    setSubmitted(query);
    setSelectedId(null);
  };

  const onReset = () => {
    setQuery('');
    setSubmitted('');
    setResult(null);
    setError(null);
    setSelectedId(null);
    if (kakaoReady) {
      markersRef.current.forEach((m) => m.setMap(null));
      markersRef.current = [];
      infoWindowRef.current?.close();
      const kakao = window.kakao;
      mapInstance.current.setCenter(new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng));
      mapInstance.current.setLevel(5);
    }
  };

  const documents = result?.documents ?? [];
  const meta = result?.meta;
  const isEnd = meta?.is_end ?? true;

  return (
    <div className="card">
      <div className="toolbar">
        <h2>📍 장소 검색</h2>
        <span className="muted">카카오 지도 + 키워드 검색</span>
      </div>

      <form className="search-bar" onSubmit={onSearch}>
        <input
          type="text"
          className="search-input"
          placeholder="검색할 장소 (예: 카페, 강남역, 서울대)"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" className="primary" disabled={!query.trim()}>검색</button>
        {submitted && (
          <button type="button" className="ghost" onClick={onReset}>초기화</button>
        )}
      </form>

      {kakaoError && <div className="error">지도 로드 실패: {kakaoError}</div>}
      {error && <div className="error">에러: {error}</div>}

      <div className="map-layout">
        <div className="map-list">
          {loading && (
            <div className="book-empty">
              <div className="spinner" />
              <p>검색 중…</p>
            </div>
          )}
          {!loading && submitted && documents.length === 0 && !error && (
            <div className="book-empty">
              <div className="book-empty-icon">📭</div>
              <p>검색 결과가 없습니다.</p>
            </div>
          )}
          {!loading && !submitted && (
            <div className="book-empty">
              <div className="book-empty-icon">🔍</div>
              <p>장소를 검색해보세요</p>
            </div>
          )}
          {!loading && documents.length > 0 && (
            <>
              <div className="book-meta">
                <span>총 <strong>{meta.pageable_count.toLocaleString()}</strong>건</span>
                <span className="dot">·</span>
                <span>{page} 페이지</span>
              </div>
              <ul className="place-list">
                {documents.map((p) => (
                  <li
                    key={p.id}
                    className={`place-item ${selectedId === p.id ? 'active' : ''}`}
                    onClick={() => focusPlace(p)}
                  >
                    <div className="place-name">{p.place_name}</div>
                    {p.category_name && (
                      <div className="place-category">{p.category_name}</div>
                    )}
                    <div className="place-addr">
                      {p.road_address_name || p.address_name}
                    </div>
                    {p.phone && <div className="place-phone">📞 {p.phone}</div>}
                  </li>
                ))}
              </ul>
              <div className="pagination">
                <button onClick={() => setPage(1)} disabled={page === 1}>«</button>
                <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1}>‹</button>
                <span className="page-num active">{page}</span>
                <button onClick={() => setPage((p) => p + 1)} disabled={isEnd}>›</button>
              </div>
            </>
          )}
        </div>
        <div className="map-canvas" ref={mapRef} />
      </div>
    </div>
  );
}