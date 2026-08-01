---
name: numista-draft
description: Research and prepare sourced, non-submitting Numista catalogue contribution drafts. Use for creating or editing coin types, revising pending submissions, proposing series, adding date lines or varieties, resolving an ambiguous Numista request, and producing a field-by-field dossier for visible browser review without saving, uploading, or submitting.
---

# Numista Draft

Prepare evidence and a human-reviewable draft. Never make the contribution itself.

## Repository scope

- Run only inside the `jenarvaezg/coindex` Git repository.
- Confirm the repository root contains `spec.md`, `data/numista-type-cache.json`, and `scripts/record-fixture.py` before doing research or browser work.
- If those markers are absent, stop and explain that this is a Coindex project skill; do not continue against another repository.
- Store generated dossiers and renders under `/private/tmp`, never in the repository unless the user explicitly requests a versioned artifact.

## Hard safety boundary

- Never click or invoke Save, Upload, Submit, Send, Publish, Confirm, or equivalent controls.
- Never upload an image or other file, even when its rights are verified.
- Never call a mutation endpoint. The public Numista API is for catalogue lookup; treat all API use as read-only.
- Open only a visible, interactive browser for final review and optional prefill. Do not use headless browser automation for the handoff.
- Stop after prefilling. Tell the user what remains for their own review and manual action.

These restrictions apply even if the user asks to “finish”, “publish”, or “submit”.

## Workflow

1. Read [references/numista-workflow.md](references/numista-workflow.md) completely before research or browser work.
2. Classify exactly one action:
   - `create_type`: a genuinely missing physical/design type.
   - `edit_type`: corrections to a published catalogue type.
   - `edit_pending_submission`: changes to an existing, still-pending request. Never describe this as publishing.
   - `propose_series`: a structural grouping proposed through the forum/administrators, not the new-type form.
   - `add_dateline_or_variety`: another date, mint, strike, or recognised variety on an existing type.
   - `ambiguous`: evidence does not yet distinguish the routes. Keep the dossier not ready.
3. Search repository caches, fixtures, downloaded issue artifacts, and existing dossiers first. Record which local artifacts were inspected.
4. Research broadly. Prefer official mint, issuer, legal-gazette, museum, and Numista guideline sources; corroborate with recognised catalogues and reputable auction archives. Record evidence per field, not as one undifferentiated bibliography.
5. Perform a fresh duplicate check across `coins`, `exonumia`, `unverified`, and `pending_submissions`. Use several identifying queries and record the date, scopes, queries, and result.
6. Plan API calls only for gaps that local artifacts and ordinary web research cannot resolve. State the exact endpoints and number of calls, render the plan hash, then obtain explicit user confirmation for that exact hash before making any call. Never trust `confirmed_by_user` inside the dossier itself. A zero-call plan still records `requires_confirmation: true`.
7. Build the JSON dossier described in the reference. Mark every field `verified`, `unknown`, `inferred`, `conflict`, or `not_applicable`. Only `verified` fields may support a ready draft; each needs one or more direct sources.
8. Evaluate and render:

   ```bash
   python3 <skill-directory>/scripts/render_draft.py dossier.json --today YYYY-MM-DD --require-ready -o new-draft.md
   ```

   If the dossier plans API calls, rerun only after the user approves the hash printed in the first render, passing it as `--approve-api-plan-digest HASH`. The output path must be a new file.

9. If ready, open the recorded route in a visible browser and prefill only:
   - Prefer the Codex in-app browser when it connects successfully.
   - If browser bootstrap fails before navigation, including `missing field sandboxPolicy`, do not retry or reinstall plugins during the run. Use the repository-scoped `numista_review_browser` Playwright MCP fallback. It is configured as headed Chrome in `.codex/config.toml` for Codex CLI and `.mcp.json` for Claude Code.
   - If that MCP server is unavailable because its local runtime is absent, obtain permission to run `scripts/setup_review_browser.sh`, then tell the user to restart Codex or Claude so the project server is reloaded. Do not install anything silently.
   - Claude requires the user to approve project-scoped MCP servers from `.mcp.json`. If `numista_review_browser` is pending approval, ask the user to approve it in Claude Code and retry; never bypass Claude's permission system.
   - Never replace this fallback with a headless browser. If neither visible browser is available in the current session, keep the dossier ready and stop before prefilling.
   - The persistent browser profile is `.playwright-mcp/numista-review-profile`. Let the user log in manually when needed. Only one Codex or Claude session may control this profile at a time; if it is locked, stop and ask the user to close the other review-browser session.
   - For `create_type`, use `https://en.numista.com/catalogue/contributions/nouveau.php?type=coin` as the initial route. If it has changed, navigate from the current Numista help page or relevant catalogue entry instead of guessing a replacement URL.
10. Leave the page open for the user. Do not save, upload, or submit.

## Readiness rules

Keep the draft `not_ready` when any required field is absent, unknown, inferred, conflicting, or lacks a direct source; when open questions/conflicts remain; when the duplicate check is stale or incomplete; when an API budget lacks explicit approval; when image rights are unclear; or when the route remains ambiguous. A blocked render is a research report only and must not be opened or prefilled in the browser.

Images are optional when descriptions and lettering adequately identify both sides. Record external pictures as research evidence only unless authorship, an authorised Numista source, public domain, or a compatible licence is verified. Regardless of rights, never upload them.

Render a field table with value, status, and source links; date-line evidence; duplicate-check details; blockers; and the exact browser warning: “Abrir y rellenar; no guardar, subir ni enviar.”
