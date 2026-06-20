# ShelfView 자체개발 프로젝트
## VS Code AI 에이전트용 개발 실행 계획서

- 문서 버전: 1.0
- 기준일: 2026-06-18
- 프로젝트 가칭: **ShelfView**
- 기반 프로젝트: **Tales 1.24.4 안정 릴리스 포크**
- 대상 운영 환경: **Unraid의 Docker 컨테이너**
- 문서 목적: 사람과 AI 코딩 에이전트가 동일한 범위, 구조, 품질 기준으로 개발하도록 하는 단일 기준 문서

> 2026-06-19 소유자 결정: 현재 구현된 Web-Tales/ShelfView 기능은 충분한
> 수준으로 보고 개발 계획을 종결한다. 남은 Firefox 수동 검증, 상세 성능
> 측정, 공식 v1.0 릴리스 후보 정리는 후속 선택 작업으로 남기며 현재
> 범위의 차단 조건으로 보지 않는다.

> 이 문서는 최초 구현의 기준선이다. 실제 저장소를 조사한 결과와 충돌하면 코드를 억지로 맞추지 말고, 근거를 남긴 ADR(Architecture Decision Record)을 작성한 뒤 계획을 갱신한다.

---

# 0. AI 에이전트에게 가장 먼저 전달할 지시

아래 문장을 VS Code의 AI 채팅에 첫 요청으로 사용한다.

```text
이 저장소의 AGENTS.md, .github/copilot-instructions.md,
docs/ARCHITECTURE.md, docs/BACKLOG.md,
docs/SECURITY_AND_LICENSE.md를 먼저 읽어라.

지금은 기능 구현을 시작하지 말고
.github/prompts/01-repository-audit.prompt.md의 절차로 저장소를 조사하라.
조사 결과를 docs/AI_PROGRESS.md에 기록하고,
현재 코드에서 재사용할 수 있는 스캐너, 미디어 분석기, 페이지 API,
읽기 진행률, 사용자 권한, Vue 리더 구성요소의 실제 경로를 제시하라.
추측한 경로나 존재하지 않는 API를 만들지 마라.
```

AI가 저장소 조사를 완료한 뒤에는 백로그의 **SV-000부터 한 티켓씩** 진행한다. 한 번의 작업에서 여러 마일스톤을 동시에 구현하지 않는다.

---

# 1. 프로젝트 목표

ShelfView는 Unraid 서버의 도서·만화 파일을 브라우저에서 읽는 자체 호스팅 웹 애플리케이션이다. 기존 Tales의 검증된 서버 기능을 유지하면서 다음 기능을 확장한다.

1. PDF, TXT, ZIP/CBZ, JPG·PNG·WebP 이미지 폴더와 ZIP 내부의 TXT·PDF·JPG·GIF 콘텐츠를 하나의 도서관에서 관리한다.
2. 만화 보기와 텍스트 책 보기를 하나의 일관된 리더 UI에서 제공한다.
3. 단면, 양면, 연속 스크롤, 웹툰, 좌→우, 우→좌 모드를 제공한다.
4. 밝기, 대비, 흑백, 세피아, 반전, 저조도, OLED 검정 테마를 제공한다.
5. 사용자별 마지막 읽은 위치, 여러 개의 책갈피, 뷰어 설정을 서버에 동기화한다.
6. 원본 미디어는 읽기 전용으로 유지하며 Docker로 배포한다.
7. 업스트림 Tales의 보안 수정과 기능을 지속해서 병합할 수 있는 구조를 유지한다.

## 1.1 성공 기준

MVP는 다음 시나리오가 모두 동작하면 완료로 본다.

- Unraid 공유 폴더의 CBZ/ZIP/PDF가 기존 Tales 기능을 잃지 않고 열린다.
- UTF-8 및 한국어 레거시 인코딩 TXT가 안전하게 열리고, 테마·글꼴·행간을 변경할 수 있다.
- ZIP 내부에 포함된 TXT, PDF, JPG, GIF 항목도 안전하게 읽을 수 있다.
- 명시적으로 표시된 이미지 폴더가 한 권의 책으로 스캔되고 자연 정렬된 순서로 읽힌다.
- 모든 지원 형식에서 마지막 위치가 복원된다.
- 한 권에 여러 책갈피를 추가·이름 변경·삭제·이동할 수 있다.
- 설정이 사용자별로 저장되고 다른 브라우저에서도 복원된다.
- 미디어 폴더를 Docker에 읽기 전용으로 마운트해도 모든 읽기 기능이 작동한다.
- 인증되지 않은 사용자는 콘텐츠·페이지·텍스트·책갈피 API에 접근할 수 없다.
- 기존 Tales 리더로 되돌아갈 수 있는 기능 플래그 또는 폴백 경로가 존재한다.

## 1.2 MVP에서 제외할 것

다음 항목은 최초 릴리스의 필수 범위가 아니다.

- OCR, 번역, TTS, AI 요약
- 서버 측 AI 업스케일링과 고비용 노이즈 제거
- DRM 전자책
- EPUB 렌더러 교체
- 모바일 네이티브 앱
- 클라우드 동기화 서비스
- 편집기 또는 도서 제작 기능
- Tales 전체를 Vue 3, Spring 최신 구조 등으로 일괄 마이그레이션
- 사용자 업로드를 인터넷에 공개하는 멀티테넌트 SaaS

---

# 2. 기준선과 핵심 기술 결정

## 2.1 Tales를 전체 서버 기반으로 포크한다

Tales는 MIT 라이선스이며 웹 UI, 사용자 관리, 라이브러리 스캔, 메타데이터, 웹 리더, REST API, OPDS, 읽기 상태 등 필요한 기반을 이미 제공한다. 프로젝트는 현재 `tales` Spring Boot 백엔드, `tales-webui` Vue 프런트엔드, `tales-tray`로 나뉜다.

기준 태그는 문서 작성일 현재 안정 릴리스인 `1.24.4`로 고정한다. `master`의 최신 커밋에서 바로 개발을 시작하지 않는다.

```bash
git clone https://github.com/gotson/tales.git shelfview
cd shelfview

git remote rename origin upstream
git remote add origin <내-저장소-주소>

git fetch upstream --tags
git switch --create shelfview-main 1.24.4
git push --set-upstream origin shelfview-main
```

브랜치 정책:

- `shelfview-main`: 배포 가능한 통합 브랜치
- `upstream-sync/<version>`: 업스트림 병합 전용
- `feat/SV-xxx-설명`: 기능 티켓
- `fix/SV-xxx-설명`: 버그 티켓
- `docs/SV-xxx-설명`: 문서 티켓

`upstream/master`를 임의로 리베이스하지 않는다. 업스트림 반영은 별도 브랜치에서 병합·테스트한 뒤 PR로 가져온다.

## 2.2 현재 스택을 존중한다

최초 기준선에서 사용하는 스택은 다음과 같다.

| 영역 | 기준 |
|---|---|
| 백엔드 | Kotlin, Spring Boot, Gradle, jOOQ, Flyway |
| 프런트엔드 | Vue 2.6, TypeScript 4.9, Vuex 3, Vuetify 2, Vue CLI 5 |
| 개발 요구사항 | JDK 21+, Node.js 18+ |
| 배포 | 단일 Docker 이미지, 기본 포트 25600 |
| 저장소 | Tales의 기존 DB·마이그레이션 체계 유지 |

