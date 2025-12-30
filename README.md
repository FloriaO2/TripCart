# ✈️ TripCart

**장소 기반 스마트 쇼핑 리스트 관리 앱**

---

<p align="center">
  <img width="255" alt="TripCart" src="https://github.com/user-attachments/assets/cd871286-dfee-4872-933d-18d6a720f2ab" />
  <img width="130" alt="KakaoTalk" src="https://github.com/user-attachments/assets/0718540f-894d-4001-bf8c-90a7a89dcc9c" />
</p>

## 🤷 What is TripCart?

**TripCart는 이런 앱입니다.**

✅ 위치와 상점을 중심으로 구성되는 **개인 / 공유 쇼핑 리스트 관리 앱**

✅ **구글 로그인**으로 간편 시작, 국내/해외 쇼핑 환경 모두 대응

✅ 현재 위치 근처의 등록 매장이 근처에 있으면 **푸시 알림**

✅ 공개 상품 검색 기반의 **상품 불러오기 + 상품 사용 랭킹 + 리뷰 시스템**

---

## 🫵 Who uses TripCart?

**TripCart는 이런 분에게 적합합니다!**

✅ 여행 중 쇼핑 동선을 스마트하게 관리하고 싶은 분

✅ 매장별로 쇼핑 리스트를 정리하고 싶은 분

✅ 일정이 빠듯할 때 **쇼핑 리스트를 공유해 실시간으로 협업함으로써 시간을 절약**하고 싶은 분

✅ 공개된 상품을 검색해 **빠르게 원하는 상품을 불러오고 각 리뷰를 참고**하고 싶은 분

---

## 🛠️ 기술 스택

> Frontend
> 
- `Kotlin` + `Jetpack Compose` – Android 네이티브 UI

> Backend
> 
- `Firebase Authentication` – Google Sign-In / ID Token 인증
- `Cloud Functions` – 푸시 알림, 상품 랭킹, 상태 동기화, 알림 트리거

> Location & Map
> 
- `Google Maps API` – 매장 마커 및 지도 렌더링
- `Google Places API` – 매장 검색 및 장소 상세 정보 제공
- `Geofencing` – 도보 4분 이내 매장 접근 감지 및 알림 트리거
- `Android SDK` – 실시간 현재 위치 수집

> Storage
> 
- `Firebase Storage` – 공개/리스트 상품 이미지 저장

> DB
> 
- `RoomDB` – 개인 리스트 오프라인 저장
- `Firestore` – 공유 리스트 및 공개 상품 데이터 실시간 관리

---

## 🛍️ TripCart의 주요 기능

## [Fun 1] 나만의 매장별 쇼핑 리스트

### 필요한 상품을 나만의 리스트로 관리하세요!

<br />
<p align="center">
  <img width="145" src="https://github.com/user-attachments/assets/fcc82c16-9241-4d6e-9c89-ed197c3e6269" style="margin:0 10px;"/>
  <img width="132" src="https://github.com/user-attachments/assets/be31e305-1561-49f5-a2be-d30734ff8400" style="margin:0 10px;"/>
  <img width="149" src="https://github.com/user-attachments/assets/59da4bb3-148b-4300-9caa-bbc87007e6b7" style="margin:0 10px;"/>
  <img width="177" src="https://github.com/user-attachments/assets/6e853481-e85b-48a0-a7ea-0c42e9883fc2" style="margin:0 10px;"/>
  <img width="177" src="https://github.com/user-attachments/assets/1e05167b-3a8d-4313-8c7e-dc150cf5ebe2" style="margin:0 10px;"/>
</p>
<br />

**🛒 상품 추가 및 리스트 연결**

1. 상품은 **검색해서 불러오거나, 직접 입력해서 추가**할 수 있어요
2. **사진, 이름, 품목**을 모두 입력한 상품은 공개할 수 있으며, **공개 상품**은 추후 검색해서 불러올 수 있어요
3. 수량과 간단한 메모를 함께 적어 저장할 수 있어요
4. 장볼 때 리스트에서 하나씩 체크하며 구매 현황을 관리해요
5. 리스트 항목은 필요하면 **수정 및 삭제**가 가능해요

