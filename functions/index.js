
//  Firebase Cloud Functions 정의 파일

/*
 *  - 원래는 {countries}와 {places}끼리 각각의 컬렉션으로 분리되어있었는데
 *    컬렉션끼리는 비교가 불가능하다고 해서
 *    {countries}와 {places}를 같은 컬렉션 내 문서 단위로 변경하기 위해 Cloud Functions 사용
 * 
 *  - 랭킹 관련 데이터 추가 및 삭제때는 {countries}와 {places}를 각각의 컬렉션으로 분리하는 것이
 *    유저간 동시성 제어에 유리해서 그대로 냅뒀고,
 *    Cloud Functions를 통해 만드는 DB는
 *    서버가 자체적으로 데이터를 변환하는거라 여러 유저가 동시에 접근할 일이 없으며
 *    DB 구조상 랭킹 집계에 최적화되었다는 장점이 있음
 *    -> 쓰기 전용 (기존) / 읽기 전용 (랭킹용) 으로 아예 나누어서 사용하되
 *       두 데이터의 변환에는 Cloud Functions을 사용하자!
 *
 * 1. syncRankingCountries - product_stats의 국가별 totalCount 변경 시 ranking_countries 동기화
 * 2. syncRankingCountryProducts - product_stats의 국가별 상품 count 변경 시 ranking_countries/products 동기화
 * 3. syncRankingPlaces - product_stats의 상점별 상품 count 변경 시 ranking_places/products 동기화
 * 4. migrateRankingData - 기존 product_stats 데이터를 ranking 컬렉션으로 일괄 동기화
 *                         일시적으로 동기화시켜주면 되므로 수동 실행하는 HTTP 함수로 구현
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Firebase Admin SDK 초기화
admin.initializeApp();

// Firestore 데이터베이스 인스턴스
const db = admin.firestore();

/*
 * 국가별 총 상품 수 동기화 함수
 * product_stats/countries/{country}/totalCount 변경 시
 * ranking_countries/{country}/totalCount를 동일한 값으로 업데이트
 */
exports.syncRankingCountries = functions.firestore
  .document('product_stats/countries/{country}/totalCount')
  // .onWrite(async (change, context) ...) = 수정 내역이 있는 문서만 뽑아서 처리!
  // product_stats/countries/{country}/totalCount 경로 단위로 수정 사항 감지
  // - 전체 데이터를 다시 계산하는 것이 아닌 수정된 데이터만 변환하면 됨
  .onWrite(async (change, context) => {
    const country = context.params.country;
    // 생성 및 수정된 문서가 있으면 change.after.exists가 true라서 change.after.data()를 가져오고
    // 삭제된 문서면 수정 후의 문서가 존재하지 않으니 change.after.exists가 false라서 null을 가져옴
    const newData = change.after.exists ? change.after.data() : null;
    
    if (!newData) {
      // 문서가 삭제된 경우 ranking_countries에서도 삭제
      try {
        await db.collection('ranking_countries').doc(country).delete();
      } catch (error) {
        // 에러 무시
      }
      return;
    }
    
    const newCount = newData.count || 0;
    
    // FieldValue.increment()가 원자적 연산이므로 newCount는 항상 최신 값
    // 여러 Cloud Functions가 동시에 실행되느라 데이터가 누락되는 문제는 트랜잭션을 사용함으로써 해결
    // - 동시성 강화
    try {
      await db.runTransaction(async (transaction) => {
        const rankingRef = db.collection('ranking_countries').doc(country);
        
        // 트랜잭션이 동시성을 보장하므로 항상 최신 값으로 업데이트
        transaction.set(rankingRef, {
          totalCount: newCount
        }, { merge: true });
      });
    } catch (error) {
      // 트랜잭션 실패 시 Firestore가 자동으로 재시도
    }
  });

/*
 * 국가별 상품 랭킹 동기화 함수
 * product_stats/countries/{country}/products/{productId}/count 문서 변경 시
 * ranking_countries/{country}/products/{productId}/count를 동일한 값으로 업데이트
 */
exports.syncRankingCountryProducts = functions.firestore
  .document('product_stats/countries/{country}/products/{productId}/count')
  // product_stats/countries/{country}/products/{productId}/count 경로 단위로 수정 사항 감지
  // - 전체 데이터를 다시 계산하는 것이 아닌 수정된 데이터만 변환하면 됨
  .onWrite(async (change, context) => {
    const country = context.params.country;
    const productId = context.params.productId;
    const newData = change.after.exists ? change.after.data() : null;
    const oldData = change.before.exists ? change.before.data() : null;
    
    const newCount = newData?.count || 0;
    const oldCount = oldData?.count || 0;
    
    // count가 변경되지 않았으면 스킵
    if (newCount === oldCount) {
      return;
    }
    
    // FieldValue.increment()가 원자적 연산이므로 newCount는 항상 최신 값
    // 여러 Cloud Functions가 동시에 실행되느라 데이터가 누락되는 문제는 트랜잭션을 사용함으로써 해결
    // - 동시성 강화
    try {
      await db.runTransaction(async (transaction) => {
        const rankingRef = db.collection('ranking_countries')
          .doc(country)
          .collection('products')
          .doc(productId);
        
        if (newCount > 0) {
          transaction.set(rankingRef, {
            count: newCount
          }, { merge: true });
        } else {
          // count가 0이면 문서 삭제
          transaction.delete(rankingRef);
        }
      });
    } catch (error) {
      // 트랜잭션 실패 시 Firestore가 자동으로 재시도
    }
  });

