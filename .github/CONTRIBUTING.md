# MOEMI-BE 협업 규칙

이슈 → 브랜치 → 커밋 → PR → 리뷰 → 머지 순서로 작업합니다.

## 1. 브랜치

| 브랜치 | 용도 |
|---|---|
| `main` | 배포 브랜치. 직접 push 금지, `develop`에서만 머지 |
| `develop` | 통합 브랜치. 모든 작업 PR의 base |
| `<type>/<이름>` | 작업 브랜치. `develop`에서 만들고 `develop`으로 PR |

작업 브랜치 이름:

- 기능 명세서의 기능: `feat/lifehelper-<기능번호>` (예: `feat/lifehelper-4`)
- 그 외: `<type>/<짧은-설명>` (예: `fix/favorite-order`, `chore/github-conventions`)
- `<type>`은 아래 커밋 type과 같습니다.

```bash
git fetch origin
git switch -c feat/lifehelper-5 origin/develop
```

## 2. 커밋

### 형식

```
<type>: <한 줄 요약>

- 무엇을, 왜 바꿨는지 1~3줄 (선택)
```

- 요약은 한국어, 50자 내외, 명사형으로 끝냅니다. (예: `feat: 버스 즐겨찾기 API 구현`)
- 요약에 "및", "그리고"가 들어가면 커밋을 나눠야 한다는 신호입니다.
- 관련 이슈가 있으면 본문 마지막 줄에 `#이슈번호`를 적습니다.

### type

| type | 언제 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 그대로, 구조 개선 |
| `test` | 테스트만 추가·수정 |
| `docs` | 문서 (README, 이 파일 등) |
| `chore` | 빌드·설정·의존성·배포 (`build.gradle`, `Dockerfile`, `.github` 등) |
| `style` | 포맷·오타 등 동작과 무관한 변경 |

### 단위

- **커밋 하나 = 기능 하나.** 한 기능의 Entity·Repository·Service·Controller·DTO·테스트는 한 커밋으로 묶어도 됩니다.
- 서로 다른 기능, 기능 추가와 리팩터링, 기능 코드와 설정만의 변경은 나눕니다.
- 판단 기준: "이 커밋만 되돌렸을 때 말이 되는가?"

커밋 템플릿을 쓰려면 한 번만 설정하세요.

```bash
git config commit.template .github/.gitmessage
```

## 3. Pull Request

- base는 **`develop`**, 제목은 대표 커밋과 같은 형식 (`feat: 대중교통 즐겨찾기(기능 4) API 구현`)
- 본문은 PR 템플릿을 채웁니다. 해당 없는 칸은 지우지 말고 "없음"이라고 적습니다.
- PR을 올리기 전에:
  - [ ] 최신 `develop`을 반영 (`git fetch origin && git merge origin/develop`)
  - [ ] `./gradlew test` 통과
  - [ ] 키·비밀번호가 커밋에 없음
- 관련 이슈는 본문에 `Closes #이슈번호`로 연결합니다. 머지되면 이슈가 자동으로 닫힙니다.
- 리뷰어 1명 이상 승인 후 머지합니다. 머지한 작업 브랜치는 삭제합니다.
- 배포에 영향이 있는 변경(새 환경변수, 새 테이블, 의존성)은 본문 "배포 시 필요"에 반드시 적습니다.

## 4. 이슈

- 새 작업은 이슈부터 만듭니다. **New issue**에서 템플릿을 고릅니다.
  - **기능 요청**: 새 기능·API
  - **버그 리포트**: 잘못 동작하는 것
  - **작업**: 설정·리팩터링·문서 등 그 외
- 제목은 `[기능]`, `[버그]`, `[작업]` 말머리 뒤에 한 줄 요약을 씁니다. (예: `[기능] 버스 도착 3분 전 알림`)
- 담당자(Assignee)를 지정하고 작업 브랜치를 만듭니다.
- 진행 상황과 결정 사항은 이슈 댓글에 남깁니다.

## 5. 비밀값

- API 키·DB 비밀번호는 절대 커밋하지 않습니다.
  - 내 PC: `application-local.properties` (git-ignore, `application-local.properties.example`을 복사해서 사용)
  - 배포 서버: Cloudtype 환경변수
- 실수로 커밋했다면 바로 키를 재발급하고 팀에 알립니다. 커밋을 지워도 기록은 남습니다.
