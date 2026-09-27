import json
import re

with open('products.json', 'r', encoding='utf-8') as f:
    products = json.load(f)

name_to_image = {}
for p in products:
    name = p['Name']
    sanitized = name.lower().replace(' ', '_')
    name_to_image[name] = f'/{sanitized}.png'

with open('product-service/src/main/resources/data.sql', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    match = re.search(r"VALUES \('(.*?)',", line)
    if match:
        name = match.group(1)
        if name in name_to_image:
            line = re.sub(r"(VALUES \('.*?', '.*?', [0-9.]+, ')(.*?)(')", r"\g<1>" + name_to_image[name] + r"\g<3>", line)
    new_lines.append(line)

with open('product-service/src/main/resources/data.sql', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print('Updated data.sql with exact matching')
