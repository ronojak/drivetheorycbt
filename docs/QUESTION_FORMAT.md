# Question Bank Format

Place one or more files under `app/src/main/assets/seed/`. The app merges all files and de‑duplicates by `id`.

Supported formats
- JSON array (`*.json`):
```
[
  { "id": "q1", "text": "...", "options": ["A","B","C","D"], "correctIndex": 0, "category": "...", "difficulty": 1 },
  { "id": "q2", ... }
]
```
- JSON Lines (`*.jsonl`): one JSON object per line with the same fields as above.
 - CSV (`*.csv`): header can include `id`, `text` (or `question`), `option1..N` or `optionA..E` (or `A..E`), `correctIndex` (0/1‑based) or `correct/answer` (letter or exact option text), optional `category`, `difficulty`.

Fields
- `id` (string, unique), `text` (string), `options` (array of 2–6 strings),
- `correctIndex` (0‑based), optional `category` (string), optional `difficulty` (int).

Tips
- Split large banks across multiple files, e.g., `questions_signs.json`, `questions_rules.json`, `questions_safety.jsonl`.
- To update, drop new files or replace existing ones; on app start they are merged automatically.
- Keep `id` stable to avoid duplicates; last file wins on duplicate ids.
 - Precedence: CSV loads first and JSON/JSONL override it for the same `id` (JSON is treated as most accurate).

Example locations
- `app/src/main/assets/seed/questions_seed.json` (starter set)
- `app/src/main/assets/seed/questions_mybank.json`
- `app/src/main/assets/seed/questions_extra.jsonl`
