---
name: pr
description: WSL의 gh CLI로 이 저장소(wakeupjunyoung/MOEMI-BE)에 Pull Request를 만든다. PR 생성 전에 반드시 최신 base 브랜치를 pull 받아 충돌을 먼저 해결한다. "PR 만들어줘", "풀리퀘 올려줘", "PR 생성", "create a PR" 같은 요청에 사용.
---

# PR 생성 (gh CLI)

- 기본 base 브랜치: `main` (원격 기본 브랜치)
- 저장소: `wakeupjunyoung/MOEMI-BE` / gh 계정 `jahan-93` (권한 ADMIN, 직접 push 가능)
- **이 스킬은 사용자가 PR을 요청했을 때만 동작한다.** CLAUDE.md Rule 1(push 금지)의 유일한 예외이며,
  push 직전에 "`<브랜치>` 를 origin에 push 하고 PR을 만들겠다"고 알리고 진행한다.

## 1. 사전 점검

```bash
gh auth status                  # 로그인/스코프 확인 (repo 스코프 필요)
git status --short              # 커밋 안 된 변경이 남아 있는지
git branch --show-current
git log --oneline origin/main..HEAD   # PR에 담길 커밋 목록
```

- 커밋되지 않은 변경이 있으면 멈추고 사용자에게 알린다. `commit` 스킬로 기능 단위 커밋을 먼저 만든다.
- 현재 브랜치가 `main`/`master`면 PR을 만들 수 없다. 기능 브랜치를 먼저 만든다:
  `git switch -c feat/<기능명>` (기존 커밋을 옮겨야 하면 사용자에게 확인)
- `origin/main..HEAD` 가 비어 있으면 PR로 올릴 커밋이 없는 것이다. 멈춘다.

## 2. base 브랜치 pull (필수)

PR을 만들기 **전에** 항상 최신 base를 받아 온다.

```bash
git fetch --prune origin
git pull --rebase origin main     # 현재 기능 브랜치 위로 최신 main 을 올린다
```

- 충돌이 나면 **PR 생성을 중단**하고 충돌 파일 목록과 함께 사용자에게 보고한다.
  임의로 한쪽을 버리고 해결하지 않는다. 해결 후 `git rebase --continue`.
- rebase 가 곤란한 상황(이미 공유된 브랜치 등)이면 `git merge origin/main` 으로 대체하고 그 사실을 알린다.
- pull 이후 컴파일이 깨지지 않는지 확인: `./gradlew compileJava` (테스트를 건드렸으면 `./gradlew test`)

## 3. push + PR 생성

```bash
git push -u origin "$(git branch --show-current)"

gh pr create --base main --head "$(git branch --show-current)" \
  --title "feat: 회원가입 API 구현" --body-file - <<'BODY'
## 요약
회원가입 API를 추가했다.

## 변경사항
- Member 엔티티 / Repository / Service / Controller 추가
- 이메일 중복 검사 및 BCrypt 비밀번호 암호화

## 테스트
- `./gradlew test` 통과
- POST /api/members 수동 확인

## 확인 필요
- 비밀번호 정책을 프론트 검증과 맞출지

🤖 Generated with [Claude Code](https://claude.com/claude-code)
BODY
```

- 제목은 대표 커밋 메시지와 같은 형식(`<type>: <한국어 요약>`)을 쓴다.
- 커밋이 여러 개면 제목은 PR 전체를 아우르는 한 줄로 쓰고, 세부는 `## 변경사항`에 나열한다.
- 초안으로 올릴 때는 `--draft` 를 붙인다.
- 생성 후 PR URL을 사용자에게 그대로 전달한다. (`gh pr view --web` 으로 열 수 있음)

## 4. 실패 시

- `gh` 인증 만료: `gh auth login` 은 대화형이라 이 환경에서 실행할 수 없다.
  사용자에게 프롬프트에서 `! gh auth login` 을 직접 실행하도록 안내한다.
- push 거부(non-fast-forward): 강제 push 하지 말고 2번의 pull 단계를 다시 수행한 뒤 보고한다.
- PR이 이미 존재하면 `gh pr view` 로 기존 PR을 보여주고, 새로 만들지 말고 push 만으로 갱신한다.

## 금지

- `git push --force`, `--force-with-lease` 는 사용자가 명시적으로 요청할 때만.
- PR 머지(`gh pr merge`)는 이 스킬의 범위가 아니다. 사용자가 따로 요청해야 한다.
- base 브랜치(`main`)에 직접 push 하지 않는다.
