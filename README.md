# TripCart

UI (Compose)
    ↓
ViewModel # UI와 데이터 소스 연결, 비즈니스 로직 처리
    ↓
──────────────
│   데이터 소스
│   ├─ Remote API (Google Places API)
│   │   ├─ PlacesApiService # API 엔드포인트 정의 (무엇을 호출할지)
│   │   ├─ PlacesApiClient # Retrofit 설정 및 서비스 생성 (어떻게 호출할지)
│   │   └─ PlacesApiModels # API 응답 모델
│   │
│   ├─ Firestore (클라우드 NoSQL)
│   │   └─ ViewModel에서 직접 접근
│   │
│   └─ Room (Local SQL)
│       ├─ Entity # 각 테이블 정의
│       ├─ Dao # 테이블에 데이터 삽입하는 방식 정의
│       └─ Converter # SQL에서 다루지 못하는 배열, 날짜 등 특수 타입 데이터 구현
──────────────