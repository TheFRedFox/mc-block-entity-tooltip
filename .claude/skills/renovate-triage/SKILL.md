---
name: renovate-triage
description: >-
  Triage open Renovate dependency PRs for this Fabric mod. Use when the user asks
  to "check the renovate PRs", "triage dependency updates", "what should we do with
  the renovate PRs", "review the dependency dashboard", or wants a recommendation
  on which dependency bumps to merge, close, or flag. Reads version pins from
  gradle.properties and renovate.json at runtime, classifies each PR against the
  project's Dependency Triage Rules in CLAUDE.md, and outputs a decision table.
  Produces a plan only; never merges or closes without explicit authorization.
---

# Renovate Triage

Classify open Renovate dependency PRs and emit a decision plan.

**Policy lives in `CLAUDE.md` → `## Dependency Triage Rules`. That section is the
single source of truth. This file is procedure only — read CLAUDE.md first and
apply its rules; do not restate or reinvent policy here.**

## Step 0 — Load policy + state (every run; never hardcode versions)

1. Read `CLAUDE.md`, section `## Dependency Triage Rules`. Hold its Version policy,
   Minecraft-line pinning, Classification outcomes, and Authorization rules in mind
   for the whole run.
2. Read `gradle.properties`. Extract: `minecraft_version` (the current MC line),
   `fabric_version` (note its `+<mc>` suffix), `modmenu_version`,
   `cloth_config_version`, `fabric_kotlin_version`, `loader_version`, `mod_version`.
3. Read `build.gradle` for versions not in `gradle.properties` (Fabric Loom, Kotlin
   plugin, mod-publish-plugin).
4. Read `renovate.json`. Parse the two `allowedVersions` regex pins — one for
   `net.fabricmc.fabric-api:fabric-api`, one for the `custom.mojang-minecraft`
   datasource. Treat these regexes as the authoritative MC-line constraint; parse
   them from the file, never assume their contents.

If `gradle.properties` or `renovate.json` cannot be read, stop and report — do not
guess versions.

## Step 1 — Enumerate

1. `gh pr list --state open --json number,title,headRefName,author,url`
2. `gh issue view 5 --json title,body` — the Renovate **Dependency Dashboard**
   (keep that label beside the number; if issue 5 is not the dashboard, fall back
   to `gh issue list --search "Dependency Dashboard in:title" --json number`).

If there are **zero open PRs**: say so explicitly, give a short sanity summary
comparing the dashboard's "Detected Dependencies" against the current pins (flag
anything behind a pin), then stop. Nothing to triage, nothing to authorize.

## Step 2 — Inspect each PR

Prefer Renovate-authored PRs (author `app/renovate` or branch prefixed
`renovate/`). For each:

1. `gh pr view <n> --json title,body,files`; use `gh pr diff <n>` for the exact
   version delta in `gradle.properties` / `build.gradle` / workflow files.
2. Renovate groups deps (Minecraft & Fabric / Kotlin / GitHub Actions / Gradle
   plugins) — a single PR may bump several. Classify **per dependency**; note when
   one PR mixes outcomes.

## Step 3 — Classify

Apply the **Classification outcomes** from `CLAUDE.md → Dependency Triage Rules`,
one outcome per dependency, using values read in Step 0:

- proposed ≤ current in `gradle.properties`/`build.gradle` → **Already applied manually**
- Fabric API whose `+<suffix>` ≠ current `minecraft_version`, or any version failing
  the relevant `renovate.json` `allowedVersions` regex → **Wrong MC line**
- pre-release (`alpha`/`beta`/`rc`/`snapshot`/`-pre`): if dependency is
  `com.terraformersmc:modmenu` and it is a forward step toward stable vs. current
  `modmenu_version` → **Valid stable update** (call out the ModMenu exception);
  otherwise → **Pre-release**
- SemVer major bump or MC API tier change → **Major update**
- otherwise stable, within pins, newer → **Valid stable update**

When ambiguous between Valid and Major, choose **Major** (flag for review) — never
silently upgrade ambiguity into a merge. If a PR vanishes mid-run (Renovate
auto-close of an already-applied bump), note it and continue.

## Step 4 — Output

Emit one Markdown table, then a prose summary:

| PR | Dependency | Current | Proposed | Classification | Recommended action | Why |
|----|-----------|---------|----------|----------------|--------------------|-----|

Fill `Current`/`Proposed` with values read this run. In the summary: group merge
candidates; list Major / Wrong-MC / Pre-release separately, each with a one-line,
paste-ready rationale suitable for a PR-close comment.

## Step 5 — HARD STOP (authorization gate)

Run **no** mutating command: no `gh pr merge`, `gh pr close`, `gh pr comment`,
`gh pr review`, no branch push, no editing of `renovate.json` /
`gradle.properties` / `CLAUDE.md`.

End with the table + summary and an explicit prompt, e.g. "This is a plan only —
tell me which rows to act on and I'll execute them individually." Only after the
user authorizes specific PRs may you run the corresponding mutating commands, one
authorized batch at a time, re-confirming if the set is ambiguous.
