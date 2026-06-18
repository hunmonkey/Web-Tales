# ShelfView VS Code AI Starter Pack

이 폴더는 Komga 포크 저장소의 **루트에 그대로 복사**하기 위한 개발 지침 묶음이다.

## 포함 파일

- `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md`: 전체 개발 계획
- `AGENTS.md`: 여러 AI 에이전트가 공통으로 읽는 핵심 규칙
- `.github/copilot-instructions.md`: VS Code Copilot 상시 지침
- `.github/instructions/*.instructions.md`: 백엔드·프런트엔드·보안별 지침
- `.github/prompts/*.prompt.md`: 반복 작업용 슬래시 명령
- `docs/ARCHITECTURE.md`: 축약 아키텍처
- `docs/BACKLOG.md`: 실행 순서와 티켓
- `docs/AI_PROGRESS.md`: AI 작업 기록
- `docs/ADR/`: 중요한 결정 기록
- `docs/SECURITY_AND_LICENSE.md`: 보안·라이선스 가드레일
- `docker-compose.unraid.example.yml`: Unraid 배포 예시

## 시작 순서

1. Komga `1.24.4` 태그에서 포크 브랜치를 만든다.
2. 이 폴더의 **내용물**을 저장소 루트에 복사한다.
3. VS Code로 루트를 연다.
4. AI 채팅에서 `/repository-audit`를 실행한다.
5. `docs/AI_PROGRESS.md`의 감사 결과를 검토한다.
6. `/implement-ticket SV-003`처럼 한 티켓씩 구현한다.
7. 완료 후 `/review-and-test SV-003`을 실행한다.

## 주의

- 이 팩은 Komga 소스 자체를 포함하지 않는다.
- AGPL/GPL 프로젝트의 코드를 기본 구현에 복사하지 않는다.
- 실제 코드 구조가 계획과 다르면 추측으로 진행하지 말고 ADR을 남긴다.
- 원본 미디어는 항상 읽기 전용으로 취급한다.
