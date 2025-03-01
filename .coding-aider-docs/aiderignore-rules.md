# Gitignore Patterns Reference

## Basic Rules
- Lines starting with `#` are comments; blank lines are ignored.
- Leading/trailing whitespace is ignored unless escaped with `\`.

## Wildcards
- `*` : Matches any number of characters (except `/`).
- `?` : Matches exactly one character.
- `[abc]` : Matches any one character in the set (ranges like `[a-z]` allowed).

## Recursive Patterns
- `**` : Matches directories recursively.
    - Example: `**/temp` matches any "temp" folder at any depth.

## Directory and Absolute Patterns
- Trailing `/` : Restricts the pattern to directories only.
- Leading `/` : Anchors the pattern to the repository root.

## Negation
- `!` : Negates a pattern to include files otherwise ignored.
    - Example:
      ```
      logs/
      !logs/important.log
      ```

## Order & Escaping
- **Order Matters:** Later rules override earlier ones.
- Special characters (`*`, `?`, `[`) can be escaped with `\`.