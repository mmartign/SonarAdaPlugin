# Changelog

What each version of the plugin changes for a SonarQube user.

The version of the plugin is that of the AdaLang Analyzer it is aligned with,
so an entry says what the reports of that analyzer change in Sonar: which
issues appear, disappear, or move to another rule or severity. What the
analyzer itself changed is in
[its own changelog](https://github.com/mmartign/AdaLang_Analyzer/blob/main/CHANGELOG.md).
Versions before 1.8.3 are in the git history.

## 1.13.5 - 2026-11-10

Aligned with AdaLang Analyzer 1.13.5. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.13.4, and the plugin reads them with no change. Expect
issues on discriminant checks in SPARK code: the analyzer now raises one at
each component of a variant part and at each assignment of a record with
discriminants, where it raised one only where a static constraint decided
it. On the analyzer's benchmark corpora run with `--verify` the issues on
obligations go from 32,336 to 32,233: 105 close and 2 open.

- **Issues on discriminant checks.** A `discriminant-check` obligation the
  analyzer leaves unproved opens an issue under the rule
  `proof-obligation:discriminant-check`, which the plugin already had. On
  the corpora 135 are added, all in CoAP-SPARK: 130 proved safe, 3
  unsupported, and 2 unproved, the two issues that open.
- **Fewer issues.** 105 obligations move from unproved to proved safe, so
  their issues close: 38 `precondition`, 17 `assertion`, 12
  `integer-overflow`, 8 `range-check`, 8 `pointer-dereference`, 7
  `loop-invariant-preservation`, 6 `postcondition`, 5 `predicate-check`, 3
  `loop-invariant-initialization` and 1 `length-check`; 79 in CoAP-SPARK,
  21 in libkeccak, 5 in Saatana.
- **Another reason on some issues that stay.** 28 unproved obligations
  read a discriminant of an object that has no value yet, which the
  analyzer now allows (a discriminant always has one): instead of
  `uninitialized-object`, 16 now have no reason code (the provers left them
  unproved), 11 have `unsupported-expression-kind` and 1
  `missing-static-bounds`.
- **Definite errors on discriminants.** A component read under a guard
  that selects another variant, or a constrained object given other
  discriminants, is a definite error: its issue has high severity. None of
  the corpora has one.

No finding changes, and no definite error is new or gone on the corpora.

## 1.13.4 - 2026-11-09

Aligned with AdaLang Analyzer 1.13.4. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.13.3, and the plugin reads them with no change. Expect fewer
issues on records that hold an access value, the contexts RecordFlux
generates among them: the analyzer now follows access values and the
bounds of the arrays they designate. On the analyzer's benchmark corpora
run with `--verify` the issues on obligations go from 32,469 to 32,336,
and none opens.

- **Fewer issues.** 133 obligations move from unproved to proved safe, so
  their issues close: 57 `predicate-check` (the predicate of a RecordFlux
  context at `Ctx.Cursors (Fld) := ...`, `Ctx.Buffer := null` and the
  like), 42 `range-check`, 18 `integer-overflow`, 6
  `pointer-dereference`, 6 `assertion`, 3 `postcondition` and 1
  `precondition`; 129 of them in CoAP-SPARK, 4 in GNATColl.
- **Another reason on some issues that stay.** 261 unproved obligations
  read an access value or the bounds of what it designates, which the
  analyzer now translates; their issues give what stops it next: 232 now
  have no reason code (the provers left them unproved) instead of
  `missing-static-bounds` (172), `unsupported-expression-kind` (50) or
  `unsupported-attribute` (10), and 29 have another code.
- **Definite errors where null was just assigned.** A dereference of an
  access object just assigned `null`, or a call whose precondition requires
  it not to be null, is a definite error: its issue goes from medium to high
  severity. None of the corpora has one.

No finding changes, and no definite error is new or gone on the corpora.

## 1.13.3 - 2026-11-08

Aligned with AdaLang Analyzer 1.13.3. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.13.2, and the plugin reads them with no change. Expect fewer
issues on arrays with `Relaxed_Initialization`, whose elements the analyzer
now follows one by one. On the analyzer's benchmark corpora run with
`--verify` the issues on obligations go from 32,658 to 32,469, and none
opens.

- **Initialization checks GNATprove does not have gone.** An element or a
  slice of an object with relaxed initialization passed to an `out`
  parameter reads no value and no longer has an `initialization-check`
  obligation: 18 `unproved` obligations and their issues are gone.
- **Fewer issues.** 171 obligations move from unproved to proved safe, so
  their issues close: 45 `assertion` (`R (0 .. 5)'Initialized` and the
  like), 32 `initialization-check`, 28 `index-check`, 23
  `integer-overflow`, 18 `range-check`, 21 loop invariants (11 on entry,
  10 preserved by an iteration), 2 `postcondition` and 2 `precondition`;
  101 of them in SPARKNaCl.
- **Another reason on some issues that stay.** 37 unproved assertions,
  postconditions and loop invariants say `'Initialized` of elements, of a
  slice or of a whole array that the analyzer now translates; their issues
  give what stops it next: 31 that had `uninitialized-object` now have no
  reason code (the provers left them unproved), 4 have
  `unsupported-operator` instead, and 2 `unsupported-expression-kind`
  instead of `unsupported-conversion`.

No finding changes, and no definite error is new or gone.

## 1.13.2 - 2026-11-07

Aligned with AdaLang Analyzer 1.13.2. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.13.1, and the plugin reads them with no change. Expect fewer
`initialization-check` issues on objects with `Relaxed_Initialization`, and
a few new ones where the analyzer used to take such an object as having a
value in every part. On the analyzer's benchmark corpora run with
`--verify` the issues on obligations go from 32,871 to 32,658.

- **Initialization checks GNATprove does not have gone.** The prefix of
  `X'Initialized`, the copy of an object with relaxed initialization into
  another one or as the actual of such a formal, which reads no value, and
  such an `out` parameter at the exit no longer have an
  `initialization-check` obligation: 122 `unproved` obligations and their
  issues are gone, and 33 proved ones with them.
- **Fewer issues.** 99 obligations move from unproved to proved safe, so
  their issues close: 26 `initialization-check`, 20 `range-check`, 19
  `assertion`, 13 loop invariants (6 on entry, 7 preserved by an
  iteration), 12 `precondition`, 5 `postcondition`, 2 `predicate-check` and
  1 `integer-overflow`; 92 of them are in SPARKNaCl. The analyzer now
  follows an object with relaxed initialization as a whole, and knows it
  has a value in every part where a precondition, an assertion or a loop
  invariant says `X'Initialized` of it.
- **New issues.** 8 `initialization-check` obligations that were proved
  are `unproved` now (6 in SPARKNaCl, 2 in a Saatana test program): reads of
  a parameter with relaxed initialization, which can come with parts that
  have no value (FP-140 in the analyzer). GNATprove proves the 6 of
  SPARKNaCl from what the precondition says of each element, which the
  analyzer does not follow yet; nothing says that the 2 of Saatana have a
  value.
- **The reason given.** The issue of an initialization check left unproved
  on an object with relaxed initialization says so: "the object has relaxed
  initialization and is not known to have a value in every part".
- **A false proof gone.** In 1.13.0 and 1.13.1 an assertion read after a
  write of an element of the same array could be proved whatever it said
  (FP-141 in the analyzer). No obligation of the corpora relied on it.

No finding changes on any corpus, and no `definite-error` is new or gone.

## 1.13.1 - 2026-11-05

Aligned with AdaLang Analyzer 1.13.1. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.13.0, and the plugin reads them with no change. Expect far
fewer `range-check` issues on arrays, and fewer issues on index checks,
preconditions and other checks in code that works on arrays of records,
RecordFlux's in particular. On the analyzer's benchmark corpora run with
`--verify` the issues on obligations go from 35,988 to 32,871.

- **Range checks of array values gone.** An array given to an array subtype
  -- the operand of a conversion, the initial value of an object, an
  assigned value -- had a `range-check` obligation as a scalar has, beside
  the checks of its length and of the bounds of a slice, and nothing could
  decide it. The analyzer no longer raises it: 1,853 `unproved` obligations
  and their issues are gone, and 18 proved ones with them. The
  `--recommended` and `--spark` presets lose the same obligations.
- **Fewer issues.** 1,264 obligations move from unproved to proved safe, so
  their issues close: 963 `index-check`, 86 `precondition`, 83
  `integer-overflow`, 51 `range-check`, 27 `postcondition`, 22 loop
  invariants (8 on entry, 14 preserved by an iteration), 20 `assertion`, 10
  `length-check` and 2 `predicate-check`. 1,108 of them are in CoAP-SPARK,
  109 in Tokeneer and 45 in AWS. The scalar components of the elements of an
  array of records are now in the analyzer's verification conditions, an
  index of an enumeration type is its position, an object declared with a
  slice or a concatenation has the bounds it takes from it, and what is
  known of the elements where a loop is entered holds of `X'Loop_Entry`.
  Most of the index checks are of an array indexed by an enumeration, at an
  index of that type.

No finding changes on any corpus, and no `definite-error` is new or gone.

## 1.13.0 - 2026-11-03

Aligned with AdaLang Analyzer 1.13.0. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.12.3, and the plugin reads them with no change. Expect fewer
issues on loop invariants, predicate checks, assertions and range checks in
code that works on arrays, and one more on a loop invariant. On the
analyzer's benchmark corpora run with `--verify` the issues on obligations
go from 36,144 to 35,988.

- **Fewer issues.** 157 obligations move from unproved to proved safe, so
  their issues close: 60 loop invariants (43 on entry, 17 preserved by an
  iteration), 36 `predicate-check`, 20 `range-check`, 19 `assertion`, 10
  `precondition`, 5 `index-check`, 4 `integer-overflow` and 3
  `postcondition`. 113 of them are in SPARKNaCl. An element of an array read
  at an index that is not known is now the element of the array's contents,
  of which preconditions, assertions and stores speak; loops keep what holds
  of arrays they do not write, `X'Loop_Entry` is translated, and what holds
  after a `for` loop is what holds at the end of its last iteration.
- **One more issue.** A loop invariant in SPARKNaCl that 1.12.3 proved
  preserved is `unproved` again: the analyzer had checked the next iteration
  with the loop parameter at the value it had (its FP-139). GNATprove proves
  the invariant, so the issue says what the analyzer cannot prove, not that
  the code is wrong.

No finding changes on any corpus, and no `definite-error` is new or gone.

## 1.12.3 - 2026-11-02

Aligned with AdaLang Analyzer 1.12.3. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.12.2, and the plugin reads them with no change. Expect fewer
issues on range, overflow and index checks and on loop invariants, and a few
new `precondition` issues in code that RecordFlux generates. On the
analyzer's benchmark corpora run with `--verify` the issues on obligations
go from 36,251 to 36,144.

- **Fewer issues.** 138 obligations move from unproved to proved safe, so
  their issues close: 81 `range-check`, 27 `integer-overflow`, 4
  `index-check`, and 26 loop invariants, 15 on entry and 11 preserved by an
  iteration. 78 of them are in SPARKNaCl. An element of an array read at an
  index that is not known is now within the subtype of the array's
  components in SPARK code, and a `Loop_Invariant` that does not lead its
  loop is now proved where it stands or by cutting the loop there.
- **New `precondition` issues.** `--verify` now checks the precondition of
  an operator that a generic unit hides behind an abstract function, as
  RecordFlux does for `Index`, at the operator, and that of a call of a
  renaming. Of the 44 obligations this adds on the corpora, all in
  CoAP-SPARK, 13 are proved safe and 31 stay unproved and raise one issue
  each, 22 at operators and 9 at calls of a renaming. GNATprove proves all
  31, so they say what the analyzer could not prove, not that the code is
  wrong. The operation of an issue at an operator is the operator alone
  (`+`), at the column of the operator.
- **Never a definite error at such an operator.** In an instance GNAT may
  call the predefined operator of the actual instead, so a precondition the
  arguments break there is `unproved`, with that reason, not
  `definite-error`.

No finding changes on any corpus, and no `definite-error` is new or gone.

## 1.12.2 - 2026-11-01

Aligned with AdaLang Analyzer 1.12.2 as tagged on 2026-11-02. The tag first
stood on an earlier build, which gave a call two precondition obligations, and
this entry first described that build. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.12.1, and the plugin reads them with no change. Expect about
half as many `precondition` issues, a few new ones in generic units, and a few
fewer issues in generic units declared inside generic units. On the analyzer's
benchmark corpora run with `--verify` the issues on obligations go from 37,826
to 36,251.

- **One `precondition` issue for a call.** A call with actuals had two
  precondition obligations at the same place, one whose operation is the name
  of the subprogram and one whose operation is the call with its actuals, so
  an unproved precondition raised two issues. The first is gone: its issue
  closes, and the issue on the call stays. A call with no actuals in
  parentheses had one and keeps it. On the corpora 1,596 issues close this
  way, and the `precondition` issues go from 3,244 to 1,674.
- **New `precondition` issues.** `--verify` now checks the precondition at a
  call of a generic formal subprogram when some instance gives the formal a
  subprogram with a `Pre` aspect. Nothing checked it before. Of the 119
  obligations this adds on the corpora, 93 are proved safe and raise no issue,
  and 26 stay unproved and raise one each, 14 in libkeccak and 12 in
  CoAP-SPARK. GNATprove proves all 26, so they say what the analyzer could not
  prove, not that the code is wrong.
- **One issue for all the instances.** The issue is on the call in the generic
  body, and it closes when every instance proves the call. It is always
  `unproved`, never `definite-error`, even when an instance gives an actual
  whose precondition the call breaks.
- **Fewer issues.** Generic units declared inside generic units are now
  analyzed with the actuals of their instances: 5 obligations in libkeccak move
  from unproved to proved safe, so their issues close, 3 `range-check` and 2
  `integer-overflow`.
- **Issues that may open on your code.** Since 1.10.0 a division, index or
  range check in a postcondition could be proved safe with what was true on
  entry instead of at the exit, when an `X'Old` stood to its left in an
  `and then` and the subprogram writes the object the check reads (`FP-137` in
  the analyzer's changelog). Such a check now stays unproved unless the exit
  proves it, and raises an issue. None opens on the corpora.

No finding changes on any corpus, and no `definite-error` is new or gone.

## 1.12.1 - 2026-10-31

Aligned with AdaLang Analyzer 1.12.1. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.12.0, and the plugin reads them with no change. Expect fewer
issues on preconditions, above all on code that RecordFlux generates, and a
few new issues on code the analyzer used to pass by mistake. On the analyzer's
ten benchmark corpora the issues on obligations go from 38,214 to 37,826.

- **Fewer issues.** `--verify` keeps more of what is known across a statement
  and reads more of what a call says: 388 obligations move from unproved to
  proved safe on the ten corpora, so their issues close: 357 `precondition`, 15
  `integer-overflow`, 7 `pointer-dereference`, 7 `range-check` and 2
  `postcondition`.
- **New issues that were always due.** Two false-safes of the analyzer are
  corrected, `FP-135` on an array passed to a formal of another constrained
  subtype with other bounds and `FP-136` on a function given an access value.
  Code of either form can get a new unproved issue, which earlier versions
  passed in error: none on the ten corpora. Read the analyzer's changelog for
  what to re-check.
- **An issue can get more severe.** A precondition that a slice given as an
  actual breaks is now a `definite-error`, a Reliability issue of high
  severity, where it was unproved: none on the ten corpora.

No finding changes on any corpus.

## 1.12.0 - 2026-10-30

Aligned with AdaLang Analyzer 1.12.0. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.11.2, and the plugin reads them with no change. Expect many
fewer `proof-obligation:initialization-check` issues, some new ones at calls,
and a few new issues on code the analyzer used to pass by mistake. On the
analyzer's ten benchmark corpora the issues on obligations go from 39,885 to
38,214.

- **Fewer `initialization-check` issues.** `--verify` now follows
  initialization through contracts and through the parts of an object, as
  GNATprove does: 1,658 such obligations move from unproved to proved safe on
  the ten corpora, so their issues close, and 182 are no longer raised.
- **New `initialization-check` issues at calls.** A subprogram of SPARK code is
  now entered with what it takes from outside initialized, and each such object
  is checked at each call. These obligations have the rule they had, are placed
  at the call, name the object read as their subject, and read
  "`P` reads `G`" as their operation. On the ten corpora 2,208 are raised:
  2,035 proved safe and 173 unproved, which are new issues.
- **Fewer issues of other kinds.** 116 obligations move from unproved to proved
  safe as well: 36 `range-check`, 27 `integer-overflow`, 23 `precondition`, 12
  `postcondition`, 8 `assertion`, 7 `index-check` and 3 `predicate-check`.
- **New issues that were always due.** Two false-safes of the analyzer are
  corrected, `FP-133` on objects with `Relaxed_Initialization` and `FP-134` on
  an object declared with another name in one declaration, as in
  `First, Second : Integer`. Code of either form can get a new unproved issue,
  which earlier versions passed in error: 4 such on the ten corpora. Read the
  analyzer's changelog for what to re-check.

A proved initialization check now rests on the checks at the calls, which is an
assumption the analyzer states in "What comes from outside" of its supported
verification subset. No finding changes on any corpus.

## 1.11.2 - 2026-10-29

Aligned with AdaLang Analyzer 1.11.2. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.11.1, and the plugin reads them with no change. Expect a few
new `proof-obligation:index-check` issues where code assigns to a slice, and
fewer `proof-obligation:range-check` issues on slices.

- **New `index-check` obligations on slices assigned to.** `--verify` now
  raises an index check at each bound of a slice that is the target of an
  assignment, `A (L .. H) := ...`, as GNATprove does, besides the range
  check of the slice it raised already. One that is not proved is a new
  `proof-obligation:index-check` issue. On the analyzer's ten benchmark
  corpora 437 are raised: 186 proved, 233 unproved, and 18 unsupported,
  which raise no issue.
- **Fewer `range-check` issues on slices.** The range check of a slice is
  now proved, too, when its bounds are within the array whenever the slice
  is not null, as in `Tmp (0 .. B - 1)` with `B` of zero. On the ten corpora
  176 such range checks move from unproved to proved safe, so their issues
  are gone; no finding of any preset changes.
- **A false positive is corrected.** `Dead_Store` and
  `Overwritten_Assignment` no longer report a store to a variable that a
  function or procedure declared in the same `declare` block reads when it
  is called later in the block (FP-132 in the analyzer's changelog), so such
  issues are gone. None of the benchmark corpora had one.
- Over the analyzer's 221 `tests/verification_*` bodies, run with their
  specs, the JSON reports give 8,489 obligations instead of 8,451, the 38
  new index checks (32 proved, 6 unproved), and the same 1,017 findings; 8
  range checks move from unproved to proved safe, so the issues on
  obligations are 2,038 instead of 2,040.

## 1.11.1 - 2026-10-27

Aligned with AdaLang Analyzer 1.11.1. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line
are those of 1.11.0, and the plugin reads them with no change. Expect fewer
`unproved` issues on `--verify` reports, most of all on RecordFlux-generated
code, and a few issues that move from `unproved` to a definite error.

- **Fewer `unproved` issues.** `--verify` now proves obligations that read an
  operator a declaration defines (RecordFlux's `Index - Index` among them),
  `T'Pos` and `T'Val`, `F'Result` in the postcondition of a function,
  `X'Constrained` of a component of a variable, a mask of low bits
  (`X and 16#FF#`), a power of known operands, or `Interfaces.Shift_Right` and
  `Shift_Left` by a known amount. On the analyzer's ten benchmark corpora 191
  obligations move from unproved to proved safe -- 63 range checks, 52
  preconditions, 37 overflow checks, 27 postconditions, 7 length checks, 4
  assertions and 1 index check, 128 of them in CoAP-SPARK -- so their issues
  are gone, and no finding of any preset changes.
- **Some `unproved` issues become definite errors.** Where an operator's
  function does not compute what its symbol says, or a modular power wraps to
  zero, a claim that the function's expression shows false is now a definite
  error: its `proof-obligation:<kind>` issue keeps its rule and moves from
  MEDIUM to HIGH, and a false assertion also gets a `Known_Assertion_Failure`
  issue. None of this happens on the benchmark corpora; on the analyzer's
  own fixtures, written to show it, 9 obligations move so.
- **A false proof is corrected.** 1.8.6 to 1.11.0 proved `not X'Constrained`
  where `X` is a formal object of mode `in` of a generic unit, which is a
  constant, so that `X'Constrained` is True (FP-131 in the analyzer's
  changelog). Such a claim is now a definite error, raising a HIGH issue
  where there was none.
- Over the analyzer's 219 `tests/verification_*` bodies of 1.11.0, run with
  their specs, the JSON reports give the same 8,213 obligations and 991
  findings instead of 982 (the 9 `Known_Assertion_Failure` above); 13
  obligations move from unproved to proved safe and 9 from unproved to a
  definite error, so the issues on obligations are 1,992 instead of 2,005.

## 1.11.0 - 2026-10-26

Aligned with AdaLang Analyzer 1.11.0. The report formats, the methods, the
reason codes, the rules, and the command line are those of 1.10.4, and the
plugin reads them with no change. There is one new kind of obligation,
`pointer-dereference`, which the plugin imports like any other. Expect new
issues where SPARK code dereferences an access value, and a few fewer on
preconditions and postconditions after a write through one.

- **New `pointer-dereference` issues.** In SPARK code `--verify` checks that an
  access value is not null where it is dereferenced: at `P.all` and at the
  implicit dereferences `P.C`, `P (I)`, `P (I .. J)`, `P'First`, `P'Last`,
  `P'Length` and `P'Range`. The check is an obligation of the new kind
  `pointer-dereference` (rule `proof-obligation:pointer-dereference`); one
  that a precondition, a guard, or a callee's postcondition proves raises no
  issue, and one that is not proved is an `unproved` issue whose message says
  that the access value is not known not to be null. None is raised where the
  subtype of the access value excludes null, as GNATprove raises none, nor in
  code that is not SPARK. On the analyzer's ten benchmark corpora 402 are
  raised, all in CoAP-SPARK, 350 of them proved.
- **Fewer `unproved` issues after a write through an access value.** In SPARK
  code a write to elements of an array an access value designates, such as
  `Ctx.Buffer.all (A .. B) := Data`, no longer makes the analysis forget what
  it knew of the access value. On the ten benchmark corpora 21 obligations
  move from unproved to proved safe, 14 preconditions and 7 postconditions,
  all in CoAP-SPARK.
- No other issue is new, and none changes rule or severity. Over the
  analyzer's 219 `tests/verification_*` bodies, run with their specs, the JSON
  reports give 8,213 obligations instead of 8,180 (33 dereference checks, 15
  of them proved) and the same 982 findings; no obligation changes verdict, so
  the issues on obligations are 2,005 instead of 1,987.

## 1.10.4 - 2026-10-25

Aligned with AdaLang Analyzer 1.10.4. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line are
those of 1.10.3, and the plugin reads them with no change. Expect fewer
`unproved` issues, most of them in generic units, and one rare new
`Ineffective_Operation` issue.

- **Fewer `unproved` issues in generic units.** With `--verify` the analyzer
  analyzes a generic unit once more for each of its instances in the run, its
  formal objects having the values of their actuals and its formal scalar
  types being their actual subtypes, and an obligation proved for every
  instance is proved: Keccak's and RecordFlux's generics are where it shows.
  The instances must be in the same run, as they are when the plugin runs the
  analyzer on the project. On the analyzer's ten benchmark corpora 562
  obligations move from unproved to proved safe.
- **Fewer `unproved` issues on typed constants.** A constant of a package whose
  initializer is static, such as `Bits : constant Positive := 2**Log`, has its
  value, as a named number has; so has a name seen through an instance of a
  generic package. Two checks that such a value makes hold by their form are
  no longer raised, so their obligations, and the issue of the one that was
  unproved, are gone.
- **One more `Ineffective_Operation` issue can appear** where a typed constant
  of value 0 or 1 is an identity operand (`Index_1 - Index_0` with
  `Index_0 : constant Natural := 0`), as it already did for a named number:
  one on the ten benchmark corpora.
- No other issue is new, and none changes rule or severity. Over the
  analyzer's 210 `tests/verification_*` bodies of 1.10.3, run with their specs,
  the JSON reports give the same 8,001 obligations and 945 findings: 1
  obligation moves from unproved to proved safe and none the other way, so the
  issues on obligations are 1,934 instead of 1,935.

## 1.10.3 - 2026-10-24

Aligned with AdaLang Analyzer 1.10.3. The report formats, the kinds of
obligation, the methods, the reason codes, the rules, and the command line are
those of 1.10.2, and the plugin reads them with no change. Expect fewer
`unproved` issues and no new ones.

- **Fewer `unproved` issues after an assignment.** What a variable is assigned
  stays within the variable's subtype, so overflow and range checks on it that
  rest on that subtype are proved, also after a loop has widened its bounds.
- **Fewer `unproved` issues in loops.** A precondition on `in` parameters, or
  what a constant declared before the loop says, holds all through the loop,
  and checks in the body that rest on it together with the loop invariant are
  proved.
- **Fewer `unproved` issues after a write to an array element.** In SPARK code
  that takes no `'Access` or `'Address`, such a write no longer makes the
  analysis forget what it knew of every other array; and a division by a named
  number is now decided.
- No issue is new, and none changes rule or severity. Over the analyzer's 207
  `tests/verification_*` bodies of 1.10.2, run with their specs, the JSON
  reports give the same 7,744 obligations and 922 findings: 54 obligations move
  from unproved to proved safe and none the other way, so the issues on
  obligations are 1,893 instead of 1,947.

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
