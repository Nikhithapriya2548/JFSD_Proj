# Catalog seeding script

`seed_products.py` fills the OmniShop catalog with ~100–150 realistic products
by scraping a **legal scraping sandbox** and POSTing each item to product-service.

## Why this sandbox?

`https://webscraper.io/test-sites/e-commerce/allinone` is a public site built
**explicitly for scraping practice**: stable HTML, real listings across
laptops / tablets / phones, and a homepage notice that items aren't for sale.
No login, no paywall, no ToS conflict — scraping it for a student project is
exactly its intended use. (Do NOT point this script at real e-commerce sites.)

## Prerequisite

product-service must be running first:

```bash
cd D:\JFSD_Project
docker compose up --build -d
```

## How to run

```bash
cd D:\JFSD_Project\scripts
pip install -r requirements.txt
python seed_products.py
```

If products already exist, the script asks `[y/N]` before adding more
(idempotency safeguard against duplicate seeding).

## What it does

1. Scrapes all paginated listings in Laptops / Tablets / Phones
2. Enriches each with random `stockQuantity` (seed `42`; ~15% are 0 so the
   frontend "Out of Stock" badge has real data)
3. Dedupes by `(name, category)`, skips unparsable rows with warnings
4. POSTs each product to `http://localhost:8081/api/v1/products` (0.1s apart)
5. Prints a summary: per-category counts, price min/max/avg,
   in-stock vs out-of-stock, and every failure reason
