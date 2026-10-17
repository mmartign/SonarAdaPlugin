# Changelog

What each version of the plugin changes for a SonarQube user.

The version of the plugin is that of the AdaLang Analyzer it is aligned with,
so an entry says what the reports of that analyzer change in Sonar: which
issues appear, disappear, or move to another rule or severity. What the
analyzer itself changed is in
[its own changelog](https://github.com/mmartign/AdaLang_Analyzer/blob/main/CHANGELOG.md).
Versions before 1.8.3 are in the git history.

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
