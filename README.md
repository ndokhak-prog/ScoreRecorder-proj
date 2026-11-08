# ScoreRecorder-proj
파크골프 스코어기록 카드앱 (N독학러 파크골프)

## 다운로드 (APK)
최신 버전은 [Releases](https://github.com/ndokhak-prog/ScoreRecorder-proj/releases/latest) 에서 받을 수 있습니다.

- 파일: `ndokhak_parkScoreCard.apk`
- 기존 앱이 설치되어 있으면 서명이 달라 업데이트가 되지 않으므로, 앱을 삭제한 뒤 설치하세요.

## 개요
파크골프 라운드의 타수를 기록하고, 기록 카드 이미지를 만들고, 구장을 평가하는 Android 앱입니다.
Kotlin + Jetpack Compose 로 작성되었습니다.

## 주요 기능
- 구장 관리: 구장 정보(지역/홀 수/파/거리) 등록·수정, 검색·정렬, 중복 확인
- 경기 기록: 참가자별 타수 입력, 음성 입력, 실시간 순위 계산
- 기록 카드: 라운드 결과를 이미지로 생성·공유
- 구장평가: 9개 항목 별점(0.5 단위)·총평·특이사항, 구장별 최신 평가 저장 및 목록 표시

## 프로젝트 구조
```
app/src/main/java/com/example/scorerecorder/
  data/      모델 및 저장소 (GolfCourse, ScoreRecord, CourseEvaluation, Storage, ...)
  ui/        Compose 화면 (CourseList, CourseDetail, NewGame, RecordDetail, CourseEvaluation, ...)
app/src/test/java/com/example/scorerecorder/
  단위 테스트 + Robolectric 스크린샷 테스트
requirements/  화면 설계서 및 이미지 리소스
```

## 빌드
요구 사항: JDK 17, Android SDK(compileSdk 34)

```bash
# 디버그 빌드
gradlew.bat assembleDebug

# 테스트
gradlew.bat testDebugUnitTest
```

테스트 실행 시 스크린샷은 `app/build/screenshots/` 에 생성됩니다.

## 릴리즈 서명
서명 키와 비밀번호는 저장소에 커밋하지 않습니다.
로컬에 아래 파일을 준비해야 릴리즈 빌드에 서명이 적용됩니다.

```
keystore/ndokhak-release.keystore
keystore/keystore.properties
```

`keystore.properties` 형식:

```properties
storeFile=ndokhak-release.keystore
storePassword=********
keyAlias=********
keyPassword=********
```

`keystore.properties` 가 없으면 릴리즈 빌드는 서명 없이 생성됩니다.

```bash
gradlew.bat assembleRelease
# app/build/outputs/apk/release/ndokhak_parkScoreCard.apk
```
