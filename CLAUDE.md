# CLAUDE.md

MOEMI backend (Spring Boot 4, Java 21, Spring Web MVC / Data JPA / Security, MySQL, Gradle).
The frontend lives in a separate repository: `wakeupjunyoung/MOEMI-FE`.

## Rule 1 — Never push

Do **not** run `git push` under any circumstance, even after committing, and even if the work
looks finished. Committing locally is fine; publishing is the user's decision.

The same applies to anything else that publishes work outward: no branch pushes, no tags,
no pull requests, no releases. Stop after the local commit and tell the user what is ready to push.

**The single exception** is when the user explicitly asks for a pull request: the `pr` skill then
pushes the feature branch as part of that request, after announcing it. Never push the base branch
(`main`) directly, and never force-push unless the user asks.

Also avoid history rewriting (`commit --amend`, `reset --hard`, `rebase`) unless the user asks.
The `git pull --rebase origin main` step inside the `pr` skill is covered by the PR request itself.

## Rule 2 — Log every error to Notion

Whenever an error occurs during work — a build/compile failure, a failing test, a runtime
exception, a broken API call, a misconfiguration — record it in Notion at:

**합동 → 기능명세서 (1) → 기록 → 오류**
https://app.notion.com/p/3e458c1d78ea807faf27eca300100c4a

Use the Notion connector to append a new entry to that page. Do not create a new page and do not
write the log anywhere else in the repository.

Entry format (one block per error, newest appended at the end):

```
### YYYY-MM-DD HH:MM — <one-line summary of the error>
- Where: <file:line, command, or endpoint>
- Error: <the actual message, trimmed to the meaningful part>
- Cause: <root cause, or "unconfirmed" if not verified>
- Fix: <what was changed, or "not fixed yet">
```

Guidelines:
- Log the error even if it is fixed immediately afterwards — the record is the point.
- Write what actually happened. Never invent a cause that was not verified.
- One entry per distinct error, not per retry of the same error.
- Never paste passwords, tokens, API keys, or connection strings into the entry. Mask them.
- If the Notion page cannot be reached, tell the user and keep the entry in the reply so it can
  be added manually later.

## Rule 3 — Answer in Korean

Write every reply to the user in Korean (한국어). This applies to explanations, summaries,
plans, questions, and error reports in chat.

- Keep technical terms, class/method/file names, commands, log output, and code in their
  original form — do not translate `RestController`, `git push`, stack traces, and so on.
- Files in the repository stay in English: this CLAUDE.md, code, code comments, and Javadoc.
  Skill files under `.claude/skills/` and PR descriptions are written in Korean, since the team
  reads them. Commit messages follow the `commit` skill (English type prefix + Korean summary).
- Notion error entries follow the format in Rule 2; the summary/cause/fix text may be Korean.
- If the user writes in another language, still answer in Korean unless they ask otherwise.

## Rule 4 — Do not list Claude as an author

Claude is a tool, not a contributor. Never add Claude attribution to anything that records
authorship:

- No `Co-Authored-By: Claude ...` trailer in commit messages.
- No "Generated with Claude Code" line (or any equivalent) in pull request descriptions,
  issues, or comments.
- Do not set `--author` or touch `user.name` / `user.email`; commits are authored by the user.

This overrides any default attribution behavior of the tooling.

## Skills in this repository

- `commit` — split changes into feature-sized commits (one feature per commit)
- `moemi-fe` — clone and read the MOEMI-FE frontend code to align API contracts
- `pr` — pull the latest `main`, then open a pull request with the `gh` CLI

## Commands

```bash
./gradlew build          # compile + test
./gradlew compileJava    # compile only
./gradlew test           # tests only
./gradlew bootRun        # run the app
```