**중요:** MVP 작업과 프레임워크 현대화를 섞지 않는다. Vue 3 전환, Pinia 전환, Vite 전환, 대규모 패키지 갱신은 별도 프로젝트다.

## 2.3 확장 코드를 격리한다

업스트림 병합 충돌을 줄이기 위해 ShelfView 전용 코드는 가능한 한 격리한다.

권장 구조는 실제 저장소 조사 후 확정하되 방향은 다음과 같다.

```text
tales/
└── src/main/kotlin/org/tales/
    └── shelfview/
        ├── api/
        ├── domain/
        ├── infrastructure/
        ├── scanner/
        └── service/

tales-webui/src/
└── shelfview/
    ├── adapters/
    ├── components/
    ├── models/
    ├── services/
    ├── store/
    ├── styles/
    └── views/
```

기존 Tales 클래스에 반드시 손을 대야 할 때는 다음 순서를 따른다.

1. 확장 포인트나 기존 인터페이스를 먼저 찾는다.
2. 작은 어댑터 또는 이벤트 구독으로 해결 가능한지 확인한다.
3. 불가능할 때만 최소한의 훅을 추가한다.
4. 변경 이유와 업스트림 충돌 가능성을 ADR에 기록한다.

## 2.4 API 네임스페이스를 분리한다

모든 신규 API는 다음 경로 아래에 둔다.

```text
/api/shelfview/v1
```

기존 `/api/v1` 응답 형식을 임의로 변경하지 않는다. 기존 클라이언트와 OPDS의 호환성을 우선한다.

## 2.5 새 리더를 단계적으로 도입한다

기존 리더를 즉시 삭제하지 않는다.

- 새 라우트 예: `/read-v2/:bookId`
- 사용자 또는 서버 기능 플래그로 활성화
- 오류 발생 시 기존 리더로 이동할 수 있는 메뉴 제공
- 안정화 후 기본 리더를 새 리더로 전환

---

# 3. 오픈소스 재사용과 라이선스 정책

## 3.1 기본 정책

ShelfView의 신규 코드는 **MIT 호환 공개를 염두에 둔 독자 구현**을 기본값으로 한다. 프로젝트 소유자가 다른 라이선스를 선택하기 전까지 다음 규칙을 지킨다.

- Tales의 MIT 라이선스와 저작권 고지를 보존한다.
- Apache-2.0, MIT, BSD 계열 라이브러리는 조건을 확인하고 의존성으로 사용한다.
- GPL/AGPL 코드는 기본 구현에 복사·붙여넣기하지 않는다.
- 다른 프로젝트의 UI 흐름, 공개 기능 아이디어, 데이터 모델 개념은 참고할 수 있다.
- 코드를 직접 가져왔다면 파일 단위 출처, 커밋, 라이선스, 수정 내용을 기록한다.
- AI가 생성한 코드라도 특정 오픈소스 파일과 비정상적으로 유사하면 재작성한다.

이 문서는 법률 자문이 아니다. 외부 공개나 상용 배포 전에는 최종 라이선스 검토가 필요하다.

## 3.2 소스별 활용 결정

| 프로젝트 | 라이선스 | 기본 활용 방침 |
|---|---|---|
| Tales | MIT | 전체 포크, 서버·UI 기반으로 사용 |
| PDF.js | Apache-2.0 | 2단계 PDF 텍스트 레이어·검색에 패키지 의존성으로 검토 |
| zip.js | BSD-3-Clause | 로컬 파일 열기 기능이 필요할 때 선택적으로 사용 |
| foliate-js | MIT | TXT 페이지네이션 연구용; 특정 커밋 고정 후 제한적으로 검토 |
| Readium Web | BSD-3-Clause | Publication/Locator 개념 참고 |
| Koodo Reader/Kookit | AGPL-3.0 | 기능·UX 참고만; 코드 복사 금지 |
| Kavita | GPL-3.0 | 기능·운영 흐름 참고만; 코드 복사 금지 |
| BookLore | AGPL-3.0 | 기능·UX 참고만; 코드 복사 금지 |

## 3.3 PDF 구현 결정

MVP에서는 PDF 파일도 Tales가 제공하는 페이지 스트림을 이용해 **고정 페이지 시퀀스**로 읽는다. 이 방법은 새 리더의 단면·양면·연속 스크롤·필터·책갈피를 빠르게 통일한다.

PDF.js는 다음 요구가 생기는 후속 단계에서만 도입한다.

- PDF 텍스트 선택
- 문서 내 검색
- 링크·목차
- 접근성 텍스트 레이어
- 원본 PDF의 직접 Range 렌더링

PDF.js를 도입할 때는 현재 Vue 2 빌드 체계와 웹 워커 구성을 별도 스파이크로 검증한다.

## 3.4 ZIP 처리 결정

서버형 뷰어에서는 브라우저가 대용량 ZIP 전체를 내려받아 해제하지 않는다. 기존 Tales의 분석·페이지 제공 경로를 재사용한다.

후속 확장에서는 ZIP 내부에 TXT, PDF, JPG, GIF가 섞여 있는 도서도 지원한다. 이 기능은 단순 이미지 ZIP과 달리 텍스트 리더와 이미지/PDF 리더가 한 압축파일 안에서 만나는 구조이므로, 구현 전에 별도 ADR로 Manifest 형태, 읽기 순서, GIF/PDF 처리 방식을 결정한다.

zip.js는 다음 기능에만 선택적으로 사용한다.

- 브라우저에서 로컬 파일을 서버 업로드 없이 임시 열기
- 관리자의 사전 검사 도구
- 향후 오프라인 PWA

---

# 4. 전체 아키텍처

```text
[브라우저 / 모바일 웹]
        |
        | HTTPS + Session/API Key
        v
[Tales Web UI + ShelfView Unified Reader]
        |
        | /api/v1                  기존 API
        | /api/shelfview/v1        신규 API
        v
[Spring Boot / Kotlin]
  +-------------------------------+
  | 기존 Tales 서비스             |
  | - 사용자/권한                 |
  | - 라이브러리/스캔             |
  | - 미디어 분석                 |
  | - 페이지 스트림               |
  | - 기존 진행률                 |
  +-------------------------------+
  | ShelfView 확장                |
  | - 통합 Publication Manifest   |
  | - TXT 처리                    |
  | - 이미지 폴더 가상 도서       |
  | - 확장 Locator                |
  | - 다중 책갈피                 |
  | - 사용자 뷰어 설정            |
  +-------------------------------+
        |
        +------> [SQLite / config]
        |
        +------> [/cache 썸네일·분석 캐시]
        |
        +------> [/data 원본 미디어, read-only]
```

## 4.1 통합 Publication 모델

프런트엔드는 파일 확장자를 직접 분기하지 않고 서버의 Manifest를 받는다.

