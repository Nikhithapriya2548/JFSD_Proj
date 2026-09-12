#!/usr/bin/env python3
"""Match each product to a photo that fits its NAME via Unsplash search.

- One Unsplash search per DISTINCT product name (duplicates share the result).
- Accepts a photo only if its alt/description mentions a word from the
  product name (brand/model token); otherwise keeps the category image.
- Resumable: results saved to matched_names.json; stops gracefully when the
  Unsplash hourly rate limit runs out (free tier: 50/hr). Re-run later.
- Never prints API keys.
"""
import json
import os
import sys
import time

import requests
from dotenv import load_dotenv

load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

API_URL = "http://localhost:8081/api/v1/products"
STATE_FILE = os.path.join(os.path.dirname(__file__), "matched_names.json")
NON_PRODUCT_WORDS = ("person", "people", "hand", "hands", "outdoor", "cafe",
                     "lifestyle", "crowd", "street", "beach")
KEY = os.getenv("UNSPLASH_ACCESS_KEY")


def clean_name(name):
    return name.replace(".", "").strip()


def tokens(name):
    return [t.lower() for t in clean_name(name).split() if len(t) > 2][:3]


def search(name):
    """Returns (urls_list, rate_remaining)."""
    try:
        r = requests.get(
            "https://api.unsplash.com/search/photos",
            params={"query": clean_name(name), "per_page": 10,
                    "orientation": "squarish", "order_by": "relevant",
                    "client_id": KEY},
            timeout=20,
        )
        remaining = int(r.headers.get("X-RateLimit-Remaining", 1))
        if r.status_code == 429:
            return None, 0
        if r.status_code != 200:
            print(f"  search '{name}': HTTP {r.status_code}")
            return [], remaining
        return r.json().get("results", []), remaining
    except requests.RequestException as e:
        print(f"  search '{name}': network error ({e})")
        return [], 99


def pick(results, name):
    toks = tokens(name)
    for p in results:
        text = f"{p.get('alt_description') or ''} {p.get('description') or ''}".lower()
        if any(w in text for w in NON_PRODUCT_WORDS):
            continue
        if any(t in text for t in toks):
            url = p.get("urls", {}).get("small")
            if url:
                return url, text[:80]
    return None, None


def main():
    if not KEY or "PASTE" in KEY:
        print("ERROR: no Unsplash key in scripts/.env")
        sys.exit(1)
    products = requests.get(API_URL, timeout=15).json()
    state = {}
    if os.path.exists(STATE_FILE):
        state = json.load(open(STATE_FILE))

    names = sorted({p["name"] for p in products})
    todo = [n for n in names if n not in state]
    print(f"distinct names: {len(names)} | already matched: {len(state)} | todo: {len(todo)}")

    matched = kept = 0
    for name in todo:
        results, remaining = search(name)
        if results is None:  # rate limited
            print("Rate limit hit — stopping. Re-run in an hour to continue.")
            break
        url, desc = pick(results, name)
        if url:
            state[name] = {"imageUrl": url, "desc": desc}
            matched += 1
            print(f"  MATCH '{name}' <- [{desc}]")
        else:
            state[name] = {"imageUrl": None}
            kept += 1
            print(f"  keep category image: '{name}' (no name-matching photo)")
        json.dump(state, open(STATE_FILE, "w"), indent=1)
        if remaining < 2:
            print("Budget nearly out — stopping. Re-run in an hour to continue.")
            break
        time.sleep(1)

    # Apply: PUT every product whose name has a matched URL
    updated = 0
    for p in products:
        entry = state.get(p["name"], {})
        url = entry.get("imageUrl") if isinstance(entry, dict) else None
        if not url or p.get("imageUrl") == url:
            continue
        payload = {"name": p["name"], "description": p.get("description") or "",
                   "price": p["price"], "stockQuantity": p["stockQuantity"],
                   "category": p.get("category"), "imageUrl": url}
        try:
            r = requests.put(f"{API_URL}/{p['id']}", json=payload, timeout=15)
            if r.status_code == 200:
                updated += 1
            else:
                print(f"  PUT failed id={p['id']}: HTTP {r.status_code}")
        except requests.RequestException as e:
            print(f"  PUT failed id={p['id']}: {e}")

    print(f"\nDone: names matched this run: {matched} | kept category image: {kept} "
          f"| products updated: {updated}")


if __name__ == "__main__":
    main()
