# Changelog

What each version of the plugin changes for a SonarQube user.

The version of the plugin is that of the AdaLang Analyzer it is aligned with,
so an entry says what the reports of that analyzer change in Sonar: which
issues appear, disappear, or move to another rule or severity. What the
analyzer itself changed is in
[its own changelog](https://github.com/mmartign/AdaLang_Analyzer/blob/main/CHANGELOG.md).
Versions before 1.8.3 are in the git history.

## 1.10.2 - 2026-10-24

Aligned with AdaLang Analyzer 1.10.2. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line are
those of 1.10.1, and the plugin reads them with no change. Expect fewer
`unproved` issues and no new ones.

- **The `unproved` issues 1.10.1 brought back are gone again.** Checks inside
  or after a loop whose invariants are proved, which 1.10.0 proved and 1.10.1
  did not, are proved again.
- **Fewer `unproved` issues after loops in a row.** What a loop does not
  change keeps its bounds past it however many loops come before, and
  overflow and range checks that rest on them are proved.
- **Fewer `unproved` issues on components of a bounded subtype.** A subtype
  bound written with `/`, `mod` or `rem` over an attribute
  (`Positive range 1 .. Positive'Last / 8`) is known, and the overflow checks
  of arithmetic on such components are proved.
- No issue is new, and none changes rule or severity. Over the analyzer's 207
  `tests/verification_*` bodies, run with their specs, the JSON reports give
  the same 7,744 obligations as 1.10.1, 38 of them moving from `unproved` to
  proved safe and none the other way; the findings are 922, as before, and
  the issues on obligations 1,947 instead of 1,985.

## 1.10.1 - 2026-10-23

Aligned with AdaLang Analyzer 1.10.1. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line are
those of 1.10.0, and the plugin reads them with no change. Expect fewer issues
on loop invariants and on the checks that follow from them, and a few new ones
where 1.10.0 called safe what is not.

- **Fewer `unproved` issues.** 1.10.1 proves more loop invariants -- those of a
  `for` loop that name its parameter, and those of a loop with an `exit` or a
  `return` in it -- and keeps what is known across the declaration of a record
  or array object, so more of the checks that follow from them are proved.
- **New issues where 1.10.0 was wrong.** Two false-safes are corrected
  (`FP-129` and `FP-130` in the analyzer's changelog): a check after a loop
  that changes an object through a call, and the precondition of a call with
  no actual in parentheses (`Bump;`) inside a loop. Such checks that do not
  hold are `unproved` issues now, and a precondition that fails where it is
  checked is a `definite-error` issue with a `Known_Precondition_Failure`
  finding.
- **A few `unproved` issues come back.** Some checks inside or after a loop
  whose invariants are proved, which 1.10.0 proved soundly, are `unproved`
  again; a later version is to prove them anew.
- Over the analyzer's 205 `tests/verification_*` bodies, run with their specs,
  the JSON reports give the 7,667 obligations of 1.10.0, 40 of them moving
  from `unproved` to proved safe, 26 from proved safe to `unproved` -- 6 of
  them where 1.10.0 was wrong -- and one to `definite-error`; the findings
  are 910 instead of 909, and the issues on obligations 1,958 instead of 1,972.

## 1.10.0 - 2026-10-22

Aligned with AdaLang Analyzer 1.10.0. The report formats, the methods, the
reason codes, the rules, and the command line are those of 1.9.1, and the
plugin reads them with no change. There is one new kind of obligation,
`predicate-check`, which the plugin imports like any other. Expect new issues
where SPARK code gives a value to an object whose subtype has a predicate, and
fewer issues on postconditions that use `'Old`.

- **New `predicate-check` issues.** In SPARK code `--verify` checks that a
  value given to an object whose subtype has a predicate, or to a part of one,
  keeps the predicate: at an assignment, at a call, at a declaration, at a
  `return`, at a conversion. The check is an obligation of the new kind
  `predicate-check` (rule `proof-obligation:predicate-check`); one that is
  proved raises no issue, and one that is not is an `unproved` issue whose
  message says what was not certified. None is raised where the value already
  has the predicates of its own subtype.
- **Fewer `unproved` issues.** 1.10.0 translates `X'Old`, in the
  postcondition of a subprogram and in that of a callee it calls, and proves
  postconditions, preconditions, and assertions that depend on it. It also
  keeps more of what is known across a call: constants, the other components
  of a record when one component is passed, and facts about functions of SPARK
  generic units.
- **A few new `definite-error` issues.** A postcondition that the
  postcondition of a callee makes false on every path, which 1.9.1 left
  `unproved`, can now be a `definite-error`.
- **No false-safe corrected.** Nothing 1.9.1 proved safe is an issue in
  1.10.0. Over the analyzer's 202 `tests/verification_*` bodies, run with
  their specs, the JSON reports give 7,499 obligations instead of 7,478 (21
  predicate checks, 9 of them proved) and the same 888 findings; the issues on
  obligations are 1,926 instead of 1,933.

## 1.9.1 - 2026-10-20

Aligned with AdaLang Analyzer 1.9.1. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line are
those of 1.9.0, and the plugin reads them with no change. Expect fewer issues
on SPARK `Depends` aspects: fewer `flow-dependencies` proof-obligation issues,
and fewer `Depends_Contract_Mismatch` findings; outside SPARK contracts every
obligation keeps the verdict it had.

- **Fewer `unproved` issues.** 1.9.1 proves a `Depends` aspect. What each
  output may depend on is worked out from the body, and a `flow-dependencies`
  obligation whose aspect lists exactly that is proved safe, with the method
  `flow-analysis`. Up to 1.9.0 it was `unproved` wherever it was raised, with
  the method `none`, so every `Depends` aspect was an issue. More `Global`
  aspects are proved too: those of callers of an instance of
  `Unchecked_Conversion` or `Unchecked_Deallocation`, of a generic formal
  subprogram, or of an instance of a generic subprogram, and those of
  subprograms that read a deferred constant.
- **Some issues change.** A `flow-dependencies` obligation that stays
  `unproved` gives the method `flow-analysis` instead of `none`, and its
  message names the output and the input the aspect gets wrong ("Value may
  depend on Factor, which the aspect does not list for it").
- **`Depends_Contract_Mismatch` findings change.** The check now takes its flow
  findings from the same analysis. It reports none on a `Depends` aspect that
  holds: 1.9.0 misread expanded names, state abstractions, out parameters of an
  indefinite type, and functions that read a global. A wrong flow is reported
  once, no longer a second time as "listed after null", and wrong flows that
  1.9.0 missed are found. Where the dependencies cannot be worked out (across
  an exception handler, a dispatching call, or tasking) it reports no flow
  finding, and the obligation stays `unproved`.
- **No false-safe corrected.** No obligation is a new `definite-error`, and
  nothing 1.9.0 proved safe is an issue in 1.9.1. Imported `--verify` reports
  of 1.9.0 stay right on what they prove; regenerate them to drop the false
  `Depends_Contract_Mismatch` findings.

## 1.9.0 - 2026-10-19

Aligned with AdaLang Analyzer 1.9.0. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line are
those of 1.8.11, and the plugin reads them with no change. Expect fewer
proof-obligation issues in SPARK code that calls subprograms whose effects the
analyzer does not know, such as RecordFlux-generated code; outside SPARK code
every obligation keeps the verdict it had.

- **Fewer `unproved` issues.** 1.9.0 raises the obligations 1.8.11 raised and
  decides more of them. In SPARK code that takes no access value or address
  and declares no task or protected object, a call whose effects are not known
  keeps what is known of the caller's own objects and parameters; it lost all
  of it. A subprogram or a generic instance that the caller declares is no
  longer taken as code such a call could run. Expression functions are read
  eight calls deep instead of four, and where two paths meet, a fact that holds
  on both is kept even when they give an object different values.
- **Some issues change.** An obligation that stays `unproved` can give another
  reason for it, or none, and a longer inline path in its message.
- **No new finding and no false-safe corrected.** No obligation is a new
  `definite-error`, the checks report the findings of 1.8.11, and nothing
  1.8.11 proved safe is an issue in 1.9.0, so imported `--verify` reports of
  1.8.11 need not be regenerated.

## 1.8.11 - 2026-10-18

Aligned with AdaLang Analyzer 1.8.11. The report formats, the kinds of
obligation, the methods, the reason codes, and the rules are those of 1.8.10,
and the plugin reads them with no change. Expect fewer proof-obligation issues
in code that works on the components of a record, most of all in SPARK code.

- **Fewer `unproved` issues.** 1.8.11 raises the obligations 1.8.10 raised and
  decides more of them. What 1.8.10 knew of a record object was lost where a
  component of it was written (`Ctx.P.Next := 1`), where a procedure was
  called, and past a condition it could not translate; 1.8.11 keeps what it
  knows of the other components. In SPARK code that takes no access value or
  address and declares no task or protected object, a procedure call loses
  only the parts of the objects it is given. A function called without
  arguments (`pragma Assert (Inv)`) is a call to its body; it was read as an
  object never initialized.
- **Some issues change.** An obligation that stays `unproved` can give another
  reason for it, and one 1.8.10 could not decide can now be a
  `definite-error`, an issue of higher severity; an assertion or a
  precondition among these is also a finding of the `Known_Assertion_Failure`
  or `Known_Precondition_Failure` check.
- **No false-safe corrected.** Nothing 1.8.10 proved safe is an issue in
  1.8.11, so imported `--verify` reports of 1.8.10 need not be regenerated.

## 1.8.10 - 2026-10-17

Aligned with AdaLang Analyzer 1.8.10. The report formats, the kinds of
obligation, the methods, the reason codes, and the rules are those of 1.8.9,
and the plugin reads them with no change. Expect more proof-obligation issues
in code that instantiates a generic subprogram, uses a quantified expression
as a value, iterates over a container, or declares a package in a subprogram.

- **More issues in code that 1.8.9 did not verify.** A subprogram that
  declares an instance of a generic subprogram, or uses a quantified
  expression as a value (the condition of an `if`, the right side of an
  assignment, an initial value), was outside what 1.8.9 verifies, and every
  obligation in it was `unsupported`, which is no issue. 1.8.10 decides them:
  those it does not prove are `unproved` issues under the rule of their kind,
  and one it shows to fail is a `definite-error` issue; an assertion among
  these is also a finding of the `Known_Assertion_Failure` check. A call to an
  instance is taken as a call whose effects are not known.
- **Two false-safes corrected.** A check after an iteration that is not over
  a discrete range or an array (`for E of Container`, `for C in Iterator`, in
  a loop or in a quantified expression) could be proved safe where the
  iterator's operations, or the `First`, `Next`, `Has_Element` or `Element`
  of an `Iterable` type, change state. So could a check in a subprogram that
  declares a package whose elaboration changes state: an initial value that
  calls a function with side effects, a package body with statements, an
  instance of a generic package. Both concern every release with `--verify`
  up to 1.8.9. They are now `unproved` issues. Regenerate imported `--verify`
  reports with 1.8.10.

## 1.8.9 - 2026-10-16

Aligned with AdaLang Analyzer 1.8.9, from 1.8.6: version 1.8.8 of the plugin
changed only its version number. The report formats, the kinds of obligation,
the methods, the reason codes, and the rules are those of 1.8.6, and the
plugin reads them with no change. Expect more proof-obligation issues in code
that uses access types, and fewer in SPARK code that passes records.

- **More issues in code that uses access types.** A subprogram that
  dereferences an access value, allocates, or declares an access type or has
  an anonymous one was outside what 1.8.8 verifies, and every obligation in it
  was `unsupported`, which is no issue. 1.8.9 decides them: those it does not
  prove are `unproved` issues under the rule of their kind.
- **Fewer `unproved` issues in SPARK code.** 1.8.7 proves more preconditions
  of calls given a record or a part of one, and more checks on a record whose
  subtype has a predicate. What it proves from a predicate holds where the
  predicate holds: `--verify` does not check a predicate where an object is
  assigned or passed, as GNATprove does, so code given `SPARK_Mode` that does
  not keep its predicates has to discount these.
- **Two false-safes corrected.** A check written with a record object `X` of
  a type that has a component `X`, given to a function, could be proved safe
  where it fails (1.8.2 to 1.8.7). So could a read of an explicitly aliased
  parameter after a write through an access value, or after a call that may
  write the object given for it (1.6.0 to 1.8.8). They are now `unproved`
  issues. Regenerate imported `--verify` reports with 1.8.9.
- **Two false errors corrected.** The second argument of `T'Read (Stream,
  Item)` was taken for a read, not a write: an `out` parameter or a local not
  yet assigned given to it was a `definite-error` issue under
  `proof-obligation:initialization-check`, and, with `--recommended` or
  `--spark`, a finding of the `Uninitialized_Output` check. Neither is
  reported now. The `initialization-check` at the exit of such an `out`
  parameter is still `unproved`.

## 1.8.6 - 2026-10-10

Aligned with AdaLang Analyzer 1.8.6. The report formats, the kinds of
obligation, and the rules are those of 1.8.5, and the plugin reads them with
no change.

- **Fewer `unproved` issues.** 1.8.6 raises the obligations 1.8.5 raised and
  decides more of them. Much of what 1.8.5 left `unproved` was written with a
  form it did not hand to its solvers: an `if` or a `case` expression, a
  component of a component, a discriminant, the element of an array at a known
  index, `X'Constrained`, an access value compared with `null`. 1.8.6 reads
  these.
- **Some issues change.** An obligation that stays `unproved` can give another
  reason for it, and one 1.8.5 could not decide can now be a `definite-error`,
  an issue of higher severity; an assertion among these is also a finding of
  the `Known_Assertion_Failure` check.
- **A false-safe corrected.** A component read in an element of an array,
  `Row (I).Count`, was one value for every element of the array. An assertion,
  precondition, postcondition, loop invariant or runtime check written with
  such a component, or standing under a condition written with one, could be
  proved safe where it does not hold, a division by a component that can be
  zero among them; it is now an `unproved` issue under the rule of its kind.
  Regenerate imported `--verify` reports with 1.8.6.

## 1.8.5 - 2026-10-09

Aligned with AdaLang Analyzer 1.8.5. The report formats, the kinds of
obligation, and the rules are those of 1.8.4, and the plugin reads them with
no change.

- **Fewer `unproved` issues.** 1.8.5 raises the obligations 1.8.4 raised and
  decides more of them: it knows that a constant, an `in` parameter and a loop
  parameter hold a value wherever they are read, what a call leaves of its
  actuals, and what the bounds of an array can be.
- **Some issues change.** An obligation that stays `unproved` can give another
  reason for it, and one 1.8.4 could not decide can now be a `definite-error`,
  an issue of higher severity; a precondition among these is also a finding of
  the `Known_Precondition_Failure` check.
- **Two false-safes corrected.** The read of an object after it was the `out`
  actual of a call was proved safe where the callee can return without writing
  the parameter; it is now an `unproved` issue under
  `proof-obligation:initialization-check`, or a `definite-error` one where the
  callee never writes the parameter. And a precondition, postcondition,
  assertion or loop invariant was proved through a function that is declared
  in one part of a package and completed by an expression function in another.
  Regenerate imported `--verify` reports with 1.8.5.

## 1.8.4 - 2026-10-09

Aligned with AdaLang Analyzer 1.8.4. Expect more proof-obligation issues after
upgrading from 1.8.3.

- **New rule `proof-obligation:length-check`.** 1.8.4 checks that an array
  given to a target has the target's length: in an assignment, an initial
  value, a returned value, an actual parameter, the operand of a conversion,
  and the operands of `and`, `or` and `xor` on arrays. The ones it does not
  prove are issues under the new rule. An assignment has its check at the
  value, at the `:=`, or one at each, depending on its target.
- **More issues under two rules there already were.** 1.8.4 also checks the
  range of the length of an array where it is converted to an integer type,
  under `proof-obligation:range-check`, and the precondition of an operator
  that a declaration defines, under `proof-obligation:precondition`.
- **Fixed:** the expression that blocked a proof is kept whole in the message
  of its issue when it is written on several lines of the source. In
  console-text output, which is what the plugin reads when it runs the
  analyzer itself, the lines after the first were dropped.

## 1.8.3 - 2026-10-08

Aligned with AdaLang Analyzer 1.8.3, from 1.8.0. Expect more proof-obligation
issues after upgrading; the totals are not comparable with those of 1.8.0.

- **Added `sonar.ada.adalang.gnatproveLogPaths`.** Given the log of a GNATprove
  run on the same sources, the analyzer sets GNATprove's verdict beside each of
  its own proof obligations. The verdict is in the message of the issue, and
  the counts of the verdicts are in the scanner log. The log adds and removes
  no issue.
- **Added the declaration of the object of an initialization check** as a
  secondary location of its issue, from JSON reports.
- **Three new rules**, `proof-obligation:termination`,
  `proof-obligation:data-dependencies`, and
  `proof-obligation:flow-dependencies`. The analyzer raises obligations where
  earlier versions raised none: that a function returns, that a subprogram
  respects its `Global` and `Depends` aspects, and the checks in expression
  functions, inside preconditions and postconditions, on actual parameters, on
  the bounds of a slice, and on `out` parameters at exit. No route proves a
  `Depends` aspect yet, so every `flow-dependencies` obligation is `unproved`,
  with the method `none`, and each `Depends` aspect has one such issue; a
  wrong aspect is reported by the `Depends_Contract_Mismatch` check as before.
- **False-safes and false errors corrected.** Earlier versions could report an
  obligation as proved safe that a legal execution violates, and a definite
  error where there is none (see the analyzer's changelog for 1.6.1 and for
  1.8.1 to 1.8.3). Regenerate imported `--verify` reports with 1.8.3:
  obligations that were wrongly proved safe then appear as unproved or
  definite-error issues.