<br />
<p align="center">
  <img width="130" src="https://github.com/user-attachments/assets/b12bae78-99af-4c05-a667-975ee55f84f9" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/4166e398-964e-4592-a6f0-8bd1542f4274" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/7140e5f8-efe4-43c1-b9d6-5a426c676375" style="margin:0 10px;" />
</p>
<br />

**🏬 상점 추가 및 리스트 연결**

1. **검색**을 통해 **원하는 상점을 찾아 리스트에 추가**해요
2. **한 리스트에 여러 상점**을 추가하거나, **한 상점을 여러 리스트**에 추가할 수 있어요

<br />
<p align="center">
  <img width="143" src="https://github.com/user-attachments/assets/7c2a9e10-006b-45b3-8456-6e704595c1ef" style="margin:0 10px;" />
  <img width="143" src="https://github.com/user-attachments/assets/7e0f54be-97b0-4562-a6f6-57317b486447" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/90177148-5ab7-4344-9207-1fb765d53b5b" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/e3b51928-0884-4aff-8ad4-34c40fd01ab4" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/c21d1f18-9c2f-4f9f-a98b-31659ab5435c" style="margin:0 10px;" />
</p>
<br />

**📱 상점 정보 확인 및 외부 앱 연동**

1. 리스트 상세페이지에서 추가한 매장의 **운영 시간, 전화번호, 공식 웹사이트 정보** 등을 확인해요
2. **전화번호**를 눌러 **전화 앱으로 이동**하거나, **공식 웹사이트 링크**를 통해 **브라우저로 이동**할 수 있어요
3. **내비게이션 버튼**을 누르면 **현재 위치 및 해당 매장 위치를 이용해 구글 길찾기와 연동**돼요
4. 운영 시간에서는 **오늘의 운영 시간**을 먼저 보여주기 때문에 편리하게 오늘의 정보를 확인할 수 있어요

<br />
<p align="center">
  <img width="130" src="https://github.com/user-attachments/assets/c23e34a1-501e-4dde-98fb-1cb479559c72" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/e4cace64-628a-4b07-8d79-93d203db73c9" style="margin:0 10px;" />
</p>
<br />

**💡 찜한 상품 활용**

1. 공개 상품 한눈에 보기 페이지에서 마음에 드는 상품은 **찜**을 할 수 있어요
2. **마이페이지에서 찜한 상품을 한 번에 모아보고**, 거기서 바로
    - 내 쇼핑 리스트에 상품을 추가하거나
    - 해당 상품의 리뷰를 확인할 수 있어요
    

---

## [Fun 2] 친구들과 공유하는 쇼핑 리스트

### 그룹을 만들어 함께 장보기 리스트를 공유하세요!

<br />
<div align="center">
  <p>
    <img width="130" src="https://github.com/user-attachments/assets/294e7dc1-2394-46f6-9ebf-34c7b76c1445" style="margin:0 10px;" />
    <img width="130" src="https://github.com/user-attachments/assets/3dac66c6-c676-4303-b3d5-9f34bfdc7b7a" style="margin:0 10px;" />
    <img width="130" src="https://github.com/user-attachments/assets/606aaf0f-872b-496d-a7e0-b4d493522140" style="margin:0 10px;" />
  </p>
  <p><b>▲ 개인 리스트를 공유 리스트로 전환</b></p>
</div>

<div align="center">
  <p>
    <img width="130" src="https://github.com/user-attachments/assets/7203e847-21e2-4139-aa72-c7c8c83f8db3" style="margin:0 10px;" />
    <img width="130" src="https://github.com/user-attachments/assets/bf0c2814-dec4-458e-b88d-06797aba1c68" style="margin:0 10px;" />
    <img width="130" src="https://github.com/user-attachments/assets/03caed23-65ff-45d6-a004-dbe90431dfd4" style="margin:0 10px;" />
    <img width="118" src="https://github.com/user-attachments/assets/a9cae98f-6cc5-4f4d-86fa-d1241c45c151" style="margin:0 10px;" />
  </p>
  <p><b>▲ 초대 코드를 이용해 공유 리스트에 참여</b></p>
</div>
<br />


