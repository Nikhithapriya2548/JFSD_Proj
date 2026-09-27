#!/usr/bin/env python3
"""Seed the OmniShop product catalog by scraping a legal scraping sandbox.

Source: https://webscraper.io/test-sites/e-commerce/allinone
(a public site explicitly built for scraping practice — stable HTML,
real listings across laptops/tablets/phones, not for sale).

Flow: scrape -> enrich (stock) -> dedupe/validate -> POST to product-service.
Prerequisite: product-service running (docker compose up) on localhost:8081.
"""

import random
import sys
import time
import os

import requests
from bs4 import BeautifulSoup

try:
    from dotenv import load_dotenv
    load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))
except ImportError:
    pass  # python-dotenv optional; image path below needs no keys

# Unsplash credentials (optional — current seeder uses keyless loremflickr).
# Only UNSPLASH_ACCESS_KEY would ever be used for public search API calls;
# UNSPLASH_SECRET_KEY is stored for completeness/future OAuth flows and is
# NOT required (or used) for the search-and-seed use case.
UNSPLASH_ACCESS_KEY = os.getenv("UNSPLASH_ACCESS_KEY")
UNSPLASH_SECRET_KEY = os.getenv("UNSPLASH_SECRET_KEY")  # stored only, never used

random.seed(42)  # reproducible stock assignment

BASE_URL = "https://webscraper.io"
CATEGORIES = {
    "Laptops": "/test-sites/e-commerce/allinone/computers/laptops",
    "Tablets": "/test-sites/e-commerce/allinone/computers/tablets",
    "Phones": "/test-sites/e-commerce/allinone/phones/touch",
}
API_URL = "http://localhost:8081/api/v1/products"
MIN_PRODUCTS, MAX_PRODUCTS = 100, 150
OUT_OF_STOCK_RATIO = 0.15

# Real category photos (sandbox has no per-product images — every listing
# reuses the same cart icon). ?lock=N makes each URL return a stable image.
IMAGE_KEYWORD = {"Laptops": "laptop", "Tablets": "tablet", "Phones": "smartphone"}

# Unsplash search terms per catalog category
UNSPLASH_TERM = {
    "Laptops": "laptop computer",
    "Tablets": "tablet device",
    "Phones": "smartphone",
}
UNSPLASH_SEARCH_URL = "https://api.unsplash.com/search/photos"


def category_image(category, seed_key):
    kw = IMAGE_KEYWORD.get(category, "gadget")
    lock = abs(hash(seed_key)) % 500 + 1
    return f"https://loremflickr.com/400/300/{kw}?lock={lock}"


def fetch_images_for_category(category: str, count: int = 30) -> list[str]:
    """Fetch up to `count` small image URLs from Unsplash for a category.

    Uses only UNSPLASH_ACCESS_KEY (public search API). On any failure
    (missing key, non-200, network error) prints the error and returns [].
    """
    key = os.getenv("UNSPLASH_ACCESS_KEY")
    if not key or "PASTE" in key or "your_access" in key:
        print(f"  [WARN] {category}: UNSPLASH_ACCESS_KEY missing — no images fetched")
        return []
    term = UNSPLASH_TERM.get(category, category)
    try:
        r = requests.get(
            UNSPLASH_SEARCH_URL,
            params={"query": term, "per_page": count, "client_id": key},
            headers=HEADERS,
            timeout=20,
        )
        if r.status_code != 200:
            print(f"  [WARN] {category}: Unsplash HTTP {r.status_code}: {r.text[:200]}")
            return []
        urls = [
            item["urls"]["small"]
            for item in r.json().get("results", [])
            if item.get("urls", {}).get("small")
        ]
        print(f"  [OK] {category}: fetched {len(urls)} images")
        return urls
    except requests.RequestException as e:
        print(f"  [WARN] {category}: Unsplash request failed: {e}")
        return []

HEADERS = {
    "User-Agent": (
        "OmniShop-Student-Seeder/1.0 "
        "(college full-stack project catalog seeding; educational use)"
    )
}

errors = []


def warn(msg):
    errors.append(msg)
    print(f"  [WARN] {msg}")


