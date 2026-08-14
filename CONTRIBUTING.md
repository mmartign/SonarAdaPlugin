# Contributing

Thanks for your interest in improving the SonarQube Ada Plugin.

## Before you start

- For bug reports and feature requests, please open a
  [GitHub issue](https://github.com/mmartign/SonarAdaPlugin/issues) first,
  including SonarQube version, plugin version, and (for bugs) steps to
  reproduce.
- For anything non-trivial, open an issue to discuss the approach before
  sending a pull request — it saves rework on both sides.
- By participating in this project you are expected to uphold the
  [Code of Conduct](CODE_OF_CONDUCT.md).

## Building and testing

See the [Build](README.md#build) section of the README for the full setup,
including how to install the Libadalang Java bindings and native libraries
required by `LibadalangAnalyzer`.

Once set up:

```bash
mvn test
```

runs the full test suite. If you don't have the native Libadalang libraries
installed locally, you can still build and run everything except the tests
in `LibadalangAnalyzerTest`:

```bash
mvn -P skip-native-tests test
```

This is the same profile used by [CI](README.md#continuous-integration).

## Submitting a pull request

- Keep changes focused; unrelated cleanup makes review harder.
- Add or update tests for behavior you change.
- Make sure `mvn -P skip-native-tests test` passes locally; CI runs the same
  check on every pull request.
- Describe the change and its motivation in the pull request description.

## License

This project is licensed under the GNU General Public License v3.0 or later
(see [LICENSE](LICENSE)). By submitting a contribution, you agree that it
will be distributed under the same license.
