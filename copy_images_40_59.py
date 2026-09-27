import json
import os
import shutil
import glob

artifact_dir = r"C:\Users\K.Pavan Kumar\.gemini\antigravity\brain\3cdfded7-d973-41d4-90e1-3e82799f8d12"
public_dir = r"D:\gdg\JFSD_Proj\frontend\public"

prefixes = [
    "folio_pro_x",
    "pad_air_gen_3",
    "pad_lte",
    "slate_lte_v2",
    "slate_lte_v2_2",
    "pad_lte_2",
    "pad_pro_plus",
    "canvas_lte_v2",
    "canvas_lte_z",
    "pad_10_inch_gen_3",
    "canvas_pro_x",
    "canvas_10_inch_x",
    "canvas_air_plus",
    "tab_pro",
    "pad_12_inch_v2",
    "pad_10_inch_z",
    "tab_pro_z",
    "slate_10_inch_v2",
    "pad_10_inch_z_2",
    "slate_lte_plus"
]

with open(r"D:\gdg\JFSD_Proj\products.json", "r", encoding="utf-8") as f:
    products = json.load(f)

items = products[40:60]

for i, prefix in enumerate(prefixes):
    product_name = items[i]["Name"]
    dest_name = product_name.lower().replace(" ", "_") + ".png"
    dest_path = os.path.join(public_dir, dest_name)
    
    # find the file
    search_pattern = os.path.join(artifact_dir, prefix + "_*.png")
    matches = glob.glob(search_pattern)
    if matches:
        src_path = matches[0]
        print(f"Copying {src_path} to {dest_path}")
        shutil.copy2(src_path, dest_path)
    else:
        print(f"File not found for prefix {prefix}")