```ts
export type PublicationKind =
  | 'FIXED_PAGE_SEQUENCE'
  | 'TEXT_REFLOW'

export type SourceFormat =
  | 'CBZ'
  | 'ZIP'
  | 'PDF'
  | 'IMAGE_DIRECTORY'
  | 'TXT'

export interface ShelfViewManifest {
  schemaVersion: 1
  bookId: string
  title: string
  kind: PublicationKind
  sourceFormat: SourceFormat
  contentFingerprint: string
  pageCount?: number
  text?: {
    endpoint: string
    detectedEncoding?: string
    normalizationVersion: number
  }
  pages?: {
    endpointTemplate: string
    thumbnailEndpoint?: string
  }
  capabilities: {
    search: boolean
    textSelection: boolean
    doublePage: boolean
    continuousScroll: boolean
    rtl: boolean
    imageFilters: boolean
  }
}
```

`contentFingerprint`가 달라지면 파일 내용이 바뀐 것으로 판단한다. 기존 Locator를 복원할 수 없는 경우 사용자에게 알리고 가장 가까운 위치로 이동한다.

## 4.2 리더 어댑터

```ts
export interface ReaderAdapter {
  open(manifest: ShelfViewManifest): Promise<void>
  next(): Promise<void>
  previous(): Promise<void>
  goTo(locator: Locator): Promise<void>
  getCurrentLocator(): Locator
  getProgression(): number
  destroy(): void
}
```

초기 구현:

- `FixedPageAdapter`: CBZ, ZIP, PDF, 이미지 폴더
- `TextReflowAdapter`: TXT

UI는 어댑터의 공통 인터페이스만 사용한다.

---

# 5. 파일 형식별 설계

## 5.1 CBZ·ZIP

기존 Tales의 스캔, 분석, 페이지 목록, 페이지 이미지 API를 최대한 재사용한다.

필수 동작:

- 자연 정렬: `1.jpg`, `2.jpg`, `10.jpg`
- 압축 내부의 불필요한 폴더 엔트리 무시
- 현재 페이지 중심의 제한적 프리페치
- 원본 아카이브 전체를 브라우저로 자동 전송하지 않음
- 비정상 아카이브는 사용자 친화적인 오류로 처리

후속 ZIP 내부 혼합 콘텐츠 지원:

- ZIP 내부의 `.txt`, `.pdf`, `.jpg`, `.jpeg`, `.gif` 항목을 도서 콘텐츠로 인식
- TXT 항목은 ShelfView TXT 콘텐츠 API와 같은 인코딩·크기·줄 길이 제한 적용
- PDF 항목은 PDF.js 없이 기존 PDF 페이지 이미지 흐름을 재사용하되, 안전한 단일 엔트리 캐시 정책은 ADR에서 결정
- JPG/GIF 항목은 고정 페이지 이미지 흐름으로 제공하되, GIF 애니메이션 유지 여부는 ADR에서 결정
- 압축 경로 탈출, 절대 경로, 중첩 아카이브, 과도한 엔트리 수와 해제 크기 제한 검증
- Manifest와 리더 어댑터가 혼합 TXT/이미지 순서를 안정적으로 표현하는지 검증

## 5.2 PDF

MVP에서는 페이지 이미지로 통합한다.

필수 동작:

- 단면·양면·연속 스크롤
- 페이지 맞춤: 너비, 높이, 원본 크기
- 회전
- 페이지 번호 이동
- 이미지 필터
- 페이지 단위 책갈피

후속 PDF.js 단계는 별도 ADR과 기술 스파이크를 거친다.

## 5.3 TXT

TXT는 화면 크기와 글꼴 설정에 따라 페이지 수가 바뀐다. 따라서 책갈피를 화면 페이지 번호로 저장하지 않는다.

처리 흐름:

1. 서버는 파일 접근 권한을 확인한다.
2. BOM과 바이트 패턴으로 인코딩 후보를 제공한다.
3. 브라우저 또는 서버가 `utf-8`, `euc-kr` 등의 선택값으로 디코딩한다.
4. CRLF/LF, 연속 공백, 문단 경계를 정규화한다.
5. 텍스트 노드로 안전하게 렌더링한다.
6. 페이지형과 스크롤형을 제공한다.
7. 문단 인덱스와 문자 오프셋으로 위치를 저장한다.

보안 규칙:

- TXT 내용을 신뢰하지 않는다.
- 원문을 `v-html`로 직접 삽입하지 않는다.
- 링크 자동 변환은 초기 버전에서 하지 않는다.
- 렌더링 전 최대 파일 크기와 최대 줄 길이를 제한한다.
- 인코딩 선택 변경 시 Locator 복원 정확성을 검증한다.

권장 Locator:

```json
{
  "schemaVersion": 1,
  "kind": "TEXT",
  "paragraph": 152,
  "characterOffset": 87,
  "progression": 0.2384,
  "normalizationVersion": 1,
  "contentFingerprint": "sha256:..."
}
```

TXT 설정:

- 글꼴, 글자 크기, 행간, 문단 간격
- 본문 폭과 화면 여백
- 왼쪽 정렬·양쪽 정렬
- 페이지형·스크롤형
- 한 단·두 단
- Light, Paper, Sepia, Dark, OLED 테마

## 5.4 JPG·PNG·WebP 이미지 폴더

Tales의 일반 규칙과 충돌하지 않도록 이미지 폴더를 자동 추측하지 않는다. MVP는 명시적 마커 파일을 사용한다.

```text
/data/images/작품명/1권/
├── .shelfview-book
├── 001.jpg
├── 002.jpg
└── 010.webp
```

규칙:

- `.shelfview-book`가 있는 **말단 폴더**만 한 권으로 처리한다.
- 상위 폴더는 시리즈로 취급한다.
- 이미지 파일명은 자연 정렬한다.
- 중첩 하위 폴더는 기본적으로 오류로 표시한다.
- 지원 확장자는 서버가 실제 디코딩 가능한 형식과 일치시킨다.
- 마커 기능은 라이브러리 설정에서 기본 비활성화한다.
- 마커가 제거되었을 때 기존 책·책갈피 처리 정책을 테스트한다.

후속 버전에서만 “말단 폴더 자동 감지” 옵션을 검토한다.

---

# 6. 읽기 위치, 책갈피, 설정

## 6.1 Locator 모델

```ts
export type PageLocator = {
  schemaVersion: 1
  kind: 'PAGE'
  page: number
  offset: number
  contentFingerprint: string
}

export type TextLocator = {
  schemaVersion: 1
  kind: 'TEXT'
  paragraph: number
  characterOffset: number
  progression: number
  normalizationVersion: number
  contentFingerprint: string
}

export type Locator = PageLocator | TextLocator
```

페이지는 내부적으로 0부터 시작한다. UI에는 1부터 표시한다. API 문서와 테스트에 이 차이를 명시한다.

## 6.2 데이터 저장 원칙

- 기존 고정 페이지 형식은 Tales의 기존 읽기 진행률과 호환성을 유지한다.
- TXT와 확장 Locator는 ShelfView 테이블에 저장한다.
- 책갈피와 마지막 위치를 분리한다.
- 뷰어 설정은 전역 → 라이브러리 → 시리즈 → 책 순서로 덮어쓴다.
- 마이그레이션은 기존 Flyway 명명·실행 방식을 따른다.
- 새 마이그레이션 번호를 만들기 전에 현재 저장소의 가장 높은 버전을 확인한다.

