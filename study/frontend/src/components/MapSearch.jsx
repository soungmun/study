import { useEffect, useRef, useState } from 'react';

const BASE_URL = 'http://localhost:8080/api/places/search';
const WEATHER_URL = 'http://localhost:8080/api/weather';
const AIR_URL = 'http://localhost:8080/api/air';
const PAGE_SIZE = 15;
const DEFAULT_CENTER = { lat: 37.4892775, lng: 126.7246513 }; // 부평역

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

const POI_CATEGORIES = [
  'FD6', 'CE7', 'CS2', 'BK9', 'HP8', 'PM9',
  'CT1', 'AT4', 'SW8', 'AD5', 'MT1', 'PO3', 'SC4', 'OL7',
];

export default function MapSearch() {
  const mapRef = useRef(null);
  const mapInstance = useRef(null);
  const markersRef = useRef([]);
  const labelsRef = useRef([]);
  const infoWindowRef = useRef(null);
  const customMarkerRef = useRef(null);
  const geocoderRef = useRef(null);
  const placesRef = useRef(null);

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
  const [weather, setWeather] = useState(null);
  const [weatherLoading, setWeatherLoading] = useState(false);
  const [weatherError, setWeatherError] = useState(null);
  const [air, setAir] = useState(null);
  const [airLoading, setAirLoading] = useState(false);
  const [airError, setAirError] = useState(null);

  const findNearestPlace = (lat, lng) =>
    new Promise((resolve) => {
      const kakao = window.kakao;
      if (!kakao?.maps?.services?.Places) {
        resolve(null);
        return;
      }
      if (!placesRef.current) {
        placesRef.current = new kakao.maps.services.Places();
      }
      const places = placesRef.current;
      const opts = {
        location: new kakao.maps.LatLng(lat, lng),
        radius: 80,
        sort: kakao.maps.services.SortBy.DISTANCE,
      };
      let pending = POI_CATEGORIES.length;
      let best = null;
      POI_CATEGORIES.forEach((code) => {
        places.categorySearch(
          code,
          (data, status) => {
            if (status === kakao.maps.services.Status.OK && data?.[0]) {
              const cand = data[0];
              const candDist = Number(cand.distance ?? Infinity);
              const bestDist = best ? Number(best.distance ?? Infinity) : Infinity;
              if (candDist < bestDist) best = cand;
            }
            if (--pending === 0) resolve(best);
          },
          opts
        );
      });
    });

  const reverseGeocode = (lat, lng, fallbackName) => {
    const kakao = window.kakao;
    if (!geocoderRef.current && kakao?.maps?.services) {
      geocoderRef.current = new kakao.maps.services.Geocoder();
    }

    const coord2AddressPromise = new Promise((resolve) => {
      const geocoder = geocoderRef.current;
      if (!geocoder) {
        resolve(null);
        return;
      }
      geocoder.coord2Address(lng, lat, (res, status) => {
        const ok = status === kakao.maps.services.Status.OK && res?.[0];
        resolve(ok ? res[0] : null);
      });
    });

    Promise.all([findNearestPlace(lat, lng), coord2AddressPromise]).then(([poi, addr]) => {
      const buildingName = addr?.road_address?.building_name;
      const roadAddr = addr?.road_address?.address_name || '';
      const jibunAddr = addr?.address?.address_name || '';

      if (poi) {
        setCustomPlace({
          id: CUSTOM_ID,
          place_name: poi.place_name,
          category_name: poi.category_name || '지도에서 선택한 위치',
          category_group_name: poi.category_group_name || '근처 장소',
          road_address_name: poi.road_address_name || roadAddr,
          address_name: poi.address_name || jibunAddr,
          phone: poi.phone,
          place_url: poi.place_url,
          x: String(lng),
          y: String(lat),
        });
        return;
      }

      const placeName = buildingName || fallbackName || roadAddr || jibunAddr || '지도에서 선택한 위치';
      setCustomPlace({
        id: CUSTOM_ID,
        place_name: placeName,
        category_name: '지도에서 선택한 좌표',
        category_group_name: buildingName ? '건물' : '커스텀 핀',
        road_address_name: roadAddr,
        address_name: jibunAddr,
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
        placeCustomMarker(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng, '부평역');
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
    labelsRef.current.forEach((l) => l.setMap(null));
    labelsRef.current = [];
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

      const lastCat = (p.category_name || '').split('>').map((s) => s.trim()).filter(Boolean).pop();
      const subText = p.category_group_name || lastCat || '';
      const labelEl = document.createElement('div');
      labelEl.className = 'map-place-label';
      const nameEl = document.createElement('div');
      nameEl.className = 'map-place-label-name';
      nameEl.textContent = p.place_name;
      labelEl.appendChild(nameEl);
      if (subText) {
        const subEl = document.createElement('div');
        subEl.className = 'map-place-label-sub';
        subEl.textContent = subText;
        labelEl.appendChild(subEl);
      }
      labelEl.addEventListener('click', () => focusPlace(p));
      const label = new kakao.maps.CustomOverlay({
        position: pos,
        content: labelEl,
        yAnchor: 2.4,
        xAnchor: 0.5,
        clickable: true,
        zIndex: 3,
      });
      label.setMap(map);
      labelsRef.current.push(label);

      bounds.extend(pos);
    });
    map.setBounds(bounds);
  }, [result, kakaoReady]);

  const selectedPlace =
    selectedId === CUSTOM_ID
      ? customPlace
      : (result?.documents ?? []).find((d) => d.id === selectedId) ?? null;

  useEffect(() => {
    if (!selectedPlace) {
      setWeather(null);
      setWeatherError(null);
      setWeatherLoading(false);
      setAir(null);
      setAirError(null);
      setAirLoading(false);
      return;
    }
    const lat = parseFloat(selectedPlace.y);
    const lng = parseFloat(selectedPlace.x);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

    const controller = new AbortController();
    setWeatherLoading(true);
    setWeatherError(null);
    setAirLoading(true);
    setAirError(null);

    fetch(`${WEATHER_URL}?lat=${lat}&lng=${lng}`, { signal: controller.signal })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setWeather(data);
        setWeatherLoading(false);
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setWeatherError(e.message);
        setWeatherLoading(false);
      });

    fetch(`${AIR_URL}?lat=${lat}&lng=${lng}`, { signal: controller.signal })
      .then(async (r) => {
        if (!r.ok) throw new Error(await r.text());
        return r.json();
      })
      .then((data) => {
        setAir(data);
        setAirLoading(false);
      })
      .catch((e) => {
        if (e.name === 'AbortError') return;
        setAirError(e.message);
        setAirLoading(false);
      });

    return () => controller.abort();
  }, [selectedPlace?.id, selectedPlace?.x, selectedPlace?.y]);

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
      labelsRef.current.forEach((l) => l.setMap(null));
      labelsRef.current = [];
      infoWindowRef.current?.close();
      const kakao = window.kakao;
      mapInstance.current.setCenter(new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng));
      mapInstance.current.setLevel(5);
      placeCustomMarker(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng, '부평역');
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
          place={selectedPlace}
          weather={weather}
          weatherLoading={weatherLoading}
          weatherError={weatherError}
          air={air}
          airLoading={airLoading}
          airError={airError}
        />
      </div>
    </div>
  );
}

