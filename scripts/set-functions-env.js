#!/usr/bin/env node
/**
 * Utility script to manage Firebase Functions environment variables
 * using the new `.env` file support (Firebase CLI v13+).
 *
 * Usage examples:
 *   node scripts/set-functions-env.js SMTP_URL="smtps://user:pass@smtp.host:465"
 *   node scripts/set-functions-env.js --env-file functions/.env.production SMTP_URL="..."
 *
 * After updating the file, deploy with:
 *   firebase deploy --only functions
 */

const fs = require("fs");
const path = require("path");

const DEFAULT_ENV_FILE = path.join(__dirname, "..", "functions", ".env");

/**
 * Parse CLI args into {envFile, assignments[]}
 */
const parseArgs = () => {
  const args = process.argv.slice(2);
  const result = {
    envFile: DEFAULT_ENV_FILE,
    assignments: [],
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];

    if (arg === "--env-file") {
      const next = args[i + 1];
      if (!next) {
        throw new Error("--env-file flag requires a value");
      }
      result.envFile = path.isAbsolute(next)
        ? next
        : path.join(process.cwd(), next);
      i += 1;
      continue;
    }

    if (arg.includes("=")) {
      result.assignments.push(arg);
      continue;
    }

    throw new Error(`Unrecognized argument "${arg}". Expected KEY=value pairs.`);
  }

  if (result.assignments.length === 0) {
    throw new Error("Provide at least one KEY=value pair to set.");
  }

  return result;
};

/**
 * Load existing env file into a map
 */
const loadEnvMap = (filePath) => {
  if (!fs.existsSync(filePath)) {
    return {};
  }

  const contents = fs.readFileSync(filePath, "utf8");
  const map = {};

  contents.split(/\r?\n/).forEach((line) => {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      return;
    }
    const eqIndex = trimmed.indexOf("=");
    if (eqIndex === -1) {
      return;
    }
    const key = trimmed.substring(0, eqIndex).trim();
    const value = trimmed.substring(eqIndex + 1).trim();
    map[key] = value;
  });

  return map;
};

/**
 * Apply new assignments to env map
 */
const applyAssignments = (map, assignments) => {
  assignments.forEach((assignment) => {
    const eqIndex = assignment.indexOf("=");
    const key = assignment.substring(0, eqIndex).trim();
    const value = assignment.substring(eqIndex + 1).trim();

    if (!key) {
      throw new Error(`Invalid assignment "${assignment}" (missing key).`);
    }

    map[key] = value;
  });
};

/**
 * Write env map back to file (sorted keys)
 */
const writeEnvFile = (filePath, map) => {
  const dir = path.dirname(filePath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, {recursive: true});
  }

  const lines = Object.keys(map)
    .sort()
    .map((key) => `${key}=${map[key]}`);

  fs.writeFileSync(filePath, lines.join("\n") + "\n", {encoding: "utf8"});
};

const main = () => {
  try {
    const {envFile, assignments} = parseArgs();
    const envMap = loadEnvMap(envFile);
    applyAssignments(envMap, assignments);
    writeEnvFile(envFile, envMap);

    console.log(`Updated ${envFile} with:`);
    assignments.forEach((assignment) => console.log(`  - ${assignment}`));
    console.log("\nNext steps:");
    console.log(`  1. Review ${envFile} (it contains sensitive data; keep it out of version control).`);
    console.log("  2. Deploy updated env vars: firebase deploy --only functions");
    console.log("  3. Re-test the Voting Interface email flow.");
  } catch (error) {
    console.error("Failed to update environment variables:");
    console.error(error.message);
    process.exitCode = 1;
  }
};

main();