/*
 * 상점별 상품 랭킹 동기화 함수
 * product_stats/places/{placeId}/products/{productId}/count 문서 변경 시
 * ranking_places/{placeId}/products/{productId}/count를 동일한 값으로 업데이트
 */
exports.syncRankingPlaces = functions.firestore
  .document('product_stats/places/{placeId}/products/{productId}/count')
  // product_stats/places/{placeId}/products/{productId}/count 경로 단위로 수정 사항 감지
  // - 전체 데이터를 다시 계산하는 것이 아닌 수정된 데이터만 변환하면 됨
  .onWrite(async (change, context) => {
    const placeId = context.params.placeId;
    const productId = context.params.productId;
    const newData = change.after.exists ? change.after.data() : null;
    const oldData = change.before.exists ? change.before.data() : null;
    
    const newCount = newData?.count || 0;
    const oldCount = oldData?.count || 0;
    
    // count가 변경되지 않았으면 스킵
    if (newCount === oldCount) {
      return;
    }
    
    // FieldValue.increment()가 원자적 연산이므로 newCount는 항상 최신 값
    // 여러 Cloud Functions가 동시에 실행되느라 데이터가 누락되는 문제는 트랜잭션을 사용함으로써 해결
    // - 동시성 강화
    try {
      await db.runTransaction(async (transaction) => {
        const rankingRef = db.collection('ranking_places')
          .doc(placeId)
          .collection('products')
          .doc(productId);
        
        if (newCount > 0) {
          transaction.set(rankingRef, {
            count: newCount
          }, { merge: true });
        } else {
          // count가 0이면 문서 삭제
          transaction.delete(rankingRef);
        }
      });
    } catch (error) {
      // 트랜잭션 실패 시 Firestore가 자동으로 재시도
    }
  });

/*
 * 랭킹 데이터 동기화 함수 (HTTP 함수)
 *                           - 일시적으로 동기화시켜주면 되므로 수동 실행할 수 있는 HTTP 함수로 구현

 * 용도: 기존 product_stats 데이터를 ranking 컬렉션으로 일괄 동기화
 * 실행 방법: HTTP GET 또는 POST 요청
 * URL: https://us-central1-tripcart-10675.cloudfunctions.net/migrateRankingData
 * 
 * ranking_countries: product_stats/countries/{country}/totalCount
 * ranking_countries/{country}/products: product_stats/countries/{country}/products/{productId}/count
 * ranking_places/{placeId}/products: product_stats/places/{placeId}/products/{productId}/count
 */