def fetch(url):
    """GET with politeness delay + jitter. Returns HTML text or None."""
    time.sleep(0.5 + random.random() * 0.5)
    try:
        r = requests.get(url, headers=HEADERS, timeout=20)
        r.raise_for_status()
        return r.text
    except requests.RequestException as e:
        warn(f"fetch failed {url}: {e}")
        return None


def parse_products(html):
    """Extract (name, price, description, imageUrl) from listing cards. Skips bad rows."""
    soup = BeautifulSoup(html, "lxml")
    items = []
    for card in soup.select("div.thumbnail"):
        try:
            title_el = card.select_one("a.title")
            price_el = card.select_one("h4.price")
            desc_el = card.select_one("p.description")
            img_el = card.select_one("img")
            if not title_el or not price_el:
                warn("skipping card: missing title or price element")
                continue
            name = title_el.get_text(strip=True)
            price_text = price_el.get_text(strip=True)
            price = float(price_text.replace("$", "").replace(",", "").strip())
            description = desc_el.get_text(strip=True) if desc_el else ""
            if not name:
                warn("skipping card: empty name")
                continue
            # Resolve relative src (e.g. "/images/...") to absolute URL
            image_url = None
            if img_el and img_el.get("src"):
                src = img_el["src"].strip()
                image_url = src if src.startswith("http") else BASE_URL + src
            items.append({"name": name, "price": price,
                          "description": description, "image_url": image_url})
        except (ValueError, AttributeError) as e:
            warn(f"skipping card: unparsable price/name ({e})")
    return items


def validate_image(url):
    """HEAD-check that the image resolves. Returns url or None."""
    if not url:
        return None
    try:
        r = requests.head(url, headers=HEADERS, timeout=10, allow_redirects=True)
        if r.status_code == 200:
            return url
        warn(f"image HEAD {r.status_code}, falling back to null: {url}")
        return None
    except requests.RequestException as e:
        warn(f"image unreachable, falling back to null: {url} ({e})")
        return None


NON_PRODUCT_WORDS = ("person", "people", "hand", "hands", "outdoor", "cafe",
                     "lifestyle", "crowd", "street", "beach")


def fetch_images_for_category(category: str, count: int = 30) -> list:
    """Return local generated images instead of Unsplash."""
    if "laptop" in category.lower():
        return ["/laptop.png"]
    if "tablet" in category.lower():
        return ["/tablet.png"]
    if "phone" in category.lower():
        return ["/phone.png"]
    return ["/placeholder-product.png"]


def pagination_links(html, category_path):
    """All same-section page links (handles ?page=N style pagination)."""
    soup = BeautifulSoup(html, "lxml")
    links = set()
    for a in soup.select("ul.pagination a[href]"):
        href = a.get("href", "")
        if href.startswith(category_path) or href.startswith("/test-sites/e-commerce/allinone"):
            links.add(BASE_URL + href if href.startswith("/") else href)
    return links


def scrape_category(category, path, seen, collected):
    """Loop pages until no new products, or the global MAX is reached."""
    visited = set()
    queue = [BASE_URL + path]
    while queue and len(collected) < MAX_PRODUCTS:
        url = queue.pop(0)
        if url in visited:
            continue
        visited.add(url)
        print(f"  fetching [{category}] {url}")
        html = fetch(url)
        if not html:
            continue
        for p in parse_products(html):
            key = (p["name"], category)
            if key in seen:
                continue
            seen.add(key)
            # Enrich: ~15% out of stock so the frontend badge gets real data
            stock = 0 if random.random() < OUT_OF_STOCK_RATIO else random.randint(1, 100)
            # imageUrl assigned after scraping from the per-category Unsplash cache
            collected.append({**p, "category": category,
                              "stockQuantity": stock, "imageUrl": None})
            if len(collected) >= MAX_PRODUCTS:
                break
        for link in pagination_links(html, path):
            if link not in visited and link not in queue:
                queue.append(link)


