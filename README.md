# TripCart

UI (Compose)
    ↓
ViewModel # UI랑 Repository 연결
    ↓
Repository # 중간 관리자
    ↓
──────────────
│   Firestore (클라우드 NoSQL)
│   Room(Local SQL)
│      ├─ Entity # 각 테이블 정의
│      ├─ Dao # 테이블에 데이터 삽입하는 방식 정의
│      └─ Converter # SQL에서 다루지 못하는 배열, 날짜 등 특수 타입 데이터 구현
──────────────