개념 스키마:

```sql
CREATE TABLE shelfview_reader_position (
  user_id              TEXT NOT NULL,
  book_id              TEXT NOT NULL,
  locator_json         TEXT NOT NULL,
  progression          REAL NOT NULL,
  content_fingerprint  TEXT NOT NULL,
  updated_at           TEXT NOT NULL,
  PRIMARY KEY (user_id, book_id)
);

CREATE TABLE shelfview_bookmark (
  id                   TEXT PRIMARY KEY,
  user_id              TEXT NOT NULL,
  book_id              TEXT NOT NULL,
  locator_json         TEXT NOT NULL,
  label                TEXT,
  note                 TEXT,
  created_at           TEXT NOT NULL,
  updated_at           TEXT NOT NULL
);

CREATE INDEX idx_shelfview_bookmark_user_book
ON shelfview_bookmark(user_id, book_id);

CREATE TABLE shelfview_reader_preference (
  user_id              TEXT NOT NULL,
  scope_type           TEXT NOT NULL,
  scope_id             TEXT NOT NULL,
  format               TEXT NOT NULL,
  settings_json        TEXT NOT NULL,
  updated_at           TEXT NOT NULL,
  PRIMARY KEY (user_id, scope_type, scope_id, format)
);
```

실제 컬럼 타입, 외래키, 시간 타입, ID 생성법은 Tales의 기존 jOOQ/Flyway 패턴을 조사한 뒤 맞춘다.

## 6.3 저장 타이밍

- 위치 변경 후 약 2초 debounce
- 탭이 숨겨질 때 flush
- `pagehide` 시 flush
- 라우트 이동·컴포넌트 제거 시 flush
- 저장 실패 시 비차단 경고와 재시도
- 네트워크 재연결 시 최신 위치 충돌 정책 적용

충돌 기본 정책은 서버 `updatedAt` 기준 마지막 쓰기 우선이다. 향후 기기별 세션이 필요하면 별도 ADR을 작성한다.

---

# 7. API 초안

## 7.1 Publication

```http
GET /api/shelfview/v1/books/{bookId}/manifest
```

응답은 4.1의 `ShelfViewManifest`를 따른다.

## 7.2 TXT

```http
GET /api/shelfview/v1/books/{bookId}/text
    ?encoding=auto|utf-8|euc-kr
```

가능하면 전체 파일을 무조건 메모리에 올리지 않는다. 대용량 TXT 정책은 기술 스파이크에서 결정한다.

## 7.3 읽기 위치

```http
GET /api/shelfview/v1/books/{bookId}/position
PUT /api/shelfview/v1/books/{bookId}/position
```

## 7.4 책갈피

```http
GET    /api/shelfview/v1/books/{bookId}/bookmarks
POST   /api/shelfview/v1/books/{bookId}/bookmarks
PUT    /api/shelfview/v1/bookmarks/{bookmarkId}
DELETE /api/shelfview/v1/bookmarks/{bookmarkId}
```

## 7.5 뷰어 설정

```http
GET /api/shelfview/v1/preferences
PUT /api/shelfview/v1/preferences
```

쿼리 또는 본문에 `scopeType`, `scopeId`, `format`을 포함한다.

## 7.6 공통 API 규칙

- 모든 요청에서 기존 Tales 인증과 책 접근 권한을 재사용한다.
- 사용자가 해당 책을 볼 수 없는 경우 존재 여부를 과도하게 노출하지 않는다.
- DTO와 DB 모델을 분리한다.
- 오류 응답은 기존 Tales 형식을 따르되 ShelfView 오류 코드를 부여한다.
- Locator JSON은 서버에서 스키마 검증한다.
- 잘못된 페이지 인덱스, 인코딩, scope 값을 거부한다.
- OpenAPI/Swagger 문서에 신규 API를 노출한다.

---

# 8. 프런트엔드 설계

## 8.1 구성요소

```text
UnifiedReaderView.vue
├── ReaderTopBar.vue
├── FixedPageReader.vue
├── TextReader.vue
├── ReaderSettingsDrawer.vue
├── BookmarkDrawer.vue
├── PageNavigator.vue
├── ReaderErrorState.vue
└── ReaderLoadingState.vue
```

실제 파일명은 기존 Tales 명명 규칙을 조사해 맞춘다.

## 8.2 상태

Vuex 모듈의 개념 상태:

```ts
interface ShelfViewReaderState {
  manifest: ShelfViewManifest | null
  locator: Locator | null
  progression: number
  bookmarks: Bookmark[]
  effectivePreferences: ReaderPreferences | null
  loading: boolean
  saving: boolean
  error: ReaderError | null
}
```

원칙:

- 서버 상태와 일시 UI 상태를 구분한다.
- 컴포넌트가 API를 직접 호출하지 않고 서비스 계층을 통한다.
- 기존 Axios 래퍼와 오류 처리 패턴을 재사용한다.
- Vue 2 프로젝트 안에서 임의로 Composition API를 추가하지 않는다.
- 기존 i18n 체계에 한국어·영어 키를 추가한다.

## 8.3 고정 페이지 리더

기능:

- 한 쪽 / 두 쪽
- 연속 스크롤 / 웹툰
- LTR / RTL
- 첫 페이지 단독 표시
- 폭 맞춤 / 높이 맞춤 / 실제 크기
- 확대·축소·회전
- 키보드, 마우스 휠, 터치 스와이프
- 현재 페이지와 근처 페이지만 유지하는 가상화
- 로딩 실패 페이지 재시도

## 8.4 시력 보호와 이미지 필터

필터는 메뉴와 툴바가 아닌 **콘텐츠 요소에만** 적용한다.

```css
.shelfview-page-content {
  filter:
    brightness(var(--sv-brightness, 1))
    contrast(var(--sv-contrast, 1))
    grayscale(var(--sv-grayscale, 0))
    sepia(var(--sv-sepia, 0))
    invert(var(--sv-invert, 0));
}
```

초기 프리셋:

| 프리셋 | 목적 |
|---|---|
| 기본 | 원본 표시 |
| 눈의 피로 감소 | 밝기 감소, 약한 세피아 |
| 흑백 만화 | 흑백 + 대비 상승 |
| 저조도 | 밝기·대비 감소 |
| 야간 반전 | 페이지 반전 + 밝기 감소 |
| OLED 텍스트 | 완전 검정 배경 + 밝은 글자 |

선명화, 배경 제거, 적응형 이진화는 WebGL/Canvas 성능 스파이크 후 별도 마일스톤으로 둔다.

## 8.5 접근성과 입력

- 주요 기능은 키보드만으로 조작 가능해야 한다.
- 단축키 목록을 화면에서 확인할 수 있어야 한다.
- 포커스가 입력창에 있을 때 페이지 단축키를 가로채지 않는다.
- 버튼은 접근 가능한 이름을 가진다.
- 테마는 충분한 대비를 제공한다.
- `prefers-reduced-motion`을 존중한다.
- 모바일 세로·가로 전환 시 Locator를 유지한다.

---

# 9. 백엔드 설계 원칙

## 9.1 기존 패턴 우선

AI는 새 프레임워크나 저장소 패턴을 도입하기 전에 다음을 찾아야 한다.

