#!/usr/bin/env python3
"""Generate a PostgreSQL transaction for an audited copy of the legacy vocabulary file."""
import argparse
import json
from datetime import datetime, timezone


def text(value, max_length, required=False):
    if value is None and not required:
        return "NULL"
    if not isinstance(value, str) or "\0" in value or len(value) > max_length:
        raise ValueError(f"Expected text of at most {max_length} characters")
    if required and not value.strip():
        raise ValueError("Required vocabulary field is blank")
    return "'" + value.replace("'", "''") + "'"


def export(data, owners):
    statements = ["BEGIN;", "SET LOCAL standard_conforming_strings = on;"]
    seen_sets = set()
    for item in data["sets"]:
        set_id = item["id"]
        if not isinstance(set_id, str) or set_id in seen_sets:
            raise ValueError("Invalid or duplicate legacy set ID")
        seen_sets.add(set_id)
        community = item.get("isCommunity") is True
        email = owners.get(set_id)
        if not community and email is None:
            continue

        owner = "NULL"
        if email is not None:
            if not isinstance(email, str) or not email.strip():
                raise ValueError("Owner email cannot be blank")
            email_sql = text(email, 100, required=True)
            statements.append(
                f"SELECT 1 / (SELECT count(*)::int FROM users WHERE email = {email_sql});"
            )
            owner = f"(SELECT id FROM users WHERE email = {email_sql})"

        timestamp = datetime.fromisoformat(item["updatedAt"].replace("Z", "+00:00"))
        if timestamp.tzinfo is None:
            timestamp = timestamp.replace(tzinfo=timezone.utc)
        level = item.get("level")
        if level is not None and level not in {"N1", "N2", "N3", "N4", "N5"}:
            raise ValueError("Invalid JLPT level")
        values = [
            text(set_id, 128, required=True), "0", owner,
            text(item["title"], 200, required=True), text(item.get("description"), 2000),
            text(level, 2), str(community).upper(), text(timestamp.isoformat(), 40),
        ]
        insert = (
            "INSERT INTO vocabulary_sets (id, version, owner_id, title, description, level, community, updated_at) "
            f"VALUES ({', '.join(values)}) ON CONFLICT (id) DO NOTHING"
        )
        rows = []
        for word in data["words"].get(set_id, []):
            columns = [
                text(word["id"], 128, required=True), text(word["japanese"], 200, required=True),
                text(word["reading"], 200, required=True), text(word["meaning"], 1000, required=True),
                text(word.get("example"), 2000), text(word.get("exampleMeaning"), 2000),
            ]
            rows.append(f"({', '.join(columns)})")
        if rows:
            statements.append(
                f"WITH inserted AS ({insert} RETURNING id) "
                "INSERT INTO vocabulary_set_words (set_id, word_id, japanese, reading, meaning, example, example_meaning) "
                "SELECT inserted.id, words.* FROM inserted CROSS JOIN (VALUES "
                + ", ".join(rows) + ") AS words(word_id, japanese, reading, meaning, example, example_meaning);"
            )
        else:
            statements.append(insert + ";")

    unknown = owners.keys() - seen_sets
    if unknown:
        raise ValueError(f"Owner mapping includes unknown legacy set IDs: {sorted(unknown)}")
    statements.append("COMMIT;")
    return "\n".join(statements)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("legacy_json", type=argparse.FileType("r", encoding="utf-8"))
    parser.add_argument("--owners", type=argparse.FileType("r", encoding="utf-8"),
                        help="JSON object mapping audited legacy set IDs to emails of existing accounts")
    args = parser.parse_args()
    print(export(json.load(args.legacy_json), json.load(args.owners) if args.owners else {}))


if __name__ == "__main__":
    main()
