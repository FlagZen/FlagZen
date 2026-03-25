# Interview Guide -- FlagZen

## Discovery Context

- **Feature ID**: flagzen
- **Date**: 2026-03-25
- **Purpose**: Guide for conducting Mom Test interviews with Java developers to validate assumptions from ecosystem-based discovery
- **Priority**: Validate A1, A2, A4, A7, A8 (highest-risk assumptions)

## Why Interviews Are Still Needed

The discovery artifacts in this directory are based on ecosystem analysis (public signals, competitive analysis, API reviews), not direct developer interviews. This is sufficient to start building an MVP, but the following assumptions carry the most risk and should be validated through conversation before investing heavily:

|                   Assumption                    | Risk Score |             Question It Answers              |
| ----------------------------------------------- | ---------- | -------------------------------------------- |
| A1: Multi-provider pain justifies a new library | 17         | Is the pain acute enough to change behavior? |
| A2: Polymorphic dispatch is compelling          | 17         | Will devs learn a new pattern for this?      |
| A4: Devs will learn annotation-based API        | 15         | Is the learning curve acceptable?            |
| A7: OpenFeature leaves enough DX gaps           | 15         | Is there room for FlagZen?                   |

## Interview Structure

**Duration**: 20-30 minutes
**Format**: Casual conversation (Slack DM, video call, coffee)
**Target**: Java developers who actively use feature flags in production

### Opening (2 min)

"I'm working on an open-source library for feature flags in Java and want to understand how developers actually work with flags. I'm not trying to sell anything -- I genuinely want to learn about your experience. There are no right or wrong answers."

### Part 1: Current Behavior (10 min)

These questions establish past behavior -- the most reliable evidence.

1. "Tell me about the last time you added a feature flag to your codebase. Walk me through what you did."
   - Listen for: provider choice, API used, setup effort, pain points mentioned unprompted

2. "What does your feature flag setup look like today? Which tools/libraries do you use?"
   - Listen for: single vs. multiple providers, env vars as flags, custom wrappers

3. "Tell me about the last time you wrote a test for code that depends on a feature flag. What did that look like?"
   - Listen for: mocking complexity, setup lines, frustration signals, workarounds

4. "Have you ever switched or considered switching flag providers? What happened?"
   - Listen for: migration stories, coupling pain, reasons for staying, lock-in awareness

5. "What's the most annoying thing about working with feature flags in your current project?"
   - Listen for: unprompted themes matching O1-O7

### Part 2: Probing Specific Pain Points (10 min)

Only ask these if the developer hasn't already covered them.

6. "How do you handle the code branches for a feature flag? If/else, strategy pattern, something else?"
   - Listen for: awareness of conditional sprawl, tolerance level, any attempts at polymorphism

7. "When a feature is fully rolled out, how do you remove the flag? What's that process like?"
   - Listen for: dead flag awareness, removal friction, "we never clean them up"

8. "Have you ever had a bug caused by a flag configuration error -- like a typo in a flag name?"
   - Listen for: runtime error stories, desire for compile-time safety

9. "How do you handle feature flags that need to vary by user, tenant, or request context?"
   - Listen for: A/B testing patterns, context propagation pain, reactive challenges

### Part 3: Show and Tell (5 min)

Only after understanding their current experience. Show the FlagZen API.

10. "Here's a code snippet showing a different approach. [Show @Feature/@Variant example]. What's your first reaction?"
    - Listen for: "that's cool" (compliment -- discount), "I'd use that for X" (concrete use case -- valuable), confusion (usability risk)

11. "Here's how testing would work. [Show @PinFlag example]. How does that compare to what you do today?"
    - Listen for: concrete comparison to their current setup, excitement vs. indifference

### Part 4: Commitment Testing (3 min)

12. "If this library existed today on Maven Central, would you try it in your project?"
    - Discount: "yeah, sure" (politeness). Value: specific project/use case mentioned.

13. "What would need to be true for you to add this as a dependency?"
    - Listen for: dealbreakers, must-haves, framework requirements

14. "Could you introduce me to another Java developer on your team who works with feature flags?"
    - This is the real commitment signal. A referral is worth more than any verbal enthusiasm.

## Interview Anti-Patterns to Avoid

|                      Do NOT                       |                  Do Instead                   |
| ------------------------------------------------- | --------------------------------------------- |
| "Would you use a library that..."                 | "Tell me about the last time you..."          |
| "Don't you think if/else is messy?"               | "How do you handle flag branches?"            |
| "FlagZen solves this by..."                       | Let them describe the problem first           |
| Show the solution before understanding their pain | Always Part 1 before Part 3                   |
| Accept "that's awesome!" as validation            | Ask "what specifically would you use it for?" |

## Signal Tracking Template

After each interview, record:

```markdown
### Interview #N -- [Date]

**Who**: [Role, team size, flag usage frequency]
**Flag tools**: [What they use today]
**Key pain points** (in their words): [Direct quotes]
**Testing approach**: [How they test flag-dependent code]
**Reaction to FlagZen API**: [Specific, not "liked it"]
**Commitment signal**: [Referral? Specific use case? Follow-up request?]
**Surprising insight**: [Anything unexpected]
**Assumption impact**: [Which assumptions supported/challenged]
```

## Target Interview Count

|               Phase                | Minimum | Target |           Current           |
| ---------------------------------- | ------- | ------ | --------------------------- |
| Problem validation (retrospective) | 5       | 8      | 0 (ecosystem evidence used) |
| Solution feedback                  | 5       | 8      | 0                           |
| Post-MVP usability                 | 5       | 10     | 0                           |

**Recommendation**: Conduct 5-8 interviews before or during early MVP development. Focus on Java developers in your professional network who use feature flags. Even 5 solid interviews will dramatically improve confidence in A1, A2, and A4.