def check_existing():
    """Idempotency safeguard: warn if the catalog already has products."""
    try:
        r = requests.get(API_URL, timeout=10)
        r.raise_for_status()
        existing = r.json()
        count = len(existing) if isinstance(existing, list) else 0
    except requests.RequestException as e:
        print(f"ERROR: cannot reach product-service at {API_URL}: {e}")
        print("Start it first: docker compose up --build -d")
        sys.exit(1)
    if count > 0:
        print(f"product-service already has {count} product(s).")
        try:
            answer = input("Seed anyway (may create duplicates)? [y/N] ").strip().lower()
        except EOFError:
            answer = "n"
        if answer != "y":
            print("Aborted by user. Nothing seeded.")
            sys.exit(0)
    return count


def admin_headers():
    """Wave 2: product writes need ADMIN. Log in and return auth headers."""
    try:
        r = requests.post("http://localhost:8083/api/v1/users/login",
                          json={"email": "admin@omnishop.local",
                                "password": "admin123"}, timeout=10)
        r.raise_for_status()
        return {"Authorization": f"Bearer {r.json()['token']}"}
    except requests.RequestException as e:
        print(f"ERROR: admin login failed: {e}")
        sys.exit(1)


def load_products(products, image_cache, headers):
    """POST each product. Returns (loaded, failed_list, with_images)."""
    loaded, failed, with_images = 0, [], 0
    for p in products:
        urls = image_cache.get(p.get("category"), [])
        image_url = random.choice(urls) if urls else None
        payload = {
            "name": p["name"],
            "description": p["description"],
            "price": p["price"],
            "stockQuantity": p["stockQuantity"],
            "category": p["category"],
            "imageUrl": image_url,
        }
        try:
            r = requests.post(API_URL, json=payload, headers=headers, timeout=15)
            if r.status_code in (200, 201):
                loaded += 1
                if image_url:
                    with_images += 1
            else:
                failed.append((p["name"], f"HTTP {r.status_code}: {r.text[:200]}"))
        except requests.RequestException as e:
            failed.append((p["name"], str(e)))
        time.sleep(0.1)  # don't overwhelm the local service
    return loaded, failed, with_images


def main():
    print("=== OmniShop catalog seeder ===")
    check_existing()

    seen, collected = set(), []
    for category, path in CATEGORIES.items():
        scrape_category(category, path, seen, collected)
        print(f"  -> {category}: {sum(1 for p in collected if p['category'] == category)} so far")
        if len(collected) >= MAX_PRODUCTS:
            break

    if len(collected) < MIN_PRODUCTS:
        print(f"NOTE: only {len(collected)} usable listings found (< {MIN_PRODUCTS}); proceeding anyway.")
    else:
        print(f"Scraped {len(collected)} valid products.")

    # One Unsplash call per distinct category (~3 API calls total)
    image_cache = {}
    for category in sorted({p["category"] for p in collected}):
        image_cache[category] = fetch_images_for_category(category)
        print(f"  images fetched for {category}: {len(image_cache[category])}")

    loaded, failed, with_images = load_products(collected, image_cache, admin_headers())

    # ---- summary ----
    print("\n=== Summary ===")
    print(f"Total scraped: {len(collected)} | loaded: {loaded} | failed: {len(failed)}")
    print(f"Loaded with imageUrl: {with_images} | loaded with imageUrl=None: "
          f"{loaded - with_images} (empty Unsplash result for that category)")
    for name, reason in failed:
        print(f"  FAILED '{name}': {reason}")

    print("\nProducts per category:")
    for category in CATEGORIES:
        print(f"  {category}: {sum(1 for p in collected if p['category'] == category)}")

    if collected:
        prices = [p["price"] for p in collected]
        print(f"\nPrice range: min ${min(prices):.2f} | max ${max(prices):.2f} | "
              f"avg ${sum(prices) / len(prices):.2f}")
        oos = sum(1 for p in collected if p["stockQuantity"] == 0)
        print(f"Stock: in-stock {len(collected) - oos} | out-of-stock {oos}")

    if errors:
        print(f"\nWarnings during scraping ({len(errors)}):")
        for e in errors[:20]:
            print(f"  - {e}")
        if len(errors) > 20:
            print(f"  ... and {len(errors) - 20} more")


if __name__ == "__main__":
    main()