**💡 함께 장보기, 더 편하게**

1. **초대 코드**를 이용해 **다른 사람의 리스트에 참여**할 수 있어요
2. 그룹 멤버끼리 상품 추가·체크·메모를 **함께 편집**해요
3. 무엇을 이미 구매한 상태인지 빠르게 확인해요
4. 변경 사항이 모두에게 **즉시 업데이트**돼요

<br />
<p align="center">
  <img width="133" src="https://github.com/user-attachments/assets/4ba81481-fae7-4c95-b212-2972e5baa59a" style="margin:0 10px;" />
  <img width="146" src="https://github.com/user-attachments/assets/a90977e4-18c5-4247-a24b-cfcfedaf87b1" style="margin:0 10px;" />
</p>

<p align="center">
  <img width="133" src="https://github.com/user-attachments/assets/4226d0ff-3fe8-484f-afd3-5bbc2438c149" style="margin:0 10px;" />
  <img width="146" src="https://github.com/user-attachments/assets/7f198a45-cc00-4f04-97c8-243711969e7b" style="margin:0 10px;" />
  <img width="133" src="https://github.com/user-attachments/assets/fad4ec2a-c91d-480e-ac25-af7f1cd2b70f" style="margin:0 10px;" />
  <img width="146" src="https://github.com/user-attachments/assets/ad724a5b-9d09-46b7-b39d-954214b62cdd" style="margin:0 10px;" />
  <img width="146" src="https://github.com/user-attachments/assets/0a50b615-c317-4070-91a1-3bf3f0774caf" style="margin:0 10px;" />
</p>
<br />

**✅ 급한 일은 리스트 채팅을 사용**

1. **리스트별 채팅** 기능을 제공함으로써 그룹 멤버들끼리 대화를 나눌 수 있어요
2. 채팅에서는 **실시간으로 촬영한 사진을 전송**함으로써 현재 상황을 빠르게 공유할 수 있어요
3. 채팅이 도착하면 다른 멤버들에게 **푸시 알림 및 앱 내 알림**을 통해 알려줘요
    - 앱 내 알림은 삭제 버튼을 누르거나 옆으로 밀어 삭제할 수 있어요!
4. 채팅 알림을 누르면 해당 채팅방으로 **빠르게 이동**할 수 있어요

---

## [Fun 3] 지도 기반 매장 탐색

### 주변 매장을 지도에서 빠르게 찾아보세요!

<br />
<p align="center">
  <img width="130" src="https://github.com/user-attachments/assets/fbab9b94-16a2-4814-8ea0-e64b0b7dfc5a" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/f53a012d-6582-4b0b-bcaf-7201c390cc52" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/c35ca6ec-b014-4568-89d6-42e66adcb037" style="margin:0 10px;" />
</p>
<br />

**📍 현재 위치 기반 매장 마커 확인**

1. 현재 지도 화면이 **내 위치 근처일 때** 지도 중심이 실시간으로 반영돼요
2. 지도에는 상점 **마커들**이 쭉 표시되어, 주변에 있는 매장을 빠르게 확인할 수 있어요
3. 매장을 누르면 **해당 매장과 관련된 쇼핑 리스트**가 바로 열려요

<br />
<p align="center">
  <img width="143" src="https://github.com/user-attachments/assets/27230b10-9224-4410-a33d-ffe6f6a47663" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/ed926398-3da4-414a-a368-636075c4a0a5" style="margin:0 10px;" />
</p>
https://github.com/user-attachments/assets/d5b22ae4-327c-48c0-a6c4-fe17fb224b61
<br />

**🔔 리스트 속 매장 근처 도착 알림**

1. 내가 쇼핑 리스트에 **추가해 둔 매장 주변에 도착**하면 **푸시 알림**이 자동으로 떠요
2. 알림을 누르면 **바로 지도 화면으로 이동**해 매장 위치와 현재 위치를 비교할 수 있어요
3. 이를 통해 **근처 매장의 존재를 놓치거나 방문 기회를 잃는 상황을 줄여줘요**

---

## [Fun 4] 리뷰 랭킹 & 추천

