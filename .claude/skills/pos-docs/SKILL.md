---
name: pos-docs
description: How to keep project docs in sync: spec, plan, decision log, README index. Use after any architectural or scope change, or when the user asks to note or document something.
---

# Maintaining docs

- `docs/tech-spec.md`: current truth for architecture, stack, model, scope. Edit in place; do not append history.
- `docs/plan.md`: phases and status checkboxes. Tick items as phases complete.
- `docs/decisions.md`: add a row (`D-00n`, Open/Decided) and a dated line in the log whenever a choice is made or changed. Date format `YYYY-MM-DD`.
- `README.md`: keep the docs and skills tables accurate when files are added.
- New skill: create `.claude/skills/<name>/SKILL.md` with `name` and a one-line `description` stating when to use it, then add it to the README skills table.
- Keep docs short and factual; link rather than duplicate.
