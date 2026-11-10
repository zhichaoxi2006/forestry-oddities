#!/usr/bin/env python3
"""Fail when two shaped recipes claim the same grid for different results.

Why this exists
---------------
Vanilla matches a ``minecraft:crafting_shaped`` recipe against the *bounding box*
of the items placed in the crafting grid: ``ShapedRecipePattern#shrink`` trims
empty leading and trailing rows and columns, ``CraftingInput.ofPositioned`` trims
the grid the same way, and the pattern is also tried mirrored.  Two recipes that
occupy the same cells with the same ingredients are therefore indistinguishable,
and ``RecipeManager#getRecipeFor`` returns the first match it finds
(``byType(type).stream().filter(matches).findFirst()`` over an unordered
multimap): both recipes load, but only one of the two items can ever be crafted
while JEI and the recipe book still list both.

That is exactly how the alveary circuit foundation and the alveary transmission
used to collide with Binnie's Mods' alveary stimulator and alveary transmission -
the same grids and the same item/tag keys for four of the five ingredients, but
different results.  The two recipes were rewritten (``KTK``/``RXR``/``CCC`` and
``GRG``/``RXR``/``GRG``); this script is what keeps them from drifting back.

Only a *shared grid with different results* is a conflict.  Two recipes that
share a grid and produce the same item are fine - Forestry's three ``bog_earth``
recipes and three ``cake`` recipes are deliberately like that - so a conflict
needs both a shared grid and two distinct result ids.  Two recipes with the same
grid but a different ingredient *count* are not a conflict either, because
``ShapedRecipePattern#matches`` short-circuits on ``ingredientCount`` first.

Recipe conditions are ignored: a conditional recipe counts as present, which is
the conservative reading (it catches a conflict that only appears when the
condition holds).

Usage
-----
    python tools/check_recipes.py
        Scan this checkout, the sibling ForestryCE / Binnie's checkouts that are
        present next to it, and every jar in run/mods.

    python tools/check_recipes.py --root D:/packs/foo/data --jar foo.jar
        Add data directories and/or jars (both repeatable).

    python tools/check_recipes.py --no-jars
        Skip run/mods.

Exit status: 0 when the pack is clean, 1 when a conflict was found or when a
recipe in this checkout is malformed.
"""

from __future__ import annotations

import argparse
import io
import json
import os
import re
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
RESOURCES = os.path.join(REPO, "src", "main", "resources")
RECIPE_SEGMENT = os.sep + "recipe" + os.sep
JAR_RECIPE = re.compile(r"^data/[^/]+/recipe/")

# Sibling checkouts.  Forestry is a hard dependency and Binnie's Mods is the mod
# this content was ported from, so both are part of the pack whenever they are
# checked out; a missing checkout is simply skipped.
SIBLINGS = (
    ("Forestry CE", os.path.join(REPO, "..", "ForestryCE", "src", "generated", "resources", "data")),
    ("Forestry CE farms", os.path.join(REPO, "..", "ForestryCE", "src", "generated", "resources_farms", "data")),
    ("Binnie's Mods", os.path.join(REPO, "..", "binnie-neoforge-1.21.1", "src", "main", "resources", "data")),
)


def ingredient_key(entry):
    """A canonical, tag-aware spelling of one key entry."""
    if not isinstance(entry, dict):
        return "invalid:" + json.dumps(entry, sort_keys=True)
    has_item = "item" in entry
    has_tag = "tag" in entry
    if has_item and not has_tag:
        return "item:" + str(entry["item"])
    if has_tag and not has_item:
        return "tag:" + str(entry["tag"])
    # Ingredient types such as neoforge:components carry a whole object; keep it
    # verbatim so it never accidentally compares equal to a plain item or tag.
    return "compound:" + json.dumps(entry, sort_keys=True)


def shrink(pattern):
    """``ShapedRecipePattern#shrink``: drop empty outer rows and columns."""
    rows = list(pattern)
    if not rows:
        return []
    first_column = 10 ** 9
    last_column = -1
    for row in rows:
        first_column = min(first_column, len(row) - len(row.lstrip(" ")))
        last_column = max(last_column, len(row.rstrip(" ")) - 1)
    while rows and not rows[0].strip():
        rows.pop(0)
    while rows and not rows[-1].strip():
        rows.pop()
    if not rows:
        return []
    return [row[first_column:last_column + 1] for row in rows]


def signature(key, pattern):
    """The grid a shaped recipe matches, independent of placement and mirroring."""
    rows = shrink(pattern)
    symbols = {}
    for row in rows:
        for char in row:
            if char != " ":
                symbols[char] = ingredient_key(key.get(char, {}))
    rendered = []
    for row in rows:
        rendered.append("".join(symbols.get(char, "?") if char != " " else "_" for char in row))
    normal = "/".join(rendered)
    mirrored = "/".join(row[::-1] for row in rendered)
    return min(normal, mirrored)


def result_id(recipe):
    result = recipe.get("result")
    if isinstance(result, dict):
        return result.get("id")
    if isinstance(result, str):
        return result
    return None


def walk_directory(root, problems, fatal):
    """Yield (label, recipe) for every shaped recipe JSON under ``root``."""
    base = os.path.abspath(root)
    for directory, subdirectories, filenames in os.walk(base):
        if RECIPE_SEGMENT not in directory + os.sep:
            continue
        subdirectories.sort()
        for name in sorted(filenames):
            if not name.endswith(".json") or name.startswith("_"):
                continue
            path = os.path.join(directory, name)
            label = os.path.relpath(path, REPO) if path.startswith(REPO + os.sep) else path
            try:
                with io.open(path, encoding="utf-8-sig") as handle:
                    recipe = json.load(handle)
            except (OSError, ValueError) as error:
                report_problem(problems, fatal, "cannot read %s: %s" % (label, error))
                continue
            if isinstance(recipe, dict) and recipe.get("type") == "minecraft:crafting_shaped":
                yield label, recipe, fatal


