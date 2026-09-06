// Secret blocker: prevent credentials from being read from or written into git-tracked files.
// Pattern follows the official opencode ".env protection" plugin example.
export const SecretBlocker = async () => {
  const READ_BLOCKED = [
    /(^|\/)career-ai\/\.env$/,
    /(^|\/)application-local\.yml$/,
    /(^|\/)devtools\/env\.sh$/,
  ];
  // Secret-looking values that must never be written into repo files.
  const SECRET_PATTERNS = [
    /afxp_[A-Za-z0-9_-]{10,}/,
    /sk-[A-Za-z0-9]{10,}/,
    /-----BEGIN [A-Z ]*PRIVATE KEY-----/,
  ];

  return {
    "tool.execute.before": async (input, output) => {
      const args = output.args ?? {};

      if (input.tool === "read" && typeof args.filePath === "string") {
        if (READ_BLOCKED.some((re) => re.test(args.filePath))) {
          throw new Error(
            `SecretBlocker: do not read ${args.filePath} (holds real credentials). ` +
              `Use ~/devtools/env.sh via bash env, or the *.example.* template files instead.`,
          );
        }
        return;
      }

      if ((input.tool === "edit" || input.tool === "write") && typeof args.filePath === "string") {
        const text =
          (typeof args.content === "string" ? args.content : "") +
          (typeof args.newString === "string" ? args.newString : "");
        const hit = SECRET_PATTERNS.find((re) => re.test(text));
        if (hit) {
          throw new Error(
            `SecretBlocker: secret-looking value blocked in ${args.filePath}. ` +
              `Reference env vars (e.g. {env:VAR} / $VAR) instead of pasting tokens/keys.`,
          );
        }
      }
    },
  };
};
