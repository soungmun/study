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

const CUSTOM_ID = '__custom__';

export default function MapSearch() {
  const mapRef = useRef(null);
  const mapInstance = useRef(null);
  const markersRef = useRef([]);
  const infoWindowRef = useRef(null);
  const customMarkerRef = useRef(null);
  const geocoderRef = useRef(null);

  const [kakaoReady, setKakaoReady] = useState(false);
  const [kakaoError, setKakaoError] = useState(null);
  const [query, setQuery] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [page, setPage] = useState(1);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [customPlace, setCustomPlace] = useState(null);

  const reverseGeocode = (lat, lng, fallbackName) => {
    const kakao = window.kakao;
    if (!geocoderRef.current && kakao?.maps?.services) {
      geocoderRef.current = new kakao.maps.services.Geocoder();
    }
    const geocoder = geocoderRef.current;
    if (!geocoder) {
      setCustomPlace({
        id: CUSTOM_ID,
        place_name: fallbackName ?? '선택한 위치',
        category_name: '지도에서 선택한 좌표',
        category_group_name: '커스텀 핀',
        road_address_name: '',
        address_name: `${lat.toFixed(6)}, ${lng.toFixed(6)}`,
        x: String(lng),
        y: String(lat),
      });
      return;
    }
    geocoder.coord2Address(lng, lat, (res, status) => {
      const ok = status === kakao.maps.services.Status.OK && res?.[0];
      const r = ok ? res[0] : null;
      const buildingName = r?.road_address?.building_name;
      const placeName = buildingName || fallbackName || '선택한 위치';
      setCustomPlace({
        id: CUSTOM_ID,
        place_name: placeName,
        category_name: '지도에서 선택한 좌표',
        category_group_name: buildingName ? '건물' : '커스텀 핀',
        road_address_name: r?.road_address?.address_name || '',
        address_name: r?.address?.address_name || `${lat.toFixed(6)}, ${lng.toFixed(6)}`,
        x: String(lng),
        y: String(lat),
      });
    });
  };

  const placeCustomMarker = (lat, lng, fallbackName) => {
    const kakao = window.kakao;
    const map = mapInstance.current;
    if (!kakao || !map) return;
    const pos = new kakao.maps.LatLng(lat, lng);
    if (customMarkerRef.current) {
      customMarkerRef.current.setMap(null);
    }
    const marker = new kakao.maps.Marker({
      position: pos,
      map,
      zIndex: 10,
    });
    customMarkerRef.current = marker;
    setSelectedId(CUSTOM_ID);
    map.panTo(pos);
    reverseGeocode(lat, lng, fallbackName);
  };

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
        if (kakao.maps.services) {
          geocoderRef.current = new kakao.maps.services.Geocoder();
        }
        kakao.maps.event.addListener(map, 'click', (mouseEvent) => {
          const latlng = mouseEvent.latLng;
          placeCustomMarker(latlng.getLat(), latlng.getLng());
        });
        setKakaoReady(true);
        placeCustomMarker(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng, '서울 시청');
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

    if (customMarkerRef.current) {
      customMarkerRef.current.setMap(null);
      customMarkerRef.current = null;
    }
    setCustomPlace(null);

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
      placeCustomMarker(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng, '서울 시청');
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
        <div className="map-canvas-wrap">
          <div className="map-canvas" ref={mapRef} />
          <div className="map-hint">🖱 지도를 클릭하면 그 위치의 주소가 오른쪽에 표시돼요</div>
        </div>
        <PlaceDetail
          place={
            selectedId === CUSTOM_ID
              ? customPlace
              : documents.find((d) => d.id === selectedId) ?? null
          }
        />
      </div>
    </div>
  );
}

function PlaceDetail({ place }) {
  if (!place) {
    return (
      <aside className="place-detail place-detail-empty">
        <div className="book-empty-icon">🗺️</div>
        <p>지도 마커나 목록을 클릭하면<br />상세 정보가 여기에 표시돼요</p>
      </aside>
    );
  }

  const categoryParts = (place.category_name || '').split('>').map((s) => s.trim()).filter(Boolean);
  const group = place.category_group_name;
  const lastCat = categoryParts[categoryParts.length - 1];
  const summary = group || lastCat || '카테고리 정보 없음';
  const distance = place.distance ? `${Number(place.distance).toLocaleString()}m` : null;

  const region = (() => {
    const src = place.address_name || place.road_address_name || '';
    const tokens = src.split(/\s+/).filter(Boolean);
    return tokens.slice(0, 2).join(' ');
  })();

  const descriptor = (() => {
    const kind = group || lastCat;
    if (!kind && !region) return null;
    if (region && kind) return `${region}에 위치한 ${kind}이에요.`;
    if (kind) return `${kind} 카테고리의 장소예요.`;
    return `${region}에 위치한 장소예요.`;
  })();

  return (
    <aside className="place-detail">
      <div className="place-detail-header">
        <div className="place-detail-name">{place.place_name}</div>
        <div className="place-detail-summary">{summary}</div>
      </div>

      <div className="place-detail-body">
        {(categoryParts.length > 0 || group || region || descriptor) && (
          <div className="place-detail-row place-detail-row-block">
            <span className="place-detail-label">🏷️ 분류</span>
            <div className="place-detail-value place-detail-info">
              <div className="place-detail-info-tags">
                {group && <span className="info-tag info-tag-primary">{group}</span>}
                {region && <span className="info-tag">📍 {region}</span>}
              </div>
              {categoryParts.length > 0 && (
                <div className="place-detail-info-path">
                  {categoryParts.map((c, i) => (
                    <span key={i}>
                      {i > 0 && <span className="info-sep">›</span>}
                      <span>{c}</span>
                    </span>
                  ))}
                </div>
              )}
              {descriptor && (
                <div className="place-detail-info-desc">{descriptor}</div>
              )}
            </div>
          </div>
        )}

        {place.road_address_name && (
          <div className="place-detail-row">
            <span className="place-detail-label">📮 도로명</span>
            <span className="place-detail-value">{place.road_address_name}</span>
          </div>
        )}

        {place.address_name && place.address_name !== place.road_address_name && (
          <div className="place-detail-row">
            <span className="place-detail-label">🏠 지번</span>
            <span className="place-detail-value">{place.address_name}</span>
          </div>
        )}

        {place.phone && (
          <div className="place-detail-row">
            <span className="place-detail-label">📞 전화</span>
            <a className="place-detail-value link" href={`tel:${place.phone}`}>{place.phone}</a>
          </div>
        )}

        {distance && (
          <div className="place-detail-row">
            <span className="place-detail-label">📏 거리</span>
            <span className="place-detail-value">{distance}</span>
          </div>
        )}
      </div>

      {place.place_url && (
        <a
          className="place-detail-link"
          href={place.place_url}
          target="_blank"
          rel="noopener noreferrer"
        >
          카카오맵에서 자세히 보기 →
        </a>
      )}
    </aside>
  );
}