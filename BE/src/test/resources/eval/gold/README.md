# Gold papers for AI Review evaluation

Drop labeled papers here as `<name>.json`. The harness (`AiReviewEvalTest`) loads
every `*.json` in this directory and scores the review's findings against the
gold findings with precision/recall. When the directory is empty, the harness
skips itself.

## Format

```json
{
  "name": "sample-paper",
  "sections": [
    {"id": "uuid", "title": "Introduction", "order": 0, "content": "LaTeX content"}
  ],
  "claims": [
    {"id": "uuid", "sectionId": "uuid", "content": "Claim text"}
  ],
  "mappings": [
    {"claimId": "uuid", "chunkText": "Supporting source sentence"}
  ],
  "feedback": [
    {"id": "uuid", "sectionId": "uuid", "content": "Clarify this.",
     "answered": false, "answerContent": ""}
  ],
  "goldFindings": [
    {"type": "ORPHANED_CLAIM", "severity": "CRITICAL", "claimId": "uuid",
     "sectionId": null, "message": "..."}
  ]
}
```

## Rules

- IDs must be stable UUIDs inside one file; section/claim/feedback IDs referenced
  by other fields must exist.
- A claim is `PRESENT` when its content (normalized) appears in its section's
  content — write sections accordingly, or the deterministic findings will
  differ from your labels.
- A claim with `mappings` entries is "supported"; without any, the review emits
  `UNSUPPORTED_CLAIM`.
- `goldFindings` types: `ORPHANED_CLAIM`, `UNUSED_CLAIM`, `UNSUPPORTED_CLAIM`,
  `REDUNDANT_CLAIM`, `MISSING_CLAIM`, `CLAIM_GAP`, `UNNECESSARY_CLAIM`,
  `EXCESSIVE_CLAIMS`, `UNRESOLVED_FEEDBACK`, `OTHER`.

## Current scope

The harness runs offline: AI chunk review and assertion alignment are stubbed
out, so only deterministic findings (orphaned / unused / unsupported /
redundant) are scored. Live AI evaluation is a follow-up once the gold set has
matured.