function PlaceDetail({ place, weather, weatherLoading, weatherError, air, airLoading, airError }) {
  if (!place) {
    return (
      <aside className="place-detail place-detail-empty">
        <div className="book-empty-icon">🗺️</div>
        <p>지도 마커나 목록을 클릭하면<br />상세 정보가 여기에 표시돼요</p>
      </aside>
    );
  }

  const lastCat = (place.category_name || '').split('>').map((s) => s.trim()).filter(Boolean).pop();
  const summary = place.category_group_name || lastCat || '';

  return (
    <aside className="place-detail">
      <div className="place-detail-header">
        <div className="place-detail-name">{place.place_name}</div>
        <div className="place-detail-summary">{summary}</div>
      </div>

      <div className="place-detail-body">
        <div className="place-detail-row">
          <span className="place-detail-label">🌡️ 온도</span>
          <span className="place-detail-value">
            {weatherLoading && '불러오는 중…'}
            {!weatherLoading && weatherError && '온도 정보를 가져오지 못했어요'}
            {!weatherLoading && !weatherError && weather && (
              <>
                {weather.icon || ''} {weather.temperature != null ? `${Math.round(weather.temperature)}°C` : '—'}
                {weather.description ? ` · ${weather.description}` : ''}
              </>
            )}
          </span>
        </div>

        <AirCard
          air={air}
          loading={airLoading}
          error={airError}
        />
      </div>
    </aside>
  );
}

function AirCard({ air, loading, error }) {
  return (
    <div className="air-card">
      <div className="air-card-title">🌫️ 미세먼지</div>
      {loading && (
        <div className="air-card-status">
          <span className="spinner spinner-sm" />
          <span>대기질 정보를 불러오는 중…</span>
        </div>
      )}
      {!loading && error && (
        <div className="air-card-status" style={{ color: '#b91c1c' }}>
          ⚠️ 대기질 정보를 가져오지 못했어요
        </div>
      )}
      {!loading && !error && air && (
        <>
          <div className="air-card-grid">
            <div className="air-card-cell" style={{ background: air.pm10Color || '#94a3b8' }}>
              <span className="air-card-label">PM10 (미세먼지)</span>
              <span className="air-card-value">
                {air.pm10 != null ? `${Math.round(air.pm10)} ㎍/㎥` : '—'}
              </span>
              <span className="air-card-grade">{air.pm10Grade || '정보없음'}</span>
            </div>
            <div className="air-card-cell" style={{ background: air.pm25Color || '#94a3b8' }}>
              <span className="air-card-label">PM2.5 (초미세)</span>
              <span className="air-card-value">
                {air.pm25 != null ? `${Math.round(air.pm25)} ㎍/㎥` : '—'}
              </span>
              <span className="air-card-grade">{air.pm25Grade || '정보없음'}</span>
            </div>
          </div>
          {air.europeanAqi != null && (
            <div className="air-card-aqi">
              유럽 AQI 지수: <strong>{air.europeanAqi}</strong>
            </div>
          )}
        </>
      )}
    </div>
  );
}
