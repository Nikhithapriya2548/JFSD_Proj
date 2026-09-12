#!/usr/bin/env python3
"""One-time top-up: seed same-name price variants the main seeder dedupes away.

Main seed keeps (name, category); this adds (name, category, price) variants
not already in the catalog, taking it from ~79 toward ~150 items.
Images: keyless loremflickr category URLs (no Unsplash budget needed).
Safe to re-run: skips (name, price) pairs already present.
"""
import sys

sys.path.insert(0, __import__("os").path.dirname(__file__))
import seed_products as s

API = s.API_URL


def main():
    existing = requests_get_all()
    have = {(p["name"], p.get("category"), float(p["price"])) for p in existing}
    print(f"in catalog: {len(existing)}")

    added, failed = 0, []
    for cat, path in s.CATEGORIES.items():
        html = s.fetch(s.BASE_URL + path)
        if not html:
            continue
        for p in s.parse_products(html):
            key = (p["name"], cat, float(p["price"]))
            if key in have:
                continue
            have.add(key)
            payload = {
                "name": p["name"],
                "description": p["description"],
                "price": p["price"],
                # same enrichment as main seed: ~15% out of stock
                "stockQuantity": 0 if s.random.random() < s.OUT_OF_STOCK_RATIO
                                 else s.random.randint(1, 100),
                "category": cat,
                "imageUrl": s.validate_image(s.category_image(cat, f"{p['name']}-{p['price']}")),
            }
            try:
                r = s.requests.post(API, json=payload, timeout=15)
                if r.status_code in (200, 201):
                    added += 1
                else:
                    failed.append((p["name"], f"HTTP {r.status_code}: {r.text[:120]}"))
            except Exception as e:  # noqa: BLE001 - report, don't crash
                failed.append((p["name"], str(e)))
            s.time.sleep(0.1)
    print(f"variants added: {added} | failed: {len(failed)}")
    for n, why in failed:
        print(f"  FAILED '{n}': {why}")


def requests_get_all():
    import requests
    return requests.get(API, timeout=15).json()


if __name__ == "__main__":
    main()
