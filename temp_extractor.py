import json
with open(r'D:\gdg\JFSD_Proj\products.json', 'r', encoding='utf-8') as f:
    data = json.load(f)
for i, x in enumerate(data[140:160], 140):
    print(f'IDX: {i}')
    print(f'NAME: {x.get("Name", "")}')
    print(f'DESC: {x.get("Description", "")}')
    print(f'CAT: {x.get("Category", "")}')
    print('---')
