#!/usr/bin/env bash
# MOEMI 프론트엔드(wakeupjunyoung/MOEMI-FE) 로컬 체크아웃을 만들거나 최신화한다.
# 사용법: bash .claude/skills/moemi-fe/scripts/sync-fe.sh [브랜치]
set -uo pipefail

REPO="${MOEMI_FE_REPO:-wakeupjunyoung/MOEMI-FE}"
DEST="${MOEMI_FE_DIR:-.fe-cache/MOEMI-FE}"
BRANCH="${1:-}"

mkdir -p "$(dirname "$DEST")"

if [ ! -d "$DEST/.git" ]; then
  echo "== clone $REPO -> $DEST"
  if command -v gh >/dev/null 2>&1; then
    gh repo clone "$REPO" "$DEST" -- --quiet || exit 1
  else
    git clone --quiet "https://github.com/$REPO" "$DEST" || exit 1
  fi
else
  echo "== fetch $DEST"
  git -C "$DEST" fetch --quiet --prune origin || exit 1
fi

# 빈 레포(커밋 0개) 여부 확인
if ! git -C "$DEST" rev-parse --verify --quiet HEAD >/dev/null && \
   [ -z "$(git -C "$DEST" ls-remote --heads origin 2>/dev/null)" ]; then
  echo "!! $REPO 에 커밋이 없습니다(빈 레포). 프론트 코드가 푸시된 뒤 다시 실행하세요."
  exit 3
fi

# 대상 브랜치 결정: 인자 > 원격 기본 브랜치
if [ -z "$BRANCH" ]; then
  BRANCH="$(git -C "$DEST" symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##')"
fi
if [ -z "$BRANCH" ]; then
  BRANCH="$(git -C "$DEST" ls-remote --symref origin HEAD 2>/dev/null | sed -n 's#^ref: refs/heads/\([^\t ]*\).*#\1#p' | head -1)"
fi
[ -z "$BRANCH" ] && BRANCH=main

# 빈 레포로 클론된 경우 로컬 브랜치가 없으므로 origin 기준으로 직접 만든다
git -C "$DEST" checkout --quiet -B "$BRANCH" --track "origin/$BRANCH" 2>/dev/null \
  || git -C "$DEST" checkout --quiet "$BRANCH" 2>/dev/null \
  || { echo "!! 브랜치 '$BRANCH' 를 찾을 수 없습니다. 사용 가능한 브랜치:"; \
       git -C "$DEST" ls-remote --heads origin | sed 's#.*refs/heads/#  - #'; exit 4; }
git -C "$DEST" merge --quiet --ff-only "origin/$BRANCH" 2>/dev/null

echo
echo "== HEAD"
git -C "$DEST" log -1 --format='%h %ad %an %s' --date=short
echo
echo "== 최근 변경 파일 (20)"
git -C "$DEST" log -20 --name-only --format='' | sort -u | grep -v '^$' | head -30
echo
echo "== 소스 트리 (node_modules/빌드 제외)"
git -C "$DEST" ls-files \
  | grep -Ev '^(node_modules|dist|build|\.next|public/assets)/' \
  | grep -Ev '\.(png|jpe?g|gif|svg|ico|woff2?|ttf|lock)$' \
  | head -200
echo
echo "== 경로: $DEST"