exports.migrateRankingData = functions.https.onRequest(async (req, res) => {
  try {
    // product_stats/countries/{country}/totalCount,
    // product_stats/countries/{country}/products/{productId}/count를
    // ranking_countries 컬렉션으로 동기화

    // countries에 속해있는 모든 컬렉션 불러오기
    const countriesSnapshot = await db.collection('product_stats')
      .doc('countries')
      .listCollections();
    
    // 동기화된 국가 개수 및 상품 개수 초기화
    let countriesMigrated = 0;
    let countryProductsMigrated = 0;
    
    for (const countryCollection of countriesSnapshot) {
      const country = countryCollection.id;
      
      // totalCount 동기화
      const totalCountDoc = await db.collection('product_stats')
        .doc('countries')
        .collection(country)
        .doc('totalCount')
        .get();
      
      if (totalCountDoc.exists) {
        const totalCount = totalCountDoc.data().count || 0;
        if (totalCount > 0) {
          await db.collection('ranking_countries').doc(country).set({
            totalCount: totalCount
          }, { merge: true }); // 기존 문서가 있으면 덮어쓰기
          countriesMigrated++;
        }
      }
      
      // products 동기화
      const productsCollection = await db.collection('product_stats')
        .doc('countries')
        .collection(country)
        .doc('products')
        .listCollections();
      
      for (const productCollection of productsCollection) {
        const productId = productCollection.id;
        const countDoc = await db.collection('product_stats')
          .doc('countries')
          .collection(country)
          .doc('products')
          .collection(productId)
          .doc('count')
          .get();
        
        if (countDoc.exists) {
          const count = countDoc.data().count || 0;
          if (count > 0) {
            await db.collection('ranking_countries')
              .doc(country)
              .collection('products')
              .doc(productId)
              .set({
                count: count
              }, { merge: true }); // 기존 문서가 있으면 덮어쓰기
            countryProductsMigrated++;
          }
        }
      }
    }
    
    // product_stats/places/{placeId}/products/{productId}/count를
    // ranking_places 컬렉션으로 동기화

    // places에 속해있는 모든 컬렉션 불러오기
    const placesSnapshot = await db.collection('product_stats')
      .doc('places')
      .listCollections();
    
    // 동기화된 상점 개수 및 상품 개수 초기화
    let placesMigrated = 0;
    let placeProductsMigrated = 0;
    
    for (const placeCollection of placesSnapshot) {
      const placeId = placeCollection.id;
      
      // products 동기화
      const productsCollection = await db.collection('product_stats')
        .doc('places')
        .collection(placeId)
        .doc('products')
        .listCollections();
      
      // 상품이 존재하는 상점인지 추적하기 위해 사용
      let hasProducts = false;
      for (const productCollection of productsCollection) {
        const productId = productCollection.id;
        const countDoc = await db.collection('product_stats')
          .doc('places')
          .collection(placeId)
          .doc('products')
          .collection(productId)
          .doc('count')
          .get();
        
        if (countDoc.exists) {
          const count = countDoc.data().count || 0;
          if (count > 0) {
            await db.collection('ranking_places')
              .doc(placeId)
              .collection('products')
              .doc(productId)
              .set({
                count: count
              }, { merge: true }); // 기존 문서가 있으면 덮어쓰기
            placeProductsMigrated++;
            hasProducts = true;
          }
        }
      }
      
      if (hasProducts) {
        placesMigrated++; // 상점에 대한 동기화가 이루어졌음을 표시
      }
    }
    
    const result = {
      success: true,
      message: 'Migration completed',
      // 사실상 아래 값들은 동기화 성공 여부 결과를 표시하기 위해 집계
      summary: {
        countriesMigrated: countriesMigrated,
        countryProductsMigrated: countryProductsMigrated,
        placesMigrated: placesMigrated,
        placeProductsMigrated: placeProductsMigrated
      }
    };
    
    res.status(200).json(result);
    
  } catch (error) {
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/*
 * 채팅 알림 푸시 전송 함수
 * notifications/{userId}/user_notifications/{notificationId} 문서 생성 시
 * 수신자의 FCM 토큰을 이용해 푸시 알림 전송
 */

exports.sendChatNotification = functions.firestore
  .document('notifications/{userId}/user_notifications/{notificationId}')
  .onCreate(async (snap, context) => {
    const notificationData = snap.data();
    const userId = context.params.userId;
    
    // 채팅 알림만 처리
    if (notificationData.type !== 'chat') {
      return null;
    }
    
    // 발신자가 자신인 경우 스킵
    const senderId = notificationData.senderId;
    if (senderId === userId) {
      return null;
    }
    
    try {
      // 사용자의 FCM 토큰 가져오기
      const userDoc = await db.collection('users').doc(userId).get();
      
      if (!userDoc.exists) {
        console.log(`User ${userId} not found`);
        return null;
      }
      
      const fcmToken = userDoc.data()?.fcmToken;
      
      if (!fcmToken) {
        console.log(`FCM token not found for user ${userId}`);
        return null;
      }
      
      // 알림 제목: 리스트 이름
      const title = notificationData.listName || '리스트';
      
      // 알림 본문: {발신자닉네임}: {메시지내용} (간단한 표시용)
      const senderNickname = notificationData.senderNickname || '익명';
      const messageContent = notificationData.message || '';
      const body = `${senderNickname}: ${messageContent}`;
      
      // FCM 메시지 생성
      // - notification payload를 제거하고 data payload만 사용하면
      //   백그라운드에서도 onMessageReceived가 호출되어 커스텀 알림을 표시할 수 있음
      const fcmMessage = {
        token: fcmToken,
        // 백그라운드에서의 커스텀 알림 구현을 위해
        // notification payload 제거 및 data payload만 사용
        data: { // data payload!
          listId: notificationData.listId || '',
          type: 'chat',
          notificationId: context.params.notificationId,
          senderNickname: senderNickname, // 전송자 닉네임
          message: messageContent, // 메시지 내용
          title: title,
          body: body,
          navigateTo: 'list_detail',
          openChat: 'true' // 채팅 팝업 자동 열기
        },
        android: {
          priority: 'high'
        }
      };
      
      // FCM 푸시 전송
      const response = await admin.messaging().send(fcmMessage);
      console.log('Successfully sent message:', response);
      return null;
    } catch (error) {
      console.error('Error sending notification:', error);
      return null;
    }
  });

