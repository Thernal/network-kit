# Agent skills

Skills for AI coding agents working in projects that **use** network-kit, in the
[Agent Skills](https://agentskills.io) layout.

| Skill | Use it when |
|---|---|
| [`network-kit`](network-kit/SKILL.md) | installing network-kit; writing API classes and repositories; errors and `NetworkResult`; sign-in, token refresh, guests and session expiry; public/optional routes; several hosts; envelopes; interceptors; conditional requests; testing with MockEngine |

```
network-kit/
├── SKILL.md                  model, orientation greps, rules, verification
└── references/
    ├── setup.md              modules, dependencies, Metro bindings (clients, TokenStore, TokenRefresher, routes)
    └── usage.md              calls, failures, sessions, interceptors, envelopes, cache, testing, troubleshooting
```