<p align="center">
  <img width="155" src="https://github.com/user-attachments/assets/29967ee0-39e0-4c71-9899-779a16821f20" style="margin:0 10px;" />
  <img width="155" src="https://github.com/user-attachments/assets/8275e5a4-6099-4f80-897b-5e87c882a948" style="margin:0 10px;" />
  <img width="155" src="https://github.com/user-attachments/assets/e1885c21-ce79-48f6-8ca1-295ab8407f11" style="margin:0 10px;" />
</p>
<br />

**✅ 국가별 인기 확인 → 매장 검색 → 상품 랭킹 확인!**

1. 사용자들이 **리스트에 많이 추가한 매장 정보를 기준**으로,
    
    **여행객이 많이 방문한 국가 TOP3**와 각국의 인기 상품을 확인할 수 있어요
    
2. 특정 국가나 매장과 관련한 순위는, **검색해서 확인할 수 있어요**
3. 검색 결과에서는 **해당 매장에 등록된 공개 상품 랭킹**이
    
    **불러온 횟수가 많은 순서대로 나열**되어 있어요

<br />
<p align="center">
  <img width="150" src="https://github.com/user-attachments/assets/9f350f7d-658b-4be2-98d3-6b1ad84e6f0e" style="margin:0 10px;" />
  <img width="150" src="https://github.com/user-attachments/assets/e82cbd72-fff5-4a78-8450-9993772686cd" style="margin:0 10px;" />
  <img width="140" src="https://github.com/user-attachments/assets/cd2df8d3-ccc2-424f-9ab5-740d421d5f40" style="margin:0 10px;" />
  <img width="150" src="https://github.com/user-attachments/assets/cde68602-9850-4d18-a0d9-81f85caedc06" style="margin:0 10px;" />
</p>
<br />

**🧾 모든 공개 상품 한눈에 보기**

1. **리뷰 별점이 높은 순서대로** 정렬되어있어요
2. **원하는 카테고리나 키워드에 해당하는 상품**만 필터링해서 볼 수 있어요
3. 상품을 누르면 **리뷰 확인**이 가능해요
4. **내가 쓴 리뷰**는 파란 박스에 배치되어 한눈에 확인할 수 있어요

---

## [Fun 5] 리스트 한눈에 보기

### 내가 만든 쇼핑 리스트에 빠르게 접근하고 자유롭게 관리하세요!

<br />
<p align="center">
  <img width="150" src="https://github.com/user-attachments/assets/dc539621-fab3-4e0e-b222-07f516f6d94f" style="margin:0 10px;" />
  <img width="150" src="https://github.com/user-attachments/assets/88470d62-42cc-4dc2-bde7-c76e180cb22c" style="margin:0 10px;" />
</p>
<br />

**📌 앱 진입 시 리스트 접근**

**✅ 앱 실행 → 진행 중인 리스트로 자동 이동!**

1. 앱에 처음 들어오면 **현재 진행 중인 리스트**가 바로 열려요
2. 그래서 **지금 장보기 중인 리스트에 빠르게 접근**할 수 있어요
3. 다른 리스트는 하단의 **전체 리스트** 탭을 통해 **모두 확인**할 수 있어요

<br />
<p align="center">
  <img width="130" src="https://github.com/user-attachments/assets/095dd4e8-873f-4c47-8b9e-c87f685e4d7a" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/d7229709-4047-4c87-9a2b-6cceab8676b5" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/3cf03759-d11c-4bc5-a427-b524798b3913" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/1ed41829-81e8-4972-b2c1-0aed5c567129" style="margin:0 10px;" />
  <img width="130" src="https://github.com/user-attachments/assets/b9867864-47a6-46af-9de2-4d8435021142" style="margin:0 10px;" />
</p>
<br />

**🧾 리스트 관리 및 조작**

**✅ 리스트 선택 → 상세페이지에서 편리한 리스트 관리!**

1. **리스트 이름 변경, 리스트 진행 상태 변경, 매장 삭제, 상품 수정 등**이 가능해요
2. 개인 리스트를 **공유 리스트로 전환**할 수 있어요
3. 공유 리스트 상세페이지에서는 **초대 코드 확인, 나의 권한 및 참여자 목록 확인** 등을 할 수 있어요.