- Controller/Router 구조
- 권한 검사 방법
- 도메인 서비스와 이벤트
- Repository/jOOQ 사용법
- Flyway 마이그레이션 위치와 버전 규칙
- 미디어 스캐너 확장점
- 페이지 분석·썸네일 캐시
- 기존 읽기 진행률 API
- 테스트 픽스처와 임시 라이브러리 생성법

조사 결과가 나오기 전에는 추측한 패키지에 코드를 생성하지 않는다.

## 9.2 이미지 폴더 분석기

개념 단계:

1. 라이브러리 설정에서 `imageDirectoryBooks` 활성화 확인
2. `.shelfview-book` 마커 확인
3. 허용 이미지 목록 수집
4. 자연 정렬
5. 이미지 헤더·해상도·MIME 검사
6. content fingerprint 계산
7. 가상 도서 메타데이터 생성
8. 표지와 페이지 정보 캐시
9. 삭제·이동·변경 이벤트 처리

스캐너의 기존 “폴더=시리즈, 파일=책” 모델과 충돌하는 지점을 먼저 문서화한다. 구현이 기존 모델을 과도하게 침범하면 마커 폴더를 서버 캐시의 가상 CBZ 표현으로 추상화하는 대안을 비교한다.

## 9.3 인코딩 처리

인코딩 자동 감지는 확정값이 아니라 후보다.

- BOM이 있으면 우선한다.
- UTF-8 유효성을 검사한다.
- 실패 시 `euc-kr` 후보를 제시한다.
- 사용자가 수동 선택을 저장할 수 있게 한다.
- 인코딩 선택은 책별 설정으로 저장한다.
- 디코딩 오류 개수 또는 대체 문자 비율을 UI에 표시할 수 있다.

## 9.4 캐시

- 원본 `/data`에 파생 파일을 쓰지 않는다.
- 표지, 페이지 정보, 이미지 폴더 분석 결과는 `/cache` 또는 기존 Tales 캐시 체계를 사용한다.
- 캐시는 언제든 재생성할 수 있어야 한다.
- 캐시 키에 파일 수정 시각만 의존하지 말고 크기·fingerprint 정책을 검토한다.

---

# 10. 보안 요구사항

보안 항목은 기능 완료 후 한 번에 추가하는 작업이 아니다. 각 티켓의 완료 조건에 포함한다.

## 10.1 공통

- 모든 신규 API에 인증·권한 검사를 적용한다.
- 서버의 실제 파일 경로를 API 응답과 로그에 노출하지 않는다.
- 사용자 입력을 로그에 그대로 남기지 않는다.
- 오류에 스택 트레이스를 브라우저로 반환하지 않는다.
- 원본 미디어 볼륨은 읽기 전용으로 마운트한다.
- 관리자 설정 변경은 일반 사용자가 실행할 수 없다.

## 10.2 아카이브

- `../` 경로, 절대 경로, 드라이브 문자 경로 차단
- 심볼릭 링크 엔트리 차단
- 최대 엔트리 수 제한
- 단일 엔트리 및 총 압축 해제 예상 크기 제한
- 비정상 압축률 제한
- 중첩 아카이브 기본 차단
- 이미지 최대 픽셀 수 제한
- 파싱·디코딩 타임아웃
- 확장자 외 실제 형식 검사

제한값은 임의로 하드코딩하지 말고 설정 객체와 테스트로 관리한다.

## 10.3 TXT·웹 렌더링

- HTML 이스케이프
- 스크립트 실행 불가
- `v-html` 사용 금지, 불가피하면 검증된 sanitizer와 ADR 필요
- 매우 긴 줄과 매우 큰 파일에 대한 메모리 보호
- CSS 삽입과 URL 자동 링크 방지

## 10.4 공급망

- lockfile을 커밋한다.
- 의존성 추가 이유와 라이선스를 PR에 기록한다.
- 불필요한 신규 패키지를 추가하지 않는다.
- CI에서 비밀키·토큰을 이미지에 포함하지 않는다.
- 업스트림 Tales 보안 공지를 주기적으로 확인한다.

---

# 11. 테스트 전략

## 11.1 테스트 피라미드

1. 도메인 단위 테스트
2. Repository·API 통합 테스트
3. Vue 컴포넌트·스토어 단위 테스트
4. 소수의 브라우저 E2E 시나리오
5. 수동 대용량·모바일·Unraid 검증

## 11.2 필수 샘플 세트

저작권 문제가 없는 작은 테스트 픽스처를 저장소에 둔다.

```text
test-fixtures/
├── pdf/
│   ├── text-3pages.pdf
│   └── image-only-3pages.pdf
├── txt/
│   ├── utf8.txt
│   ├── utf8-bom.txt
│   ├── euc-kr.txt
│   ├── crlf.txt
│   └── very-long-line.txt
├── archives/
│   ├── natural-sort.cbz
│   ├── nested-folder.zip
│   ├── corrupt.zip
│   └── traversal.zip
└── image-directory/
    ├── valid-book/
    ├── no-marker/
    ├── empty-marker-book/
    └── nested-directory/
```

악성 픽스처는 실제 위험한 크기로 만들지 않고, 제한 로직을 재현하는 최소 구조로 만든다.

## 11.3 형식별 핵심 테스트

### 고정 페이지

- 1, 2, 10 자연 정렬
- LTR/RTL
- 두 쪽 보기의 홀수·짝수 시작
- 회전 후 Locator 유지
- 스크롤과 페이지 모드 전환 후 위치 보존
- 필터가 UI에 적용되지 않음

### TXT

- UTF-8, BOM, euc-kr 디코딩
- 개행 정규화
- XSS 문자열이 텍스트로 표시됨
- 글꼴·폭 변경 후 같은 문단·문자 위치 복원
- 인코딩 변경 후 오류 처리
- 대용량·긴 줄 제한

### 책갈피

- 사용자 간 격리
- 접근 권한이 없는 책의 책갈피 조회 차단
- 여러 책갈피 순서와 수정
- 파일 내용 변경 시 fingerprint 불일치 처리
- 삭제된 책에 대한 정리 정책

## 11.4 회귀 테스트 명령

실제 명령은 저장소 조사 후 `docs/DEVELOPMENT.md`에 확정한다. 현재 기준 후보:

```bash
# 백엔드
./gradlew test

# 프런트엔드
cd tales-webui
npm run lint
npm run test:unit
npm run build

# 통합 정적 자산
cd ..
./gradlew prepareThymeLeaf
```

AI는 실행하지 않은 명령을 “통과”라고 쓰지 않는다. 실패하면 실패 원인과 미실행 범위를 보고한다.

---

# 12. 성능 원칙

절대 목표값은 Unraid 하드웨어에 따라 달라지므로 먼저 기준선을 측정한다.

필수 원칙:

- 아카이브 전체를 브라우저 메모리에 자동 적재하지 않는다.
- 현재 페이지 주변만 프리페치한다.
- 연속 스크롤에서는 화면 밖 페이지를 가상화한다.
- 이미지 디코딩 실패가 전체 리더를 중단하지 않게 한다.
- TXT는 파일 크기 제한과 점진적 처리 방안을 가진다.
- Manifest는 분석 완료 후 캐시 가능해야 한다.
- 필터 변경으로 원본을 다시 다운로드하지 않는다.
- 모바일에서 두 쪽 보기와 과도한 프리페치를 자동 줄일 수 있다.

