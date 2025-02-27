# FAQ

## How can I add ALL the files to the chat?
People regularly ask about how to add **many or all of their repo’s files** to the chat. This is probably not a good idea and will likely do more harm than good.

The best approach is to think about which files need to be changed to accomplish the task you are working on. Just add those files to the chat.

Usually, when people want to add “all the files,” it’s because they think it will give the LLM helpful context about the overall code base. Aider will automatically give the LLM additional context about the rest of your git repo by analyzing your entire codebase in light of the current chat to build a compact [repository map](https://aider.chat/2023/10/22/repomap.html).

Adding irrelevant files can distract or confuse the LLM, leading to worse coding results and increased token costs.

If you still wish to add lots of files to the chat, you can:
- Use a wildcard when you launch aider: `aider src/*.py`
- Use a wildcard with the in-chat `/add` command: `/add src/*.py`
- Give the `/add` command a directory name to recursively add every file under that directory: `/add src`

## Can I use aider in a large (mono) repo?
Aider will work in any size repo but is not optimized for quick performance in very large repos. To improve performance:
- Change into a subdirectory of your repo that contains the code you want to work on and use the `--subtree-only` switch to ignore the repo outside of that directory.
- Create a `.aiderignore` file to tell aider to ignore parts of the repo that aren’t relevant to your task.

## Can I use aider with multiple git repos at once?
Currently, aider can only work with one repo at a time. However, you can:
- Run aider in repo-A and use `/read` to add some files read-only from another repo-B.
- Create repo maps in each repo and share them.
- Use aider to write documentation about a repo and read in those docs from another repo.

## How do I turn on the repository map?
Depending on the LLM you are using, the repo map may be disabled by default. If you would like to force it on, you can run aider with `--map-tokens 1024`.

## How do I include the git history in the context?
When starting a fresh aider session, you can include recent git history in the chat context. Use the `/run` command with `git diff` to show recent changes:
- `/run git diff HEAD~1` for the last commit.
- Increase the number after the tilde for multiple commits: `/run git diff HEAD~3`.

## How can I run aider locally from source code?
To run the project locally, follow these steps:
