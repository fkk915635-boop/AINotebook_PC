---
name: "web-interface-guidelines"
description: "Audits UI code against Vercel Web Interface Guidelines (design + accessibility). Invoke when user asks to review UI/UX quality, accessibility, or wants a guideline-based audit of frontend files."
---

# Web Interface Guidelines

This skill reviews frontend UI code against Vercel’s Web Interface Guidelines and outputs actionable findings.

## Source of Truth

Fetch the latest guidelines before each review from:

https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md

## When to Invoke

Invoke when the user asks for any of the following:
- UI/UX review, design polish, consistency improvements
- Accessibility review (keyboard, focus, contrast, semantics)
- “Audit” or “check against guidelines”
- Pre-merge UI review for a page/component

## Inputs

Ask for one of:
- A single file path (preferred)
- A list of file paths
- A glob/pattern and target directory (e.g. `ainotebook-frontend/src/**`)

Default suggestion for this repo:
- `AINotebook_Java/ainotebook-frontend/src/**`

## Procedure

1. Fetch the guidelines markdown from the URL above.
2. Identify the guideline categories and the expected output format described in the guidelines.
3. Read the specified target files (or prompt for them if none were provided).
4. Audit the code and UI behavior implied by the code:
   - Semantics: headings, labels, button types, form structure
   - Accessibility: focus states, keyboard navigation, color contrast, aria where needed
   - UX: loading states, error states, empty states, motion preference
   - Visual consistency: spacing, typography, color usage, alignment
   - Performance: avoid heavy assets, unnecessary reflows, excessive animation cost
5. Output findings in a terse `file:line` format when possible:
   - `file_path:line - Finding. Fix: ...`
   - If line numbers aren’t available, provide `file_path - Finding. Fix: ...`
6. For each finding, propose a concrete fix with minimal changes and consistent with the repo’s style.

## Output Rules

- Prefer short, high-signal findings (group duplicates).
- Prioritize issues by user impact: accessibility + correctness > UX polish > micro-style.
- Do not add comments to code unless the user explicitly asks.

## Example Invocation Prompts

- “用 Web Interface Guidelines 审查登录页：AINotebook_Java/ainotebook-frontend/src/views/Auth.vue”
- “按 Vercel UI guidelines 审查前端 src，并给出 file:line 的修改建议”

