---
name: commit
description: 변경사항을 기능 단위로 쪼개서 커밋한다. "커밋해줘", "지금까지 작업 커밋", "커밋 나눠줘", "commit this" 같은 요청에 사용. 한 커밋에 한 기능만 담고, 관련 없는 변경은 별도 커밋으로 분리한다. 메시지는 .github/CONTRIBUTING.md 형식(영문 type 접두어 + 한국어 요약)을 따른다.
---

# 기능 단위 커밋

팀 규칙은 `.github/CONTRIBUTING.md`가 기준이다. 커밋 메시지 양식은 `.github/.gitmessage`와 같다.

**핵심 규칙: 커밋 하나 = 기능 하나.** 변경 파일이 여러 기능에 걸쳐 있으면 커밋을 나눈다.
`git add .` 로 전부 담는 커밋은 만들지 않는다.

## 1. 현재 상태 파악

```bash
git status --short
git branch --show-current
git diff            # 아직 stage 안 된 변경
git diff --cached   # 이미 stage 된 변경
git log --oneline -10   # 기존 메시지 스타일 확인
```

### 브랜치 확인

`develop`, `main`에는 바로 커밋하지 않는다. 작업은 `develop`에서 만든 작업 브랜치에서 한다.
먼저 `./gradlew preflight`를 실행한다. `[FAIL]`이 나오면 그 문제부터 해결하고, `[WARN]`은 사용자에게 알린다.

- 현재 브랜치가 `develop`/`main`이면 커밋 전에 사용자에게 브랜치 이름을 확인하고 만든다.
  커밋 안 한 변경은 `git switch -c`로 브랜치를 만들면 그대로 따라온다.
  - 기능 명세서의 기능: `feat/lifehelper-<기능번호>` (예: `feat/lifehelper-5`)
  - 그 외: `<type>/<짧은-설명>` (예: `fix/favorite-order`, `chore/github-conventions`)
  ```bash
  git switch -c fix/favorite-order
  ```
- 현재 브랜치의 PR이 이미 머지됐으면 그 브랜치에 새 작업을 쌓지 않는다 (머지한 작업 브랜치는 삭제 대상).
  새 작업 브랜치를 최신 `develop`에서 만들지 사용자에게 확인한다. 커밋 안 한 변경을 들고 옮길 때:
  ```bash
  git stash
  git switch -c <새 브랜치> origin/develop
  git stash pop    # 충돌이 나면 멈추고 사용자에게 보고한다
  ```

## 2. 기능 단위로 묶기

변경사항을 아래 기준으로 그룹핑하고, **커밋하기 전에 사용자에게 "이렇게 N개로 나누겠다"고 목록을 보여준다.**

한 커밋에 묶어도 되는 것:
- 하나의 기능에 필요한 Entity + Repository + Service + Controller + DTO (수직 슬라이스)
- 그 기능의 테스트 코드
- 그 기능 때문에 생긴 설정 변경 (의존성 추가 등)

반드시 나눠야 하는 것:
- 서로 다른 도메인/기능 (예: 생활수치 캘린더 ↔ 준비물 체크리스트)
- 기능 추가 ↔ 기존 코드 리팩터링
- 기능 코드 ↔ 빌드·설정 파일만의 변경 (`build.gradle`, `application.properties`, `Dockerfile` 단독 변경)
- 오타/포맷팅 정리는 별도 `style:` 커밋

애매하면 "이 커밋만 되돌렸을 때 말이 되는가?"로 판단한다.

## 3. 선택적으로 stage 하고 커밋

```bash
git add src/main/java/com/example/moemi/favorite/ src/test/java/com/example/moemi/favorite/
git diff --cached --stat        # 의도한 파일만 들어갔는지 확인 (필수)
git commit -F- <<'MSG'
feat: 버스 즐겨찾기 API 구현

- 정류장·노선을 즐겨찾기에 등록·조회·삭제
- 관련 이슈: #12
MSG
```

- 파일 하나에 두 기능이 섞였으면: 가능하면 코드를 먼저 정리하고, 어려우면 사용자에게 알린 뒤 한 커밋으로 묶는다.
  (이 환경에서는 `git add -p` 같은 대화형 명령을 쓸 수 없다.)
- 커밋 사이마다 `git status --short` 로 남은 변경을 다시 확인한다.

## 4. 메시지 형식

```
<type>: <한 줄 요약 — 한국어, 50자 내외, 명사형 종결>

- 무엇을, 왜 바꿨는지 1~3줄 (선택, 어떻게는 코드가 말한다)
- 관련 이슈: #이슈번호
```

- 관련 이슈가 있으면 본문 마지막 줄에 이슈 번호를 적는다. 사용자가 번호를 말하지 않았으면 묻는다.
  줄을 `#`로 시작하지 않는다. 커밋 템플릿(에디터)으로 커밋하면 `#`로 시작하는 줄은 주석으로 지워진다.
- `Co-Authored-By: Claude ...` 같은 작업자 표기는 넣지 않는다 (CLAUDE.md Rule 4).
- 한 줄 요약에 "및", "그리고"가 들어가면 커밋을 나눠야 한다는 신호다.

| type | 언제 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 그대로, 구조 개선 |
| `test` | 테스트만 추가·수정 |
| `docs` | 문서 (README, CONTRIBUTING 등) |
| `chore` | 빌드·설정·의존성·배포 (`build.gradle`, `Dockerfile`, `.github` 등) |
| `style` | 포맷·오타 등 동작과 무관한 변경 |

예시:
- `feat: 버스 즐겨찾기 API 구현`
- `fix: 즐겨찾기 순서 변경 시 400 대신 500 반환하던 문제 수정`
- `chore: Cloudtype 배포용 Dockerfile 추가`

## 5. 커밋 전 체크리스트

- [ ] 작업 브랜치에서 커밋하는가 (`develop`/`main`이 아님)
- [ ] `git diff --cached` 에 의도하지 않은 파일이 없는가 (`build/`, `.idea/`, `*.iml` 은 .gitignore 대상)
- [ ] DB 비밀번호·토큰·API 키가 들어가지 않는가
  - `application.properties` 등에 실제 값을 하드코딩하지 않는다
  - 개인 설정 파일 `application-local.properties`는 stage 하지 않는다
- [ ] 디버그용 `System.out.println`, 주석 처리한 코드가 섞여 있지 않은가
- [ ] 컴파일되는 상태인가 (`./gradlew compileJava`, 테스트를 건드렸으면 `./gradlew test`).
  PR을 올리려면 `./gradlew test` 통과가 필수다 (`pr` 스킬).

## 주의

- **push 는 사용자가 명시적으로 요청할 때만** 한다. 커밋까지만 하고 멈춘다. PR 요청이면 `pr` 스킬이 push 한다.
- `develop`, `main`에 직접 push 하지 않는다.
- `git commit --amend`, `git reset`, `git rebase` 등 히스토리를 바꾸는 명령은 사용자가 요청할 때만 쓴다.