측정 항목:

- 첫 페이지 표시 시간
- 50, 500, 2,000페이지 도서의 메모리
- 100MB·1GB 아카이브의 서버·브라우저 동작
- 10MB·100MB TXT 처리
- 빠른 페이지 넘김 중 취소되지 않은 요청 수
- 스캔 전후 CPU·메모리·디스크 I/O

---

# 13. Docker와 Unraid 배포

## 13.1 볼륨

| 컨테이너 경로 | 권한 | 용도 |
|---|---|---|
| `/config` | 읽기/쓰기 | DB, 설정, 영구 상태 |
| `/cache` | 읽기/쓰기 | 재생성 가능한 파생 데이터 |
| `/data` | 읽기 전용 | 원본 도서·만화 |

Unraid에서는 `/config`와 `/cache`를 appdata 전용 풀에 두는 것을 권장한다. 배포 문서에는 백업·복구와 권한 점검 절차를 포함한다.

예시 Compose는 스타터 팩의 `docker-compose.unraid.example.yml`을 사용한다.

## 13.2 이미지

- 개발 중: 로컬 태그
- 릴리스: `ghcr.io/<owner>/shelfview:<version>`
- `latest`만 사용하지 않고 버전 태그 제공
- 가능하면 `linux/amd64`, `linux/arm64` 빌드
- 빌드 정보, Tales 기준 태그, Git SHA를 About 화면과 로그에 표시

## 13.3 운영

- 기본 포트 25600 유지
- 외부 공개 시 리버스 프록시와 HTTPS
- `/data` 읽기 전용
- 정기 DB 백업
- 업그레이드 전 백업 및 마이그레이션 롤백 안내
- 건강 상태와 로그에 민감 정보 미포함

---

# 14. AI 코딩 작업 규칙

이 절이 바이브 코딩의 핵심이다.

## 14.1 한 번에 한 수직 슬라이스

각 작업은 사용자 가치가 확인되는 가장 작은 수직 슬라이스로 제한한다.

좋은 예:

- Manifest API에 고정 페이지 도서 한 종류를 연결하고 테스트
- 책갈피 생성·목록 API와 최소 UI
- TXT UTF-8 한 파일을 읽고 Locator 저장

나쁜 예:

- “전체 리더를 완성해줘”
- 백엔드·프런트엔드·DB·Docker·리팩터링을 한 번에 변경
- 테스트 없이 모든 형식을 동시에 추가

권장 PR 크기는 생성 파일을 제외하고 400줄 안팎이다. 800줄을 넘길 것으로 예상되면 티켓을 쪼갠다.

## 14.2 구현 전 필수 행동

AI는 매 티켓마다 다음을 수행한다.

1. 관련 계획서와 ADR 읽기
2. 실제 코드 검색
3. 재사용 가능한 기존 패턴 2개 이상 제시
4. 수정할 파일 목록과 테스트 목록 제시
5. 위험·모호점 명시
6. 승인 또는 명시된 티켓 범위 안에서 구현

단, 이미 티켓에 결정된 사안을 반복 질문하지 않는다. 파괴적 변경, 라이선스 전환, 데이터 손실 위험이 없다면 합리적인 기본값으로 진행한다.

## 14.3 변경 금지 사항

AI는 명시적 승인 없이 다음을 하지 않는다.

- 전체 프레임워크 업그레이드
- 기존 공개 API의 호환성 파괴
- 기존 마이그레이션 수정
- `git reset --hard`, 강제 푸시, 이력 재작성
- 대량 파일 이름 변경
- 기존 테스트 삭제 또는 무력화
- 인증·권한 검사 우회
- AGPL/GPL 코드 복사
- 원본 미디어에 쓰기
- 비밀값 커밋

## 14.4 완료 보고 형식

```text
작업: SV-XXX 제목

변경한 내용
- ...

변경 파일
- path/to/file: 변경 이유

검증
- 실행: ./gradlew ...
- 결과: 통과/실패
- 미실행: 이유

보안·호환성
- 권한 검사: ...
- 마이그레이션/업스트림 영향: ...

남은 일
- ...
```

작업 종료 시 `docs/AI_PROGRESS.md`를 갱신한다.

---

# 15. 마일스톤과 백로그

## M0. 기준선과 가드레일

### SV-000 저장소 감사

산출물:

- 실제 프로젝트 구조
- 빌드·테스트 명령 검증
- 기존 스캐너, 분석기, 페이지 API, 리더, 진행률, 권한 코드 지도
- 확장 지점 후보와 위험 목록

완료 조건:

- 코드를 변경하지 않고 `docs/AI_PROGRESS.md`에 조사 결과 기록
- 모든 경로가 실제 존재함을 확인

### SV-001 기준 태그와 업스트림 정책

- Tales 1.24.4 기반 브랜치 생성
- `upstream` 원격 확인
- `docs/UPSTREAM_SYNC.md` 작성
- MIT LICENSE와 NOTICE 보존 확인

### SV-002 로컬 개발 환경

- JDK 21+, Node 18+ 확인
- 백엔드 실행
- 프런트엔드 dev server 실행
- 기본 테스트 결과 기록
- `.vscode` 권장 설정 검토

### SV-003 기능 플래그와 새 라우트 뼈대

- 새 리더 활성화 플래그
- `/read-v2/:bookId` 빈 화면
- 기존 리더 폴백 링크
- 라우팅·권한 회귀 테스트

## M1. Manifest와 리더 셸

### SV-100 Manifest 도메인·DTO

- 버전이 있는 Manifest 타입
- 고정 페이지 도서 한 종류에 응답
- 권한 검사
- 계약 테스트

### SV-101 프런트 Manifest 모델·서비스

- TypeScript 타입
- API 서비스
- 오류 모델
- 단위 테스트

### SV-102 UnifiedReader 셸

- loading/error/empty 상태
- toolbar와 콘텐츠 영역
- 기존 페이지 API를 통한 한 페이지 표시
- 라우트 이탈 정리

완료 조건:

- 기존 CBZ 한 권을 새 라우트에서 한 페이지로 읽을 수 있음
- 오류 시 기존 리더로 이동 가능

## M2. 고정 페이지 리더와 필터

### SV-200 FixedPageAdapter

- next/previous/goTo/current Locator
- 페이지 경계 처리
- 프리페치 취소

### SV-201 보기 모드

- 단면
- 양면
- 연속 스크롤
- LTR/RTL
- 페이지 맞춤

### SV-202 이미지 필터

- 밝기, 대비, 흑백, 세피아, 반전
- 프리셋
- 콘텐츠에만 필터 적용
- 모바일 성능 점검

### SV-203 입력과 접근성

- 키보드
- 터치
- 포커스
- 단축키 도움말

완료 조건:

- CBZ/ZIP/PDF에서 공통 보기 기능과 필터가 동작
- 위치 변경 이벤트가 안정적으로 발생

## M3. 위치·책갈피·설정

### SV-300 DB 마이그레이션

