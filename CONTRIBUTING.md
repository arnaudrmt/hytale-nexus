# Contributing to Nexus

Thanks for contributing! This guide keeps PRs easy to review and the project easy to maintain.

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
- `fix(system): prevent dash during combo window`

## Before Opening a PR

- [ ] Branch is named correctly (`feat/`, `fix/`, or `refactor/`)
- [ ] Commits follow the format above
- [ ] Code compiles with no errors
- [ ] PR title matches your commit format (`feat(system): ...`)
- [ ] PR description explains *what* changed and *why*

## What Goes Where

| You're adding/changing...                | Scope                                                 |
|------------------------------------------|-------------------------------------------------------|
| A new ability, enchant, or system class  | `system`                                              |
| Weapon stats, mob configs, level layouts | `asset`                                               |
| Both at once                             | Use the primary one; mention the other in the PR body |

## Keep PRs Small

One change per PR. A PR that adds a new enchant AND refactors the strike system is two PRs.
