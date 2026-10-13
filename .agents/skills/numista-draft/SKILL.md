---
name: numista-draft
description: Research and prepare sourced Numista catalogue contribution drafts, sent only on the user's explicit order. Use for creating or editing coin types, revising pending submissions, proposing series, adding date lines or varieties, resolving an ambiguous Numista request, and producing a field-by-field dossier for visible browser review before anything is saved, uploaded, or submitted.
---

# Numista Draft

Prepare evidence and a human-reviewable draft. Never make the contribution on your own initiative.

## Repository scope

- Run only inside the `jenarvaezg/coindex` Git repository.
- Confirm the repository root contains `spec.md`, `data/numista-type-cache.json`, and `scripts/record-fixture.py` before doing research or browser work.
- If those markers are absent, stop and explain that this is a Coindex project skill; do not continue against another repository.
- Store generated dossiers and renders under `/private/tmp`, never in the repository unless the user explicitly requests a versioned artifact.

## Hard safety boundary

- Never click or invoke Save, Upload, Submit, Send, Publish, Confirm, or equivalent controls.
- Never call a mutation endpoint. The public Numista API is for catalogue lookup; treat all API use as read-only.
- Open only a visible, interactive browser for final review and optional prefill. Do not use headless browser automation for the handoff.
- Stop after prefilling. Tell the user what remains for their own review and manual action.

One exception: when the user, in the current turn, explicitly orders a specific prefilled form to be sent, attach its pictures (see [Pictures](#pictures)), re-read its fields in a fresh snapshot and press that form's own submit control (for example «Modify data»). The order covers that form and its pictures only; never other forms or later turns. An order to add pictures to the user's own pending submission covers that edit the same way.

After a form is sent, take the request id from `confirmation.php?…&prop[]=<id>`, add the `waiting-numista` label to the issue, and comment a table `N# | coin | change | request` linking `voir_prop.php?id=<id>`.

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
10. Leave the page open for the user. Do not save, upload, or submit unless the exception above applies.

## Pictures

- Upload a picture only when its `rights_basis` is verified in the dossier, and set Numista's picture source and credit to match it (for example «I copied the picture from a website that gave their permission» plus the authorised site).
- The review browser only reads files under the checkout that launched it: download each picture to its `.playwright-mcp/uploads/` (git-ignored). A `File access denied` error names the allowed roots.
- Click the form's file control, then call `browser_file_upload` with the absolute path. The click opens a file chooser that only that call closes; the user cannot pick a file by hand in that browser.
- Open the report with each side's picture as uploaded or missing.

## Readiness rules

Keep the draft `not_ready` when any required field is absent, unknown, inferred, conflicting, or lacks a direct source; when open questions/conflicts remain; when the duplicate check is stale or incomplete; when an API budget lacks explicit approval; when image rights are unclear; or when the route remains ambiguous. A blocked render is a research report only and must not be opened or prefilled in the browser.

Images are optional when descriptions and lettering adequately identify both sides. Record external pictures as research evidence only unless authorship, an authorised Numista source, public domain, or a compatible licence is verified. A verified picture is uploaded under the exception above.

Render a field table with value, status, and source links; date-line evidence; duplicate-check details; blockers; and the exact browser warning: “Abrir y rellenar; no guardar, subir ni enviar.”