- 위치, 책갈피, 설정 테이블
- 기존 명명 규칙 준수
- 업·다운 또는 복원 전략 문서화
- Repository 통합 테스트

### SV-301 위치 API

- GET/PUT
- Locator 스키마 검증
- 사용자·책 권한
- debounce 저장 지원

### SV-302 책갈피 API

- 목록, 생성, 수정, 삭제
- 사용자 격리
- 잘못된 Locator 차단

### SV-303 설정 API와 우선순위

- 전역·라이브러리·시리즈·책
- 유효값 검증
- effective settings 계산

### SV-304 프런트 동기화

- 자동 저장·복원
- 책갈피 drawer
- 오프라인/실패 상태
- 탭 종료 flush

완료 조건:

- 다른 브라우저에서 위치·책갈피·설정 복원
- 사용자가 서로의 데이터를 볼 수 없음

## M4. TXT

### SV-400 TXT 스캔 설계 스파이크

- 기존 스캐너에 `txt`를 추가할 지점 확인
- Book/Media 타입 영향 분석
- 대용량 정책 제안
- ADR 작성

### SV-401 TXT 스캔·분석

- 라이브러리 옵션에서 TXT 활성화
- MIME·크기·fingerprint
- 메타데이터 기본값
- 변경·삭제 처리

### SV-402 TXT 콘텐츠 API

- UTF-8/BOM/euc-kr
- 수동 인코딩 선택
- 권한·크기 제한
- XSS 테스트

### SV-403 TextReflowAdapter

- 안전한 렌더링
- 페이지·스크롤 모드
- 글꼴·행간·폭·테마
- 반응형 재페이지네이션

### SV-404 안정 Locator

- 문단·문자 오프셋
- 설정 변경 후 복원
- normalizationVersion
- fingerprint 불일치 UX

완료 조건:

- 한국어 TXT 샘플이 테마와 페이지네이션을 포함해 읽힘
- 글꼴 변경 후 같은 내용 부근으로 복원

## M4.5. ZIP 내부 혼합 콘텐츠

### SV-450 ZIP 내부 TXT/PDF/JPG/GIF 설계 스파이크

- 기존 ZIP/CBZ 분석기가 내부 이미지 목록을 만드는 지점 확인
- ZIP 내부 TXT를 별도 책으로 볼지, 같은 책의 텍스트 항목으로 볼지 결정
- 혼합 TXT/이미지 Manifest 형태 결정
- ZIP 내부 PDF 처리 방식 결정
- GIF 애니메이션 유지 또는 첫 프레임 고정 처리 정책 결정
- 보안 제한과 테스트 픽스처 목록 정의
- ADR 작성

### SV-451 ZIP 내부 TXT 콘텐츠 연결

- ZIP 내부 TXT 항목 탐색
- UTF-8/BOM/euc-kr 인코딩 처리
- 크기·줄 길이 제한
- XSS-safe 텍스트 응답
- 권한 테스트

### SV-452 ZIP 내부 PDF/JPG/GIF 페이지 제공

- PDF/JPG/GIF 엔트리 MIME 검증
- 자연 정렬과 표지 후보 처리
- 이미지 크기 제한
- PDF 크기·페이지 수 제한과 단일 엔트리 캐시
- 기존 페이지 제공 흐름 재사용 또는 ShelfView 전용 어댑터
- 손상 이미지 오류 처리

### SV-453 혼합 ZIP 리더 통합

- TXT와 이미지가 섞인 ZIP의 읽기 순서 표시
- Manifest 기반 라우팅
- 책갈피와 마지막 위치 저장
- 기존 이미지 ZIP/CBZ 회귀 테스트

완료 조건:

- ZIP 안의 TXT, PDF, JPG, GIF 샘플이 새 리더에서 읽힘
- ZIP 전체를 브라우저로 내려받아 풀지 않음
- 악성 ZIP 경로와 과도한 크기 입력이 차단됨

## M5. 이미지 폴더 도서

### SV-500 마커 설정과 도메인

- 라이브러리별 활성화
- `.shelfview-book` 규칙
- 설정 UI와 도움말

### SV-501 스캔·분석

- 말단 폴더 감지
- 자연 정렬
- MIME·해상도 제한
- 표지·페이지 정보
- fingerprint

### SV-502 페이지 제공

- 기존 페이지 파이프라인 재사용 또는 어댑터
- 캐시
- Range/HTTP 캐시 헤더 검토
- 권한 테스트

### SV-503 파일 변경 수명주기

- 이미지 추가·삭제·이름 변경
- 폴더 이동
- 마커 삭제
- 책갈피 보존 정책

완료 조건:

- 마커 폴더가 한 권으로 표시되고 새 리더에서 읽힘
- 마커 없는 폴더는 기존 동작을 변경하지 않음

## M6. 호환성과 UX 안정화

### SV-600 기존 진행률 호환

- 고정 페이지에서 Tales 진행률과 동기화
- 기존 리더 ↔ 새 리더 왕복
- 완료/미완료 상태 회귀 테스트

### SV-601 오류·복구 UX

- 손상 파일
- 사라진 페이지
- 인코딩 오류
- 네트워크 끊김
- 폴백과 재시도

### SV-602 반응형·브라우저 검증

- 데스크톱 Chrome/Edge/Firefox
- 모바일 및 iPhone/iOS 계열 검증은 2026-06-19 사용자 지시에 따라 SV-602 완료 조건에서 제외한다.
- 기존 모바일 회전/폭 대응 자동 검사는 보조 안전망으로 유지하되, 출시 차단 조건으로 보지 않는다.

## M7. Docker·CI·Unraid

### SV-700 CI

- 백엔드 테스트
- 프런트 lint/test/build
- Docker build
- 변경 범위에 따른 작업 조건

### SV-701 Docker 이미지

- 버전·Git SHA 표시
- amd64 우선, arm64 검증
- non-root 실행
- health check 검토

### SV-702 Unraid 배포 문서

- 볼륨·권한
- appdata 백업
- 리버스 프록시
- 업그레이드·롤백
- 로그 수집

## M8. 보안·릴리스

### SV-800 위협 모델

- 자산, 신뢰 경계, 공격면
- 아카이브, TXT, API, 캐시
- 완화책과 잔여 위험

### SV-801 성능 기준선

- 대용량 아카이브·TXT
- 모바일 메모리
- 페이지 요청 취소·프리페치

### SV-802 라이선스·고지

- 의존성 목록
- `THIRD_PARTY_NOTICES`
- 직접 가져온 코드 감사
- 배포 라이선스 확정

### SV-803 v1.0 릴리스 후보

- 전체 회귀
- DB 백업·복원
- Docker 새 설치·업그레이드
- 알려진 제한
- 릴리스 노트

---

# 16. Definition of Ready와 Definition of Done

## 16.1 Ready

티켓은 다음을 만족해야 시작한다.

- 사용자 가치가 한 문장으로 설명됨
- 범위와 제외 범위가 있음
- 관련 형식·API·화면이 명시됨
- 보안·권한 요구가 있음
- 테스트 가능한 완료 조건이 있음
- 선행 티켓이 완료됨
- 라이선스가 불명확한 외부 코드에 의존하지 않음

## 16.2 Done

