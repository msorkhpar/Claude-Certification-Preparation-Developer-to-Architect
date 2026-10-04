---
paths:
  - "terraform/**/*"
---

# Terraform conventions

- Every resource carries the `owner` and `cost-centre` tags.
- Run `terraform fmt` and `terraform validate` before proposing a change.
- Pin provider versions with `~>` constraints.
