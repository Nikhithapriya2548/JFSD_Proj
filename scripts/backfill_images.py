#!/usr/bin/env python3
"""One-time backfill: give every product without an image a real photo URL.

Uses loremflickr keyword images with ?lock=<id> so each product gets a
STABLE, unique photo (same URL always returns the same image).
Safe to re-run: skips products that already have an imageUrl.
"""
import sys

import requests

API_URL = "http://localhost:8081/api/v1/products"
KEYWORD = {"Laptops": "laptop", "Tablets": "tablet", "Phones": "smartphone"}


def image_for(product):
    kw = KEYWORD.get(product.get("category"), "gadget")
    return f"https://loremflickr.com/400/300/{kw}?lock={product['id']}"


def main():
    try:
        products = requests.get(API_URL, timeout=15).json()
    except requests.RequestException as e:
        print(f"ERROR: cannot reach {API_URL}: {e}")
        sys.exit(1)

    updated, skipped, failed = 0, 0, []
    for p in products:
        if p.get("imageUrl"):
            skipped += 1
            continue
        payload = {
            "name": p["name"],
            "description": p.get("description") or "",
            "price": p["price"],
            "stockQuantity": p["stockQuantity"],
            "category": p.get("category"),
            "imageUrl": image_for(p),
        }
        try:
            r = requests.put(f"{API_URL}/{p['id']}", json=payload, timeout=15)
            if r.status_code == 200:
                updated += 1
            else:
                failed.append((p["id"], f"HTTP {r.status_code}: {r.text[:150]}"))
        except requests.RequestException as e:
            failed.append((p["id"], str(e)))

    print(f"Updated: {updated} | already had images: {skipped} | failed: {len(failed)}")
    for pid, reason in failed:
        print(f"  FAILED id={pid}: {reason}")


if __name__ == "__main__":
    main()