def walk_jar(path):
    """Yield (label, recipe, False) for every shaped recipe JSON inside a jar."""
    label_base = os.path.basename(path)
    try:
        with zipfile.ZipFile(path) as archive:
            for name in sorted(archive.namelist()):
                if not JAR_RECIPE.match(name) or not name.endswith(".json"):
                    continue
                if os.path.basename(name).startswith("_"):
                    continue
                try:
                    recipe = json.loads(archive.read(name).decode("utf-8-sig"))
                except ValueError:
                    continue
                if isinstance(recipe, dict) and recipe.get("type") == "minecraft:crafting_shaped":
                    yield label_base + "!" + name, recipe, False
    except (OSError, zipfile.BadZipFile) as error:
        print("warning: cannot read jar %s: %s" % (path, error), file=sys.stderr)


def report_problem(problems, fatal, message):
    problems.append((fatal, message))


def check(recipe, label, problems, strict):
    """Structural checks that would make the game reject or misread a recipe."""
    pattern = recipe.get("pattern")
    key = recipe.get("key")
    result = result_id(recipe)
    if not isinstance(result, str) or not result:
        report_problem(problems, strict, "%s: missing result id" % label)
    if not isinstance(pattern, list) or not pattern or not all(isinstance(row, str) for row in pattern):
        report_problem(problems, strict, "%s: pattern is not a list of strings" % label)
        return
    if len({len(row) for row in pattern}) != 1:
        report_problem(problems, strict, "%s: pattern rows differ in length" % label)
        return
    if not isinstance(key, dict):
        report_problem(problems, strict, "%s: missing key" % label)
        return
    used = {char for row in pattern for char in row if char != " "}
    undefined = sorted(used - set(key))
    if undefined:
        report_problem(problems, strict, "%s: pattern uses undefined symbols %s" % (label, ", ".join(undefined)))
    unused = sorted(set(key) - used)
    if unused:
        report_problem(problems, strict, "%s: key defines unused symbols %s" % (label, ", ".join(unused)))


def grid_rows(recipe):
    """The shrunken pattern rows, for printing a conflict."""
    return shrink(recipe.get("pattern") or [])


def legend(recipe, grid):
    chars = sorted({char for row in grid for char in row if char != " "})
    return ["%s = %s" % (char, ingredient_key(recipe.get("key", {}).get(char, {}))) for char in chars]


def main(argv=None):
    parser = argparse.ArgumentParser(description="Fail when two shaped recipes claim the same grid for different results.")
    parser.add_argument("--root", action="append", default=[], metavar="DIR", help="extra data directory to scan (repeatable)")
    parser.add_argument("--jar", action="append", default=[], metavar="JAR", help="extra jar to scan (repeatable)")
    parser.add_argument("--no-jars", action="store_true", help="do not scan run/mods")
    options = parser.parse_args(argv)

    roots = [(os.path.relpath(RESOURCES, REPO), RESOURCES)]
    for name, path in SIBLINGS:
        if os.path.isdir(path):
            roots.append((name, path))
    for path in options.root:
        if not os.path.isdir(path):
            parser.error("not a directory: %s" % path)
        roots.append(("--root " + path, path))

    jars = []
    mods = os.path.join(REPO, "run", "mods")
    if not options.no_jars and os.path.isdir(mods):
        for name in sorted(os.listdir(mods)):
            if name.endswith(".jar"):
                jars.append(os.path.join(mods, name))
    for path in options.jar:
        if not os.path.isfile(path):
            parser.error("not a file: %s" % path)
        jars.append(path)

    recipes = []
    problems = []
    print("Scanning shaped recipes:")
    for name, path in roots:
        fatal = os.path.abspath(path).startswith(REPO + os.sep)
        found = list(walk_directory(path, problems, fatal))
        recipes.extend(found)
        print("  %-24s %4d  %s" % (name, len(found), os.path.abspath(path)))
    for path in jars:
        found = list(walk_jar(path))
        recipes.extend(found)
        print("  %-24s %4d  %s" % ("jar", len(found), path))

    groups = {}
    for label, recipe, inside_repo in recipes:
        check(recipe, label, problems, inside_repo)
        groups.setdefault(signature(recipe.get("key", {}), recipe.get("pattern") or []), []).append((label, recipe))

    print("\n%d shaped recipes, %d distinct grids" % (len(recipes), len(groups)))

    conflicts = []
    for grid, members in sorted(groups.items()):
        results = {result_id(recipe) for _label, recipe in members}
        if len(members) > 1 and len(results) > 1:
            conflicts.append((grid, members))

    for fatal, message in problems:
        print("%s: %s" % ("error" if fatal else "warning", message))

    fatal_problems = [message for fatal, message in problems if fatal]
    if not conflicts and not fatal_problems:
        print("OK: no two shaped recipes share a grid for different results.")
        return 0

    for grid, members in conflicts:
        print("\nCONFLICT: the same grid produces different items")
        rows = grid_rows(members[0][1])
        for row in rows:
            print("    " + row)
        for entry in legend(members[0][1], rows):
            print("      " + entry)
        for label, recipe in members:
            print("    %-45s %s" % (result_id(recipe), label))
    if fatal_problems:
        print("\n%d malformed recipe(s) in this checkout." % len(fatal_problems))
    return 1


if __name__ == "__main__":
    sys.exit(main())