- 기능과 오류 경로 구현
- 자동 테스트 추가 또는 미추가 이유 기록
- 관련 기존 테스트 통과
- lint/build 통과
- 권한과 입력 검증 확인
- 문서·API 계약 갱신
- 신규 의존성의 이유·라이선스 기록
- `docs/AI_PROGRESS.md` 갱신
- 대규모 업스트림 충돌을 만들지 않았는지 자체 검토
- 실행하지 않은 검증을 명확히 표시
- 사용자 관점의 수동 확인 절차 제공

---

# 17. 첫 개발 세션의 정확한 순서

1. 스타터 팩 내용을 Tales 포크 루트에 복사한다.
2. VS Code에서 저장소 루트를 연다.
3. Git 상태와 기준 태그를 확인한다.
4. AI 채팅에서 `/repository-audit`를 실행한다.
5. AI가 작성한 코드 지도를 사람이 검토한다.
6. `SV-000`을 완료 처리한다.
7. 업스트림·라이선스·기준선 테스트를 `SV-001`, `SV-002`로 완료한다.
8. `/implement-ticket SV-003`으로 새 리더 라우트 뼈대만 만든다.
9. `/review-and-test SV-003`으로 독립 검토한다.
10. 작은 커밋으로 저장하고 다음 티켓으로 이동한다.

초기에 “TXT와 이미지 폴더까지 한 번에 만들어 달라”고 요청하지 않는다. 먼저 CBZ 한 페이지가 새 리더 셸에서 열리는 가장 얇은 수직 슬라이스를 완성한다.

---

# 18. 위험과 대응

| 위험 | 대응 |
|---|---|
| Tales 업스트림과 충돌 | 전용 네임스페이스, API 경로, 작은 훅, 정기 upstream-sync |
| Vue 2의 노후화 | MVP에서는 고정, 별도 현대화 ADR·프로젝트로 분리 |
| 이미지 폴더가 기존 스캔 모델과 충돌 | 마커 기반 opt-in, 스파이크 후 도메인 결정 |
| TXT 페이지가 설정마다 변함 | 페이지 번호가 아닌 문단·문자 Locator |
| 대용량 파일 메모리 폭증 | 스트리밍, 가상화, 제한, 취소 가능한 프리페치 |
| 아카이브 공격 | traversal·ratio·size·pixel·timeout 제한 |
| AI의 과도한 리팩터링 | 티켓 단위, 파일 목록 사전 제시, 금지 규칙, PR 크기 제한 |
| 라이선스 오염 | GPL/AGPL 코드 복사 금지, THIRD_PARTY 기록 |
| PDF 기능 기대 차이 | MVP 페이지 이미지, PDF.js 단계 명확히 분리 |
| 위치 데이터 손상 | versioned Locator, fingerprint, 마이그레이션·백업 테스트 |

---

# 19. 결정이 필요한 사용자 선택

개발을 막지 않는 항목은 기본값으로 시작한다.

| 항목 | 기본값 |
|---|---|
| 프로젝트 공개 여부 | 비공개 개발 후 공개 여부 결정 |
| 신규 코드 라이선스 | MIT 호환을 목표로 작성 |
| 이미지 폴더 감지 | `.shelfview-book` 마커 필수 |
| TXT 기본 인코딩 | auto → UTF-8 → euc-kr 후보 |
| PDF | MVP 페이지 이미지, PDF.js 후속 |
| 기본 읽기 방향 | 라이브러리·시리즈 설정 상속 |
| 기본 새 리더 활성화 | 관리자 opt-in |
| 원본 변경 | 절대 하지 않음 |

공개 라이선스, 상표·이름, 사용자 업로드 허용 여부는 v1.0 릴리스 전 확정한다.

---

# 20. 공식 참고 자료

- Tales 저장소: https://github.com/gotson/tales
- Tales 개발 지침: https://github.com/gotson/tales/blob/master/DEVELOPING.md
- Tales Docker 문서: https://tales.org/docs/installation/docker/
- Tales 라이브러리 문서: https://tales.org/docs/guides/libraries/
- Tales API 문서 안내: https://tales.org/docs/guides/api/
- PDF.js: https://github.com/mozilla/pdf.js
- zip.js: https://github.com/gildas-lormeau/zip.js
- foliate-js: https://github.com/johnfactotum/foliate-js
- Readium Web: https://github.com/readium/web
- VS Code custom instructions: https://code.visualstudio.com/docs/agent-customization/custom-instructions
- VS Code prompt files: https://code.visualstudio.com/docs/agent-customization/prompt-files

---

# 부록 A. 사용자 인수 테스트

## A.1 설치

- 새 `/config`로 컨테이너가 시작된다.
- 관리자 생성과 로그인에 성공한다.
- `/data`가 읽기 전용이어도 스캔된다.
- 컨테이너 재시작 후 DB와 설정이 유지된다.

## A.2 CBZ·ZIP

- 1/2/10 페이지가 올바른 순서다.
- 단면·양면·웹툰 전환 시 위치가 유지된다.
- RTL이 올바르게 동작한다.
- 밝기·대비·반전이 페이지에만 적용된다.
- 책갈피를 만든 뒤 다른 브라우저에서 이동한다.

## A.3 PDF

- 3페이지 PDF와 큰 PDF가 열린다.
- 확대·회전·페이지 이동이 된다.
- 페이지 책갈피가 복원된다.
- MVP에서 텍스트 선택이 미지원이면 UI가 거짓 기능을 표시하지 않는다.

## A.4 TXT

- UTF-8, BOM, euc-kr 파일을 읽는다.
- 검정 배경·흰 글자와 종이색 테마가 동작한다.
- 글꼴 크기를 바꿔도 같은 문장 부근으로 복원된다.
- `<script>` 문자열이 실행되지 않는다.
- 너무 큰 파일은 명확한 제한 메시지를 표시한다.

## A.5 이미지 폴더

- 마커가 있는 폴더만 책이 된다.
- 자연 정렬된다.
- 빈 폴더와 중첩 폴더는 안전하게 처리된다.
- 이미지 추가 후 재스캔 시 페이지 수가 갱신된다.

## A.6 권한

- 허용되지 않은 라이브러리의 manifest·페이지·TXT를 열 수 없다.
- 사용자 A의 책갈피를 사용자 B가 볼 수 없다.
- 로그아웃 후 이전 콘텐츠 URL이 보호된다.

---

# 부록 B. AI가 ADR을 작성해야 하는 경우

다음 상황에서는 즉시 구현보다 ADR을 먼저 작성한다.

- 기존 Tales 도메인 모델을 변경해야 함
- 기존 공개 API 응답을 변경해야 함
- 새 런타임 의존성 또는 GPL/AGPL 코드를 고려함
- DB 데이터가 손실될 수 있는 마이그레이션
- 이미지 폴더를 Book으로 표현하는 방법이 둘 이상임
- PDF.js 도입
- TXT 대용량 스트리밍 방식
- Vue 2 제약을 우회하기 위한 새 프레임워크 도입
- 원본 미디어에 쓰기가 필요한 설계
- 인증·권한 모델 변경

ADR은 문제, 제약, 후보, 결정, 결과, 되돌리기 방법을 포함한다.
