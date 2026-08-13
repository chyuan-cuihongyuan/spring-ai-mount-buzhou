# Issue tracker: Local Markdown

Issues and specs for this repo live as markdown files in **per-effort directories**. Durable efforts (wayfinder maps, specs, impl slices) live in **`.wayfinder*/` effort dirs** (committed to git): `.wayfinder/`（effort #1，已闭合）、`.wayfinder2/`（effort #2「做完美」，当前）。`.scratch/` is gitignored and only for throwaway drafts — do not put durable issues there.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`
- The spec is `.scratch/<feature-slug>/spec.md`
- Implementation issues are one file per ticket at `.scratch/<feature-slug>/issues/<NN>-<slug>.md`, numbered from `01`
- Triage state is recorded as a `Status:` line near the top of each issue file
- Comments and conversation history append to the bottom of the file under a `## Comments` heading

## When a skill says "publish to the issue tracker"

Create a new file under `.scratch/<feature-slug>/` (creating the directory if needed).

## When a skill says "fetch the relevant ticket"

Read the file at the referenced path. The user will normally pass the path or the issue number directly.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a file with one **child** file per ticket. Current convention (see the effort dir's `README.md` for frontmatter fields):

- **Map**: `.wayfinder*/MAP.md` — the Notes / Decisions-so-far / Fog body.
- **Child ticket**: `.wayfinder*/tickets/T<n>-<slug>.md`（编号在 effort 间全局续用，如 `.wayfinder2/` 从 T28 起）, with the question in the body. A `Type:` line records the ticket type (`research`/`prototype`/`grilling`/`task`); a `Status:` line records `open`/`closed`.
- **Blocking**: a `blocked-by:` line in frontmatter. A ticket is unblocked when every ticket it lists is `closed`.
- **Frontier**: scan the effort's `tickets/` for files that are open, unblocked, and unclaimed; first by number wins.
- **Claim**: set `assignee` and save before any work.
- **Resolve**: append the answer under a `## Resolution` heading, set `status: closed`, then append a context pointer (gist + link) to the map's Decisions-so-far in `MAP.md`.
