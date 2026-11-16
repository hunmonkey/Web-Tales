# Tales

> A self-hosted web reader for comics, books, PDFs, text files and image archives.
> Mount your own library on a home server and read from any browser — desktop, tablet or phone.
> Forked from Komga and extended with a unified reader (ShelfView) for PDF, TXT, ZIP/CBZ and image folders.

내 서버에 올린 만화·책·PDF·텍스트 파일을 **브라우저로 읽는 자체 호스팅 웹 리더**입니다.
홈서버(Unraid 등)에 책 폴더를 연결해 두면, PC·태블릿·휴대폰 어디서나 이어서 읽을 수 있습니다.

---

## 주요 기능

- **여러 형식을 하나의 리더로** — PDF, TXT, ZIP/CBZ, 이미지 폴더를 같은 화면(ShelfView)에서 읽습니다.
  - 고정 페이지(PDF·만화)와 글자 흐름형(TXT)을 각각에 맞는 방식으로 표시
  - 텍스트는 문단·글자 위치까지 기억해, 다른 기기에서도 읽던 곳부터 이어 읽기
- **서재 관리** — 라이브러리·시리즈·책·컬렉션·읽기 목록 정리, 중복 파일 찾기
- **여러 사용자** — 사용자별 라이브러리 접근 권한, 관리자 화면
- **모바일 최적화** — 반응형 화면, 고정 툴바, 전체 화면 읽기
- **안전한 파일 처리** — 압축 경로 탈출·압축 폭탄·과도한 파일 수를 막고, 텍스트 파일은 HTML로 해석하지 않고 글자 그대로 안전하게 표시
- **홈서버 배포** — Docker 이미지, 헬스체크, 일반 사용자 권한(non-root) 실행

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 서버 | Kotlin · Spring Boot · Gradle · jOOQ · Flyway (SQLite) |
| 화면 | Vue 2.6 · TypeScript · Vuex · Vuetify |
| 배포 | Docker (포트 25600, `/config` 볼륨) |

## 빠른 시작

요구 사항: JDK 21 이상, Node.js 22.13 이상

- 개발 환경 설정: **[DEVELOPING.md](DEVELOPING.md)**
- Unraid 설치·백업·업그레이드·되돌리기: **[docs/UNRAID_DEPLOYMENT.md](docs/UNRAID_DEPLOYMENT.md)**
- 구조 설명: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**
- 보안·라이선스 정책: **[docs/SECURITY_AND_LICENSE.md](docs/SECURITY_AND_LICENSE.md)**

## 기반 프로젝트와 라이선스

Tales는 오픈소스 만화·책 서버 **[Komga](https://github.com/gotson/komga)**(© Gauthier Roebroeck, MIT License)를 바탕으로 만들었습니다.
원본의 서재 관리·인증·페이지 전송 기능을 이어받고, 그 위에 통합 리더(ShelfView)와 형식별 읽기 기능을 더했습니다.

이 저장소도 MIT License를 따릅니다. 자세한 내용은 [LICENSE](LICENSE)를 보세요.

---

만든 사람: [hunmonkey](https://github.com/hunmonkey)
