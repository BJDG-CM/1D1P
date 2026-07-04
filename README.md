# 1D1P — 오늘의 비번 🔑

**매일 아침 9시, 완전 무작위 4자리 코드를 생성해 알림으로 알려주는 오프라인 안드로이드 앱.**

네트워크 호출이 단 한 줄도 없습니다. 코드는 `SecureRandom` 순수 난수로만 생성되고, 저장은 전부
`EncryptedSharedPreferences`(AES-256) 안에서만 이루어집니다.

---

## 주요 기능

| 기능 | 설명 |
|---|---|
| 매일 9시 코드 생성 | `AlarmManager.setExactAndAllowWhileIdle` 정확 알람 → 발화 직후 다음 날 9시 재예약 |
| 순수 난수 | `SecureRandom.nextInt(10000)` — 날짜·이전 코드로부터 유추 불가 (해시 파생 없음) |
| 로컬 알림 | 생성 즉시 "오늘의 코드: 4821" 형태로 표시 (Android 13+ 런타임 권한 처리) |
| 앱 잠금 | 8자리 숫자 비밀번호 — SHA-256 + 랜덤 salt 해시로만 저장, 평문 없음 |
| 코드 내역 | 날짜별로 누적 저장 — 지난 날짜의 코드도 확인·복사 가능 |
| 재부팅 대응 | `BOOT_COMPLETED` 리시버가 알람 자동 재등록 |
| 테스트 버튼 | 설정에서 켜면 9시가 아니어도 즉시 생성+알림 (개발/확인용) |
| 알림 토글 | 끄면 9시에 코드는 생성되지만 알림은 울리지 않음 |

## 화면 구성

```
온보딩(최초 1회, 8자리 비번 설정)
        ↓
잠금 화면(숫자 키패드) ──→ 코드 보기(탭하면 복사)
                              ├─→ 내역(날짜별 코드 목록)
                              └─→ 설정(비번 변경 · 알림/테스트 토글 · 권한 상태)
```

## 기술 스택

- **Kotlin + Jetpack Compose (Material 3)** — XML 레이아웃 없음
- **Gradle 9.6.1 · AGP 9.2.1** (Kotlin 2.4.0은 AGP 내장 — `kotlin.android` 플러그인 불필요)
- **Compose BOM 2026.06.00**
- **androidx.security:security-crypto** — `EncryptedSharedPreferences`
- **JDK 21** / compileSdk·targetSdk **37** / minSdk **26** (Android 8.0+)

## 시작하기

### 요구 사항

- **JDK 21** — Android Studio 내장 JBR로 충분 (별도 설치 불필요)
- **Android SDK** — platform 37 + build-tools (Android Studio가 자동 설치)

### 1. Clone

```bash
git clone https://github.com/BJDG-CM/1D1P.git
cd 1D1P
```

### 2-A. Android Studio로 실행 (권장)

1. Android Studio에서 clone한 폴더 열기 → Gradle 동기화가 끝날 때까지 대기
   (`local.properties`는 Studio가 자동 생성)
2. 기기 연결 또는 에뮬레이터 선택 → **Run ▶**

### 2-B. CLI로 빌드

`local.properties`가 없으므로 SDK 위치를 먼저 지정합니다. 둘 중 하나:

```bash
# 방법 1: 환경변수 (macOS/Linux)
export ANDROID_HOME=$HOME/Library/Android/sdk   # Windows: %LOCALAPPDATA%\Android\Sdk

# 방법 2: 프로젝트 루트에 local.properties 생성
# sdk.dir=C\:\\Users\\<사용자>\\AppData\\Local\\Android\\Sdk   (Windows)
# sdk.dir=/Users/<사용자>/Library/Android/sdk                  (macOS)
```

빌드 & 설치:

```bash
./gradlew assembleDebug                 # APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`
> JDK가 PATH에 없다면: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`

### 3. 테스트

```bash
./gradlew testDebugUnitTest
```

검증 항목: 코드가 항상 4자리·0000~9999 범위인지 / 난수의 비고정성(예측 불가성 최소 검증) /
올바른·틀린 비밀번호 해시 검증 / salt 덕분에 같은 비번도 매번 다른 해시가 나오는지.

## 사용 흐름

1. 첫 실행 → 8자리 숫자 비밀번호 설정 (2회 입력 확인)
2. 알림 권한 허용 (Android 13+)
3. 매일 오전 9시 알림으로 오늘의 코드 수신
4. 알림을 놓쳤다면: 앱 실행 → 비밀번호 입력 → 코드 확인 (탭하면 클립보드 복사)
5. 지난 코드가 필요하면 상단 📜 내역 버튼

**바로 동작을 확인하고 싶다면**: 설정 → "테스트 버튼 표시" ON → 코드 화면의
**지금 테스트 알림 보내기** 버튼을 누르면 9시를 기다릴 필요 없이 생성+알림이 실행됩니다.

## 프로젝트 구조

```
app/src/main/java/com/oneday/onepass/
├── core/                  # 순수 로직 (Android 의존성 없음 → 유닛 테스트 대상)
│   ├── CodeGenerator.kt   #   SecureRandom 4자리 코드 생성
│   └── PasswordHasher.kt  #   SHA-256 + salt 해시/검증 (상수시간 비교)
├── data/
│   └── SecureStore.kt     # EncryptedSharedPreferences — 코드(날짜별)·비번 해시·설정
├── alarm/
│   ├── AlarmScheduler.kt  # 다음 9시 계산 + 정확 알람 등록 (권한 없으면 inexact 폴백)
│   ├── DailyCodeWorker.kt # 생성→저장→알림→재예약 공통 루틴 (9시 알람·테스트 버튼 공용)
│   ├── DailyCodeReceiver.kt
│   └── BootReceiver.kt    # 재부팅 시 알람 재등록
├── notify/
│   └── Notifications.kt   # 알림 채널 + "오늘의 코드: XXXX" 표시
└── ui/
    ├── MainActivity.kt    # 화면 전환 + 권한 요청
    ├── AppViewModel.kt
    ├── components/        # 숫자 키패드, PIN 입력 화면 공통부
    └── screens/           # 온보딩 · 잠금 · 코드 · 내역 · 설정
```

## 보안 설계 메모

- **평문 저장 없음** — 비밀번호는 salt+해시만, 코드는 암호화 prefs 안에만 존재
- **INTERNET 권한 자체가 없음** — 매니페스트에 선언되지 않아 유출 경로 원천 차단
- `allowBackup=false` + 백업 규칙에서 암호화 prefs 명시 제외
- 알림에는 코드가 그대로 노출됨 — 이는 의도된 동작이며, 앱 잠금은 "알림을 놓친 경우"를 위한 보조 장치
- 정확 알람 권한(Android 12+ `SCHEDULE_EXACT_ALARM`)이 없으면 inexact 알람으로 폴백하고, 설정 화면에서 권한 상태 확인·이동 가능

## 라이선스

개인 프로젝트 — 별도 라이선스 미지정.
