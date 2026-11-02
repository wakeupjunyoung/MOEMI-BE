---
name: pr
description: WSL의 gh CLI로 이 저장소(wakeupjunyoung/MOEMI-BE)에 develop 대상 Pull Request를 만든다. PR 전에 최신 develop을 merge하고 ./gradlew test를 통과시킨 뒤, .github/PULL_REQUEST_TEMPLATE.md 양식으로 본문을 채운다. "PR 만들어줘", "풀리퀘 올려줘", "PR 생성", "create a PR" 같은 요청에 사용.
---

# PR 생성 (gh CLI)

팀 규칙은 `.github/CONTRIBUTING.md`가 기준이다. 이 스킬은 그 규칙대로 PR을 만든다.

- base 브랜치: **`develop`** (통합 브랜치). `main`은 배포 브랜치라 `develop`에서만 머지한다.
- 저장소: `wakeupjunyoung/MOEMI-BE` / gh 계정 `jahan-93`
- `gh`는 Windows(Git Bash) PATH에 없고 **WSL에만 있다.** `wsl.exe -e gh ...`로 실행한다.
  저장소 루트에서 실행하면 WSL의 작업 폴더도 같은 곳(`/mnt/c/...`)이 된다.
- **이 스킬은 사용자가 PR을 요청했을 때만 동작한다.** CLAUDE.md Rule 1(push 금지)의 유일한 예외이며,
  push 직전에 "`<브랜치>`를 origin에 push하고 PR을 만들겠다"고 알리고 진행한다.

## 1. 사전 점검

```bash
./gradlew preflight                      # 브랜치·충돌·develop 최신 여부 점검 ([FAIL]이면 멈춘다)
wsl.exe -e gh auth status                # 로그인/스코프 확인 (repo 스코프 필요)
git status --short                       # 커밋 안 된 변경이 남아 있는지
git branch --show-current
git fetch --prune origin
git log --oneline origin/develop..HEAD   # PR에 담길 커밋 목록
```

- 커밋되지 않은 변경이 있으면 멈추고 사용자에게 알린다. `commit` 스킬로 기능 단위 커밋을 먼저 만든다.
- 현재 브랜치가 `develop`/`main`이면 PR을 만들 수 없다. `develop`에서 규칙에 맞는 작업 브랜치를 만든다.
  기존 커밋을 옮겨야 하면 사용자에게 먼저 확인한다.
  - 기능 명세서의 기능: `feat/lifehelper-<기능번호>` (예: `feat/lifehelper-5`)
  - 그 외: `<type>/<짧은-설명>` (예: `fix/favorite-order`). `<type>`은 커밋 type과 같다.
  ```bash
  git switch -c feat/lifehelper-5 origin/develop
  ```
- 현재 브랜치의 PR이 이미 머지됐으면 그 브랜치에 이어서 올리지 않는다 (머지한 작업 브랜치는 삭제 대상).
  `develop`에서 새 작업 브랜치를 만들어 커밋을 옮길지 사용자에게 확인한다.
  ```bash
  wsl.exe -e gh pr list --repo wakeupjunyoung/MOEMI-BE --head "$(git branch --show-current)" --state all
  ```
- `origin/develop..HEAD`가 비어 있으면 PR로 올릴 커밋이 없는 것이다. 멈춘다.
- 커밋 메시지가 `<type>: <한국어 요약>` 형식인지 확인한다. 어긋나면 사용자에게 알린다 (히스토리 수정은 사용자가 요청할 때만).
- 관련 이슈를 확인한다. 사용자가 번호를 말하지 않았으면 열린 이슈에서 찾아보고, 확실하지 않으면 묻는다. 없으면 "없음".
  ```bash
  wsl.exe -e gh issue list --repo wakeupjunyoung/MOEMI-BE --state open
  ```

## 2. 최신 develop 반영 + 테스트 (필수)

PR을 만들기 **전에** 최신 `develop`을 **merge**한다. rebase가 아니라 merge다 (CONTRIBUTING 규칙).

```bash
git fetch --prune origin
git merge origin/develop
./gradlew test
```

- 이미 최신이면 `Already up to date.`로 끝난다.
- 충돌이 나면 **PR 생성을 중단**하고 충돌 파일 목록과 함께 사용자에게 보고한다.
  임의로 한쪽을 버리고 해결하지 않는다. 해결한 뒤 `git add <파일>` → `git commit`으로 merge 커밋을 마무리한다.
- **`./gradlew test` 통과가 PR 조건이다.** 실패하면 PR을 만들지 않고 결과를 보고한다 (CLAUDE.md Rule 2에 따라 Notion에 기록).
- 키·비밀번호가 커밋에 없는지 확인한다. 실제 키 값이나 `application-local.properties`가 보이면 멈추고 사용자에게 알린다.
  ```bash
  git diff --stat origin/develop...HEAD
  git diff origin/develop...HEAD | grep -niE "password|secret|token|service-?key|api-?key"
  ```

## 3. push + PR 생성

본문은 `.github/PULL_REQUEST_TEMPLATE.md` 양식으로 **짧게** 쓴다.
읽는 사람이 한눈에 "무슨 문제를 어떻게 풀었는지" 알 수 있으면 된다.

