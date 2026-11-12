#!/usr/bin/env python3
"""Fail when the Oddities chapter would not load inside the Forester's Manual.

Why this exists
---------------
This mod does not bring a book of its own. Patchouli 1.20 removed the "extend" flag,
and in exchange it lets any resource pack contribute pages to an existing book: the
content of book ``<namespace>:<name>`` is read from
``assets/<namespace>/patchouli_books/<name>/<lang>/{categories,entries}/<id>.json``,
from *every* pack in the resource manager. So this mod ships its pages under
``assets/forestry/patchouli_books/foresters_manual/`` and they appear inside
Forestry's own manual, next to Forestry's own pages - no new book, no file of
Forestry's replaced. The price is that the file paths, the entry ids they produce
(``forestry:<id>``), the category links, the recipe ids, the ``$(l:...)`` targets and
the translation keys all have to line up exactly, and Patchouli fails *softly* when
they do not:

* a missing translation key prints the key itself in the book - the book is
  ``"i18n": true``, so every name and every page text is a key, not prose;
* a link to an entry that does not exist still renders as text, so a typo silently
  stops being a link;
* a ``patchouli:crafting`` page whose recipe id is wrong logs a warning and draws an
  empty page;
* an unclosed ``$(/l)``, an unknown page type or a broken JSON throws while the book
  is built and leaves the *whole* manual at "Error loading and compiling book".

None of that fails a Gradle build, and none of it is visible in the repository, so
this script re-derives everything Patchouli derives from the paths and compares it
against the files that actually exist.

What it checks
--------------
* every page file parses and sits in the layout Patchouli reads, with the book's
  namespace (``forestry``) and language (``en_us`` - the language whose files are
  enumerated; other languages fall back to it);
* no page file of ours would *replace* one of Forestry's own pages;
* entry ``category``, category ``parent`` and every ``$(l:...)`` target resolve to an
  entry or category of the same book;
* every ``$(...)`` command is one Patchouli knows, and no ``$(/l)`` outlives its
  ``$(l:...)``;
* every name/description/text is a translation key that exists in this mod's
  ``en_us``; a missing ``zh_cn`` one is reported as a warning (the game falls back to
  English, so it is not broken, only untranslated);
* every recipe page points at a recipe that exists and is a crafting recipe, which is
  what ``patchouli:crafting`` filters for;
* every icon and spotlight item this mod names has an item model;
* every manual key in the language files is used by at least one page.

Forestry's own book and recipes are read from a sibling ``ForestryCE`` checkout when
there is one, or from jars given with ``--jar``; without either, the checks that need
Forestry's side are skipped with a note instead of guessed at.

Usage
-----
    python tools/check_manual.py
        Scan this checkout plus the sibling ForestryCE checkout / run/mods jars.

    python tools/check_manual.py --forestry D:/packs/ForestryCE/src/main/resources
        Point at a Forestry resource directory (repeatable; assets/data are searched).

    python tools/check_manual.py --jar forestry.jar --no-jars
        Check against a specific Forestry jar and skip run/mods.

Exit status: 0 when the chapter is consistent, 1 when it is not.
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

# The book this mod writes into, and the language directory Patchouli enumerates.
BOOK_NAMESPACE = "forestry"
BOOK_NAME = "foresters_manual"
BOOK_LANG = "en_us"
BOOK_ASSETS = os.path.join(RESOURCES, "assets", BOOK_NAMESPACE, "patchouli_books", BOOK_NAME, BOOK_LANG)

MOD_ID = "forestry_oddities"
LANG_EN = os.path.join(RESOURCES, "assets", MOD_ID, "lang", "en_us.json")
LANG_ZH = os.path.join(RESOURCES, "assets", MOD_ID, "lang", "zh_cn.json")

# Page types this chapter may use. Patchouli answers an unknown type by failing to
# build the book, so a new type has to be added here on purpose.
PAGE_TYPES = {
    "patchouli:text",
    "patchouli:spotlight",
    "patchouli:crafting",
    "patchouli:smelting",
    "patchouli:smoking",
    "patchouli:blasting",
    "patchouli:campfire_cooking",
    "patchouli:stonecutting",
    "patchouli:smithing",
    "patchouli:image",
    "patchouli:entity",
    "patchouli:multiblock",
    "patchouli:relations",
    "patchouli:quest",
    "patchouli:link",
    "patchouli:empty",
    "patchouli:template",
}
RECIPE_PAGES = {
    "patchouli:crafting",
    "patchouli:smelting",
    "patchouli:smoking",
    "patchouli:blasting",
    "patchouli:campfire_cooking",
    "patchouli:stonecutting",
    "patchouli:smithing",
}
# Recipe types the crafting page can render: it asks the recipe manager for the id and
# keeps it only when `Recipe#getType() == RecipeType.CRAFTING`, which every
# CraftingRecipe implementation answers, whatever its serializer is called.
CRAFTING_TYPES = {"minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:crafting_transmute"}

# Patchouli text commands (BookTextParser). `item` and `thing` are book macros that
# expand to a colour before parsing, but they are valid in the source.
STYLE_COMMANDS = {
    "br", "br2", "2br", "p",
    "/l", "/t",
    "k", "obf", "l", "bold", "m", "strike", "n", "underline", "o", "italic", "italics",
    "", "reset", "clear", "nocolor", "playername", "item", "thing",
}
COLOR_COMMAND = re.compile(r"^[0-9a-f]$")
HEX_COMMAND = re.compile(r"^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$")
LIST_COMMAND = re.compile(r"^li\d?$")
FUNCTION_PREFIXES = ("l:", "t:", "k:", "i18n:")

COMMAND = re.compile(r"\$\(([^)]*)\)")
LINK = re.compile(r"\$\(l:([^)]*)\)")
KEY_SHAPE = re.compile(r"^[a-z0-9_.-]+$")

SIBLINGS = (
    ("Forestry CE", os.path.join(REPO, "..", "ForestryCE", "src", "main", "resources")),
)


class Report:
    def __init__(self):
        self.errors = []
        self.warnings = []
        self.notes = []

    def error(self, message):
        self.errors.append(message)

    def warn(self, message):
        self.warnings.append(message)

    def note(self, message):
        self.notes.append(message)


def read_json(path):
    with io.open(path, encoding="utf-8-sig") as handle:
        return json.load(handle)


def walk_json(directory):
    for base, subdirectories, filenames in os.walk(directory):
        subdirectories.sort()
        for name in sorted(filenames):
            if name.endswith(".json") and not name.startswith("_"):
                yield os.path.join(base, name)


def resource_id(root, path, folder):
    """``.../<folder>/a/b.json`` -> the id Patchouli derives from it."""
    relative = os.path.relpath(path, os.path.join(root, folder))
    return relative[: -len(".json")].replace(os.sep, "/")


def collect_book(root, label, categories, entries, report, ours):
    """Add the categories and entries of one ``en_us`` directory to the two maps."""
    for folder, target, kind in (("categories", categories, "category"), ("entries", entries, "entry")):
        directory = os.path.join(root, folder)
        if not os.path.isdir(directory):
            continue
        for path in walk_json(directory):
            rid = "%s:%s" % (BOOK_NAMESPACE, resource_id(root, path, folder))
            shown = "%s/%s/%s.json" % (label, folder, resource_id(root, path, folder))
            try:
                page = read_json(path)
            except (OSError, ValueError) as error:
                report.error("%s: cannot read: %s" % (shown, error))
                continue
            if not isinstance(page, dict):
                report.error("%s: not a JSON object" % shown)
                continue
            if rid in target:
                report.error("%s: id %s is already taken by %s%s" % (
                    shown, rid, "this checkout" if ours and rid in target else label,
                    " (a page of Forestry's own would be replaced)" if not ours else ""))
                continue
            target[rid] = (shown, page, kind)


def collect_jar_book(path, categories, entries, report):
    """The same, for a Forestry jar: assets/forestry/patchouli_books/.../en_us/..."""
    prefix = "assets/%s/patchouli_books/%s/%s/" % (BOOK_NAMESPACE, BOOK_NAME, BOOK_LANG)
    try:
        with zipfile.ZipFile(path) as archive:
            for name in sorted(archive.namelist()):
                if not name.startswith(prefix) or not name.endswith(".json"):
                    continue
                rest = name[len(prefix):]
                folder, _, rid = rest.partition("/")
                if folder not in ("categories", "entries") or rid.startswith("_"):
                    continue
                target = categories if folder == "categories" else entries
                full = "%s:%s" % (BOOK_NAMESPACE, rid[: -len(".json")])
                if full in target:
                    continue
                try:
                    page = json.loads(archive.read(name).decode("utf-8-sig"))
                except ValueError as error:
                    report.warn("%s!%s: cannot read: %s" % (os.path.basename(path), name, error))
                    continue
                target[full] = ("%s!%s" % (os.path.basename(path), name), page,
                                "category" if folder == "categories" else "entry")
    except (OSError, zipfile.BadZipFile) as error:
        report.warn("cannot read jar %s: %s" % (path, error))


def collect_recipes(root, label, recipes, report):
    directory = os.path.join(root, "data")
    if not os.path.isdir(directory):
        return
    for path in walk_json(directory):
        parts = os.path.relpath(path, directory).split(os.sep)
        if len(parts) < 3 or parts[1] != "recipe":
            continue
        rid = "%s:%s" % (parts[0], "/".join(parts[2:])[: -len(".json")])
        try:
            recipe = read_json(path)
        except (OSError, ValueError) as error:
            report.warn("%s: cannot read recipe %s: %s" % (label, rid, error))
            continue
        if isinstance(recipe, dict) and rid not in recipes:
            recipes[rid] = recipe.get("type")


def collect_jar_recipes(path, recipes):
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            parts = name.split("/")
            if len(parts) < 5 or parts[0] != "data" or parts[2] != "recipe" or not name.endswith(".json"):
                continue
            rid = "%s:%s" % (parts[1], "/".join(parts[3:])[: -len(".json")])
            if rid in recipes:
                continue
            try:
                recipe = json.loads(archive.read(name).decode("utf-8-sig"))
            except ValueError:
                continue
            if isinstance(recipe, dict):
                recipes[rid] = recipe.get("type")


def check_command(command, report, where):
    if command in STYLE_COMMANDS or COLOR_COMMAND.match(command) or HEX_COMMAND.match(command) \
            or LIST_COMMAND.match(command) or command.startswith(FUNCTION_PREFIXES):
        return
    report.error("%s: $(%s) is not a Patchouli command" % (where, command))


def check_value(text, report, where, links):
    """Validate the markup of one rendered string: commands, links and length."""
    depth = 0
    for match in COMMAND.finditer(text):
        command = match.group(1)
        check_command(command, report, where)
        if command.startswith("l:"):
            depth += 1
            target = command[2:].split("#")[0]
            links.append((target, where))
        elif command == "/l":
            if depth == 0:
                report.error("%s: $(/l) closes a link that was never opened" % where)
            else:
                depth -= 1
        elif command in ("", "reset", "clear"):
            # `$()` resets the style stack, which is how a link written as
            # `$(l:somewhere)text$()` is closed.
            depth = 0
    if depth:
        report.error("%s: %d $(l:...) link(s) left open" % (where, depth))

    plain = COMMAND.sub("", text).strip()
    if len(plain) > 700:
        report.warn("%s: %d characters of text; Patchouli will shrink the page to fit" % (where, len(plain)))


def check_text(text, report, where, lang_en, lang_zh, links, used_keys):
    """Check one name/description/text field of a page.

    The book is ``"i18n": true``, so such a field is a translation key and the text
    that actually reaches the page - with its macros and its links - lives in this
    mod's language files. A literal value works too (Patchouli looks it up, finds
    nothing and keeps it), which is why it is only reported, not rejected.
    """
    if not isinstance(text, str):
        report.error("%s: expected a string, got %s" % (where, type(text).__name__))
        return
    if KEY_SHAPE.match(text) and " " not in text:
        used_keys.add(text)
        if text not in lang_en:
            report.error("%s: translation key %s is missing from assets/%s/lang/en_us.json" % (where, text, MOD_ID))
        if text not in lang_zh:
            report.warn("%s: translation key %s has no zh_cn translation" % (where, text))
    else:
        report.warn("%s: %r is a literal, not a key - other languages will show it unchanged" % (where, text))
        check_value(text, report, where, links)


def check_translations(used_keys, lang_en, lang_zh, report, links):
    """Check the markup of the text behind every key, in both languages."""
    for key in sorted(used_keys):
        for name, table in (("en_us", lang_en), ("zh_cn", lang_zh)):
            if key in table:
                check_value(table[key], report, "%s (%s)" % (key, name), links)


def resolve(target, known):
    if ":" not in target:
        target = "%s:%s" % (BOOK_NAMESPACE, target)
    return target in known


def check_icon(item, report, where):
    """Every item this mod names must have an item model, or the icon renders nothing."""
    if not isinstance(item, str):
        report.error("%s: icon/item is not a string" % where)
        return
    namespace, _, path = item.partition(":")
    if not path:
        report.error("%s: %s is not a namespaced item id" % (where, item))
        return
    if namespace != MOD_ID:
        return  # Forestry's items are Forestry's business.
    model = os.path.join(RESOURCES, "assets", MOD_ID, "models", "item", path + ".json")
    if not os.path.isfile(model):
        report.error("%s: %s has no item model (%s)" % (where, item, os.path.relpath(model, REPO)))


def check_category(rid, shown, page, report, lang_en, lang_zh, links, used_keys, categories, known, forestry_known):
    for field in ("name", "description", "icon"):
        if field not in page:
            report.error("%s: category has no %s" % (shown, field))
    for field in ("name", "description"):
        if field in page:
            check_text(page[field], report, "%s.%s" % (shown, field), lang_en, lang_zh, links, used_keys)
    if "icon" in page:
        check_icon(page["icon"], report, shown)
    parent = page.get("parent", "")
    if parent:
        if ":" not in parent:
            report.error("%s: parent %s must be fully qualified (domain:name)" % (shown, parent))
        elif not resolve(parent, known) and forestry_known:
            report.error("%s: parent category %s does not exist" % (shown, parent))


def check_entry(rid, shown, page, report, lang_en, lang_zh, links, used_keys, categories, known, forestry_known):
    for field in ("name", "category", "icon", "pages"):
        if field not in page:
            report.error("%s: entry has no %s" % (shown, field))
    if "name" in page:
        check_text(page["name"], report, "%s.name" % shown, lang_en, lang_zh, links, used_keys)
    if "icon" in page:
        check_icon(page["icon"], report, shown)

    category = page.get("category")
    if isinstance(category, str):
        if ":" not in category:
            report.error("%s: category %s must be fully qualified (domain:name)" % (shown, category))
        elif category not in categories and forestry_known:
            report.error("%s: category %s does not exist in this book" % (shown, category))

    pages = page.get("pages")
    if not isinstance(pages, list) or not pages:
        report.error("%s: pages must be a non-empty list" % shown)
        return
    for index, child in enumerate(pages):
        where = "%s page %d" % (shown, index)
        if not isinstance(child, dict):
            report.error("%s: not a JSON object" % where)
            continue
        kind = child.get("type")
        if kind not in PAGE_TYPES:
            report.warn("%s: page type %r is not in this script's list - Patchouli fails to build the whole "
                        "book on a type it does not know, so add it here once it is known to exist" % (where, kind))
            continue
        if kind == "patchouli:text" and "text" not in child:
            report.error("%s: a text page needs a text" % where)
        if kind == "patchouli:spotlight" and "item" not in child:
            report.error("%s: a spotlight page needs an item" % where)
        if kind in RECIPE_PAGES and not (child.get("recipe") or child.get("recipe2")):
            report.error("%s: a recipe page needs a recipe" % where)
        for field in ("text", "title"):
            if field in child:
                check_text(child[field], report, "%s.%s" % (where, field), lang_en, lang_zh, links, used_keys)
        if "item" in child:
            check_icon(child["item"], report, where)


def check_recipes(entries, recipes, report, forestry_known):
    for rid, (shown, page, _kind) in entries.items():
        for index, child in enumerate(page.get("pages") or []):
            if not isinstance(child, dict) or child.get("type") not in RECIPE_PAGES:
                continue
            for field in ("recipe", "recipe2"):
                recipe = child.get(field)
                if not isinstance(recipe, str):
                    continue
                where = "%s page %d %s" % (shown, index, field)
                if recipe not in recipes:
                    if forestry_known or not recipe.startswith(BOOK_NAMESPACE + ":"):
                        report.error("%s: recipe %s does not exist" % (where, recipe))
                    else:
                        report.warn("%s: cannot verify %s (Forestry resources not found)" % (where, recipe))
                    continue
                recipe_type = recipes[recipe]
                if child.get("type") == "patchouli:crafting" and recipe_type not in CRAFTING_TYPES \
                        and not (recipe_type or "").split(":")[-1].startswith("crafting"):
                    report.error("%s: %s is %s, which patchouli:crafting cannot show" % (where, recipe, recipe_type))


def check_links(links, known, report, forestry_known):
    for target, where in links:
        if target.startswith("http"):
            continue
        full = target if ":" in target else "%s:%s" % (BOOK_NAMESPACE, target)
        if full in known:
            continue
        if forestry_known:
            report.error("%s: link target %s is not an entry or category of %s:%s" % (
                where, full, BOOK_NAMESPACE, BOOK_NAME))
        else:
            report.warn("%s: cannot verify link target %s (Forestry resources not found)" % (where, full))


def main(argv=None):
    parser = argparse.ArgumentParser(description="Fail when this mod's pages would not load inside the Forester's Manual.")
    parser.add_argument("--forestry", action="append", default=[], metavar="DIR",
                        help="Forestry resource directory to read the book and recipes from (repeatable)")
    parser.add_argument("--jar", action="append", default=[], metavar="JAR",
                        help="Forestry jar to read the book and recipes from (repeatable)")
    parser.add_argument("--no-jars", action="store_true", help="do not scan run/mods")
    options = parser.parse_args(argv)

    report = Report()

    # --- this checkout -------------------------------------------------------------------
    categories, entries = {}, {}
    collect_book(BOOK_ASSETS, "assets", categories, entries, report, ours=True)

    lang_en = read_json(LANG_EN) if os.path.isfile(LANG_EN) else {}
    lang_zh = read_json(LANG_ZH) if os.path.isfile(LANG_ZH) else {}
    if not lang_en:
        report.error("%s is missing or empty" % os.path.relpath(LANG_EN, REPO))

    recipes = {}
    collect_recipes(RESOURCES, "this checkout", recipes, report)

    # --- everything else that contributes to the same book -------------------------------
    forestry_roots = []
    for name, path in SIBLINGS:
        if os.path.isdir(path):
            forestry_roots.append((name, path))
    for path in options.forestry:
        if not os.path.isdir(path):
            parser.error("not a directory: %s" % path)
        forestry_roots.append(("--forestry " + path, path))

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

    foreign_categories, foreign_entries = {}, {}
    for name, root in forestry_roots:
        book = os.path.join(root, "assets", BOOK_NAMESPACE, "patchouli_books", BOOK_NAME, BOOK_LANG)
        if os.path.isdir(book):
            collect_book(book, name, foreign_categories, foreign_entries, report, ours=False)
        collect_recipes(root, name, recipes, report)
        generated = os.path.join(os.path.dirname(root), "generated", "resources")
        if os.path.isdir(generated):
            collect_recipes(generated, name + " (generated)", recipes, report)
    for path in jars:
        collect_jar_book(path, foreign_categories, foreign_entries, report)
        try:
            collect_jar_recipes(path, recipes)
        except (OSError, zipfile.BadZipFile) as error:
            report.warn("cannot read jar %s: %s" % (path, error))

    forestry_known = bool(foreign_categories or foreign_entries)
    if not forestry_known:
        report.note("Forestry's own book was not found; links into it are only reported, not resolved.")

    # A page of ours on an id Forestry already uses does not sit *next to* Forestry's page: the
    # resource manager keeps one file per id, so ours would silently replace it. Both files parse,
    # both would pass the checks below, and only this comparison sees it.
    for kind, mine, theirs in (("category", categories, foreign_categories),
                               ("entry", entries, foreign_entries)):
        for rid in sorted(set(mine) & set(theirs)):
            report.error("%s: id %s is Forestry's own %s; a page of this mod would replace it"
                         % (mine[rid][0], rid, kind))

    # Forestry's ids first, then ours, so a link may point at either; the pages of ours are the
    # ones to check anyway.
    known_categories = dict(foreign_categories)
    known_categories.update(categories)
    known_entries = dict(foreign_entries)
    known_entries.update(entries)
    known = set(known_categories) | set(known_entries)

    links, used_keys = [], set()
    for rid, (shown, page, _kind) in sorted(categories.items()):
        check_category(rid, shown, page, report, lang_en, lang_zh, links, used_keys,
                       known_categories, known, forestry_known)
    for rid, (shown, page, _kind) in sorted(entries.items()):
        check_entry(rid, shown, page, report, lang_en, lang_zh, links, used_keys,
                    known_categories, known, forestry_known)

    check_translations(used_keys, lang_en, lang_zh, report, links)
    check_links(links, known, report, forestry_known)
    check_recipes(entries, recipes, report, forestry_known)

    manual_keys = {key for key in lang_en if key.startswith(MOD_ID + ".patchouli.")}
    for key in sorted(manual_keys - used_keys):
        report.warn("%s: not used by any page of the chapter" % key)

    print("Oddities chapter of %s:%s" % (BOOK_NAMESPACE, BOOK_NAME))
    print("  pages            %3d  %s" % (len(categories) + len(entries), os.path.relpath(BOOK_ASSETS, REPO)))
    print("  categories       %3d  %s" % (len(categories), ", ".join(sorted(categories)) or "-"))
    print("  entries          %3d  %s" % (len(entries), ", ".join(sorted(entries)) or "-"))
    print("  translation keys %3d  (%d in en_us, %d in zh_cn)" % (
        len(used_keys), len([key for key in used_keys if key in lang_en]),
        len([key for key in used_keys if key in lang_zh])))
    print("  links            %3d  %d resolvable" % (len(links), len([
        1 for target, _where in links
        if (target if ":" in target else "%s:%s" % (BOOK_NAMESPACE, target)) in known])))
    print("  recipes          %3d  in this checkout" % len([
        1 for rid in recipes if rid.startswith(MOD_ID + ":")]))
    print("  Forestry's book  %s" % ("found" if forestry_known else "not found"))

    for message in report.notes:
        print("\nnote: " + message)
    for message in report.warnings:
        print("warning: " + message)
    for message in report.errors:
        print("error: " + message)

    if report.errors:
        print("\n%d problem(s) the book would show or log." % len(report.errors))
        return 1
    if forestry_known:
        print("\nOK: the chapter loads inside Forestry's manual with every link, recipe and key resolved.")
    else:
        print("\nOK: the chapter is consistent within this checkout; the links into Forestry's book were "
              "not verified because Forestry's resources were not found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
