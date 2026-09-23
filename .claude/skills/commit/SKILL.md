---
name: commit
description: 변경사항을 기능 단위로 쪼개서 커밋한다. "커밋해줘", "지금까지 작업 커밋", "커밋 나눠줘", "commit this" 같은 요청에 사용. 한 커밋에 한 기능만 담고, 관련 없는 변경은 별도 커밋으로 분리한다.
---

# 기능 단위 커밋

**핵심 규칙: 커밋 하나 = 기능 하나.** 변경 파일이 여러 기능에 걸쳐 있으면 커밋을 나눈다.
`git add .` 로 전부 담는 커밋은 만들지 않는다.

## 1. 현재 상태 파악

```bash
git status --short
git diff            # 아직 stage 안 된 변경
git diff --cached   # 이미 stage 된 변경
git log --oneline -10   # 기존 메시지 스타일 확인 (있으면 그 스타일을 따른다)
```

## 2. 기능 단위로 묶기

변경사항을 아래 기준으로 그룹핑하고, **커밋하기 전에 사용자에게 "이렇게 N개로 나누겠다"고 목록을 보여준다.**

한 커밋에 묶어도 되는 것:
- 하나의 기능에 필요한 Entity + Repository + Service + Controller + DTO (수직 슬라이스)
- 그 기능의 테스트 코드
- 그 기능 때문에 생긴 설정 변경 (의존성 추가 등)

반드시 나눠야 하는 것:
- 서로 다른 도메인/기능 (예: 회원가입 ↔ 게시글 CRUD)
- 기능 추가 ↔ 기존 코드 리팩터링
- 기능 코드 ↔ 빌드·설정 파일만의 변경 (`build.gradle`, `application.properties` 단독 변경)
- 오타/포맷팅 정리는 별도 `style:` 또는 `chore:` 커밋

애매하면 "이 커밋만 되돌렸을 때 말이 되는가?"로 판단한다.

## 3. 선택적으로 stage 하고 커밋

```bash
git add src/main/java/com/example/moemi/member/ src/test/java/com/example/moemi/member/
git diff --cached --stat        # 의도한 파일만 들어갔는지 확인 (필수)
git commit -F- <<'MSG'
feat: 회원가입 API 구현

- Member 엔티티와 JPA Repository 추가
- 이메일 중복 검사 후 BCrypt로 비밀번호 저장
MSG
```

- 파일 하나에 두 기능이 섞였으면: 가능하면 코드를 먼저 정리하고, 어려우면 사용자에게 알린 뒤 한 커밋으로 묶는다.
  (이 환경에서는 `git add -p` 같은 대화형 명령을 쓸 수 없다.)
- 커밋 사이마다 `git status --short` 로 남은 변경을 다시 확인한다.

## 4. 메시지 형식

```
<type>: <한 줄 요약 — 한국어, 50자 내외, 명사형 종결>

- 무엇을 왜 바꿨는지 1~3줄 (어떻게는 코드가 말한다)
```

`Co-Authored-By: Claude ...` 같은 작업자 표기는 넣지 않는다 (CLAUDE.md Rule 4).

type: `feat`(기능) `fix`(버그) `refactor` `test` `chore`(빌드·설정) `docs` `style`

예시:
- `feat: 게시글 목록 페이지네이션 API 추가`
- `fix: 로그인 실패 시 500 대신 401 반환`
- `chore: Spring Security 의존성 추가`

한 줄 요약에 "및", "그리고"가 들어가면 커밋을 나눠야 한다는 신호다.

## 5. 커밋 전 체크리스트

- [ ] `git diff --cached` 에 의도하지 않은 파일이 없는가 (`build/`, `.idea/`, `*.iml` 은 .gitignore 대상)
- [ ] DB 비밀번호·토큰·API 키가 `application.properties` 등에 하드코딩된 채 들어가지 않는가
- [ ] 디버그용 `System.out.println`, 주석 처리한 코드가 섞여 있지 않은가
- [ ] 컴파일되는 상태인가 (`./gradlew compileJava`, 테스트를 건드렸으면 `./gradlew test`)

## 주의

- **push 는 사용자가 명시적으로 요청할 때만** 한다. 커밋까지만 하고 멈춘다.
- 현재 로컬 브랜치는 `master`, 원격 기본 브랜치는 `origin/main` 이다. push 할 때 대상 브랜치를 사용자에게 확인한다.
- 이 저장소는 아직 커밋이 없다. 첫 커밋은 Spring 스캐폴드(gradle wrapper, build.gradle, MoemiApplication)를
  `chore: Spring Boot 프로젝트 초기 설정` 하나로 묶고, 그 뒤 작업부터 기능 단위로 나눈다.
- `git commit --amend`, `git reset`, `git rebase` 등 히스토리를 바꾸는 명령은 사용자가 요청할 때만 쓴다.