- 제목: 대표 커밋 메시지와 같은 형식(`<type>: <한국어 요약>`). 커밋이 여러 개면 PR 전체를 아우르는 한 줄.
- 문제·해결: 각각 한두 문장, 쉬운 한국어로 쓴다.
  - API 경로, 클래스·파일 이름, 설정 키, 상태 코드를 나열하지 않는다. 표도 쓰지 않는다.
  - 기능 추가는 "어떤 기능을 추가했다" 정도로 쓴다. (예: "버스 즐겨찾기 기능을 추가했다.")
  - 리뷰어가 꼭 알아야 할 점(예상되는 충돌 등)이 있으면 해결 끝에 한 줄만 덧붙인다.
- 관련 이슈: `Closes #번호` (머지되면 이슈가 자동으로 닫힌다). 없으면 "없음".
- 배포 시 필요: 배포할 때 사람이 해야 할 일만 적는다 (새 환경변수 추가, 외부 API 신청 등). 없으면 "없음".
  새 환경변수는 아래 명령으로 찾는다.
  ```bash
  git diff origin/develop...HEAD -- src/main/resources | grep -n '^+.*\${'
  ```
- 템플릿의 섹션은 **지우지 않는다.** 해당 없는 칸에는 "없음"이라고 적는다.
- 체크리스트: 1·2단계에서 실제로 확인한 것만 체크한다.
- PR 본문에 Claude 생성 표기("Generated with Claude Code" 등)를 넣지 않는다 (CLAUDE.md Rule 4).

본문 예시:

```markdown
## 관련 이슈
Closes #12

## 문제
자주 타는 버스를 볼 때마다 정류장과 노선을 매번 다시 검색해야 했다.

## 해결
버스 즐겨찾기 기능을 추가했다. 즐겨찾기한 버스의 도착 정보를 첫 화면에서 바로 볼 수 있다.

## 배포 시 필요
- 환경변수 `TAGO_SERVICE_KEY` 추가 (공공데이터포털에서 TAGO 버스 API 활용신청 필요)

## 체크리스트
- [x] base 브랜치가 `develop`이다
- [x] 최신 `develop`을 반영했다
- [x] `./gradlew test` 통과
- [x] 키·비밀번호가 커밋에 없다
```

제목과 본문은 **파일로 써서** WSL gh에 넘긴다. 한글이 명령행 인자로 넘어가면서 깨지는 것을 막기 위해서다.
`.git/` 안에 두면 커밋되지 않고, 다음 PR 때 덮어쓴다.

1. Write 도구로 `.git/PR_TITLE.txt`(제목 한 줄)와 `.git/PR_BODY.md`(본문)를 만든다.
2. push한 뒤 PR을 만든다. **저장소 루트에서** 실행한다.

```bash
git push -u origin "$(git branch --show-current)"

BRANCH="$(git branch --show-current)"
MSYS_NO_PATHCONV=1 wsl.exe -e bash -c 'gh pr create --repo wakeupjunyoung/MOEMI-BE --base develop --head "$1" --title "$(cat .git/PR_TITLE.txt)" --body-file .git/PR_BODY.md' _ "$BRANCH"
```

- 초안으로 올릴 때는 `gh pr create`에 `--draft`를 붙인다.
- 사용자가 리뷰어를 말하면 `--reviewer <GitHub 아이디>`를 붙인다. 머지는 리뷰어 1명 이상이 승인한 뒤 사람이 한다.
- 만든 뒤 제목·base·커밋이 맞는지 확인하고, PR URL을 사용자에게 그대로 전달한다.
  ```bash
  wsl.exe -e gh pr view <번호> --repo wakeupjunyoung/MOEMI-BE --json title,baseRefName,headRefName,commits,mergeable
  ```

## 4. 실패 시

- `gh` 인증 만료: `gh auth login`은 대화형이라 이 환경에서 실행할 수 없다.
  사용자에게 터미널에서 `wsl.exe -e gh auth login`을 직접 실행하도록 안내한다.
- push 거부(non-fast-forward): 강제 push 하지 않는다. 원격 브랜치에 무엇이 더 있는지 확인하고 사용자에게 보고한다.
  `git log --oneline HEAD..origin/<브랜치>`
- PR이 이미 열려 있으면 `wsl.exe -e gh pr view --repo wakeupjunyoung/MOEMI-BE <브랜치>`로 보여주고,
  새로 만들지 말고 push만으로 갱신한다.
- 제목이 깨져 보이면 제목·본문을 파일로 넘겼는지 확인한다.

## develop → main 배포 PR

`main` 대상 PR은 사용자가 **배포용 PR**을 명시적으로 요청할 때만 만든다. head는 `develop`이고,
1단계의 "`develop`에서는 PR 불가" 규칙은 이 경우에 적용하지 않는다.

```bash
MSYS_NO_PATHCONV=1 wsl.exe -e bash -c 'gh pr create --repo wakeupjunyoung/MOEMI-BE --base main --head develop --title "$(cat .git/PR_TITLE.txt)" --body-file .git/PR_BODY.md'
```

## 금지

- `git push --force`, `--force-with-lease`는 사용자가 명시적으로 요청할 때만.
- PR 머지(`gh pr merge`)는 이 스킬의 범위가 아니다. 리뷰어 승인 후 사람이 머지한다.
- `develop`, `main`에 직접 push 하지 않는다.
- 이미 push한 커밋을 rebase로 바꾸지 않는다. 최신화는 merge로 한다.
