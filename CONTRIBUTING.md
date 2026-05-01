# Contributing to Nexus

Thanks for contributing! This guide keeps PRs easy to review and the project easy to maintain.

## Branching Strategy

We use a two-branch system to ensure stability:

* **`main (default)`**: Contains the latest stable release. Do not target PRs here.
* **`develop`**: The integration branch for ongoing work. **All PRs should target this branch.**

## Branch Naming

All branches must follow this pattern:

| Prefix      | Use for                                         |
|-------------|-------------------------------------------------|
| `feat/`     | Adding something new (ability, mechanic, level) |
| `fix/`      | Fixing a bug or broken behavior                 |
| `refactor/` | Reworking or overhauling existing code          |

**Examples:** `feat/freeze-enchant`, `fix/dash-stamina-drain`, `refactor/strike-system`

## Commit Messages

Follow this format: `type(scope): short description`

**Scopes:**

- `system` — code (abilities, ECS systems, mechanics)
- `asset` — Hytale asset editor content (weapons, mobs, levels, configs)

**Examples:**

- `feat(system): add bloodlust enchant trigger`
- `fix(asset): correct wave 3 mob spawn count`
- `refactor(system): simplify strike FSM transitions`

## Before Opening a PR

- [ ] **Target Branch:** Ensure your PR is targeting the `develop` branch.
- [ ] **Branch Name:** Branch is named correctly (`feat/`, `fix/`, or `refactor/`).
- [ ] **Commits:** Follow the format above.
- [ ] **Quality:** Code compiles with no errors.
- [ ] **Metadata:** PR title matches your commit format and description explains *what* and *why*.

## What Goes Where

| You're adding/changing...                | Scope                                                 |
|------------------------------------------|-------------------------------------------------------|
| A new ability, enchant, or system class  | `system`                                              |
| Weapon stats, mob configs, level layouts | `asset`                                               |
| Both at once                             | Use the primary one; mention the other in the PR body |

## Keep PRs Small

One change per PR. If you are adding a new enchant AND refactoring the strike system, please submit them as two separate
Pull Requests.
